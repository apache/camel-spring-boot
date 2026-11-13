/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.springboot.mcp.server;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that an application-provided Spring AI transport can preserve
 * authenticated caller identity by installing Camel's public context extractor.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SpringBootMcpServerCustomTransportTest.TestConfiguration.class,
        properties = {
                "spring.main.banner-mode=off",
                "spring.ai.mcp.server.protocol=STREAMABLE",
                "camel.mcp-server.tags=custom-provider",
                "camel.springboot.main-run-controller=false"
        })
public class SpringBootMcpServerCustomTransportTest {

    private static final String AUTHORIZATION = "Basic " + Base64.getEncoder()
            .encodeToString("custom-user:custom-secret".getBytes(StandardCharsets.UTF_8));

    @LocalServerPort
    private int port;

    @Test
    void customTransportCanPreserveAuthenticatedCallerIdentity() {
        McpSyncClient client = McpClient.sync(
                HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                        .addHttpRequestCustomizer((builder, method, endpoint, body, context) -> {
                            builder.header("Authorization", AUTHORIZATION);
                        })
                        .build())
                .requestTimeout(Duration.ofSeconds(10))
                .initializationTimeout(Duration.ofSeconds(10))
                .build();

        try {
            client.initialize();

            McpSchema.CallToolResult result = client.callTool(
                    new McpSchema.CallToolRequest("who_am_i", Map.of()));

            assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);

            String text = result.content().stream()
                    .filter(McpSchema.TextContent.class::isInstance)
                    .map(content -> ((McpSchema.TextContent) content).text())
                    .collect(Collectors.joining());

            assertThat(text)
                    .contains("principalPresent=true", "name=custom-user", "ROLE_MCP_USER");
        } finally {
            client.closeGracefully();
        }
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public InMemoryUserDetailsManager customTransportUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("custom-user")
                            .password("{noop}custom-secret")
                            .roles("MCP_USER")
                            .build());
        }

        @Bean
        public SecurityFilterChain customTransportSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/mcp"))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/mcp").authenticated()
                            .anyRequest().permitAll())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }

        @Bean
        @SuppressWarnings("deprecation")
        public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
                @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
                McpServerStreamableHttpProperties serverProperties) {

            return WebMvcStreamableServerTransportProvider.builder()
                    .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                    .mcpEndpoint(serverProperties.getMcpEndpoint())
                    .keepAliveInterval(serverProperties.getKeepAliveInterval())
                    .disallowDelete(serverProperties.isDisallowDelete())
                    .securityValidator(DefaultServerTransportSecurityValidator.builder()
                            .allowedOrigins(List.of(
                                    "http://localhost:*",
                                    "https://localhost:*"))
                            .build())
                    .contextExtractor(SpringAiMcpTransportContext::extract)
                    .build();
        }

        @Bean
        public RouteBuilder routes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("ai-tool:who_am_i?tags=custom-provider"
                         + "&description=Return authenticated caller identity")
                            .routeId("custom-provider-who-am-i-route")
                            .process(exchange -> {
                                Object principal = exchange.getProperty(
                                        McpToolCallContext.SECURITY_PRINCIPAL_PROPERTY);
                                Object principalName = exchange.getProperty(
                                        McpToolCallContext.SECURITY_PRINCIPAL_NAME_PROPERTY);
                                Object principalRoles = exchange.getProperty(
                                        McpToolCallContext.SECURITY_PRINCIPAL_ROLES_PROPERTY);

                                exchange.getMessage().setBody("principalPresent=" + (principal != null)
                                        + ";name=" + principalName + ";roles=" + principalRoles);
                            });
                }
            };
        }
    }
}
