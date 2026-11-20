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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Spring AI stateless MCP server ({@code spring.ai.mcp.server.protocol=STATELESS}) serves the Camel routes
 * (CAMEL-27145): no session is created, Camel's Origin validation applies, and the authenticated caller reaches the
 * route.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                classes = { SpringBootMcpServerStatelessTest.TestConfiguration.class },
                properties = {
                        "spring.main.banner-mode=off",
                        "spring.ai.mcp.server.protocol=STATELESS",
                        "camel.mcp-server.tags=stateless",
                        "camel.main.run-controller=false" })
public class SpringBootMcpServerStatelessTest {

    private static final String AUTHORIZATION = "Basic " + Base64.getEncoder()
            .encodeToString("mcp-user:mcp-secret".getBytes(StandardCharsets.UTF_8));

    private static final String INITIALIZE = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25",
            "capabilities":{},"clientInfo":{"name":"test","version":"1"}}}""";

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private McpServerEngine engine;

    @Configuration
    public static class TestConfiguration {

        @Bean
        public InMemoryUserDetailsManager mcpTestUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("mcp-user").password("{noop}mcp-secret").roles("MCP_USER").build());
        }

        @Bean
        public SecurityFilterChain mcpTestSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/mcp"))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/mcp").authenticated()
                            .anyRequest().permitAll())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }

        @Bean
        public RouteBuilder routes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("ai-tool:say_hello?tags=stateless&description=Say hello"
                         + "&parameter.name=string&parameter.name.required=true")
                            .setBody(simple("Hello ${header.name}"));

                    from("ai-tool:who_am_i?tags=stateless&description=Return the authenticated caller")
                            .process(exchange -> exchange.getMessage().setBody("name=" + exchange.getProperty(
                                    McpToolCallContext.SECURITY_PRINCIPAL_NAME_PROPERTY)));
                }
            };
        }
    }

    @Test
    void engineIsBoundToTheStatelessServer() {
        assertThat(engine).isInstanceOf(SpringAiMcpServerEngine.class);
    }

    @Test
    void clientCallsToolsWithoutSession() {
        McpSyncClient client = McpClient
                .sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                        .addHttpRequestCustomizer(
                                (builder, method, endpoint, body, context) -> builder.header("Authorization",
                                        AUTHORIZATION))
                        .build())
                .requestTimeout(Duration.ofSeconds(10))
                .initializationTimeout(Duration.ofSeconds(10))
                .build();
        try {
            client.initialize();

            assertThat(client.listTools().tools()).extracting(McpSchema.Tool::name)
                    .containsExactlyInAnyOrder("say_hello", "who_am_i");
            McpSchema.CallToolResult hello = client.callTool(
                    McpSchema.CallToolRequest.builder("say_hello").arguments(Map.of("name", "Camel")).build());
            assertThat(hello.content().toString()).contains("Hello Camel");
            McpSchema.CallToolResult caller = client.callTool(
                    McpSchema.CallToolRequest.builder("who_am_i").arguments(Map.of()).build());
            assertThat(caller.content().toString()).contains("name=mcp-user");
        } finally {
            client.closeGracefully();
        }
    }

    @Test
    void initializeReturnsNoSessionAndGetIsNotAllowed() throws Exception {
        HttpResponse<String> initialize = post(INITIALIZE, null);
        assertThat(initialize.statusCode()).isEqualTo(200);
        assertThat(initialize.headers().firstValue("Mcp-Session-Id")).isEmpty();

        HttpResponse<String> get = HTTP.send(request(null).header("Accept", "text/event-stream").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(get.statusCode()).isEqualTo(405);
    }

    @Test
    void originIsValidated() throws Exception {
        assertThat(post(INITIALIZE, "http://localhost:3000").statusCode()).isEqualTo(200);
        HttpResponse<String> rejected = post(INITIALIZE, "https://attacker.example");
        assertThat(rejected.statusCode()).isEqualTo(403);
        assertThat(rejected.body()).contains("Invalid Origin header");
    }

    private HttpResponse<String> post(String body, String origin) throws Exception {
        HttpRequest.Builder builder = request(origin)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String origin) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", AUTHORIZATION);
        if (origin != null) {
            builder.header("Origin", origin);
        }
        return builder;
    }
}
