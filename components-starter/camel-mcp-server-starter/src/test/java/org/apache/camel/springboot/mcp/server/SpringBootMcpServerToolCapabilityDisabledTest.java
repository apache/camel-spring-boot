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

import java.util.List;

import io.modelcontextprotocol.modern.server.feature.McpSyncToolRepository;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.mcp.modern.InMemorySyncPromptRepository;
import org.springframework.ai.mcp.modern.InMemorySyncResourceRepository;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With {@code spring.ai.mcp.server.capabilities.tool=false}, the Camel {@code ai-tool} routes are refused, whatever the
 * protocol: the application starts, the routes keep running, and the Camel resources and prompts are served.
 */
class SpringBootMcpServerToolCapabilityDisabledTest {

    @ParameterizedTest
    @ValueSource(strings = { "SSE", "STREAMABLE", "STATELESS", "MODERN" })
    void toolRoutesAreRefusedAndTheOtherFeaturesAreServed(String protocol) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
                .web(WebApplicationType.SERVLET)
                .properties(
                        "server.port=0",
                        "spring.main.banner-mode=off",
                        "spring.ai.mcp.server.protocol=" + protocol,
                        "spring.ai.mcp.server.capabilities.tool=false",
                        "camel.mcp-server.tags=crm",
                        "camel.main.run-controller=false")
                .run()) {
            CamelContext camelContext = context.getBean(CamelContext.class);
            assertThat(camelContext.getRouteController().getRouteStatus("crm-tool")).isEqualTo(ServiceStatus.Started);
            assertThat(toolNames(context, protocol)).isEmpty();
            assertThat(resourceUris(context, protocol)).containsExactly("camel:///crm/config");
            assertThat(promptNames(context, protocol)).containsExactly("crm_prompt");
        }
    }

    private static List<String> toolNames(ConfigurableApplicationContext context, String protocol) {
        return switch (protocol) {
            case "STATELESS" -> context.getBean(McpStatelessSyncServer.class).listTools().stream()
                    .map(McpSchema.Tool::name).toList();
            // without the capability, Spring AI defines no tool repository, and the server serves no tools
            case "MODERN" -> context.getBeanProvider(McpSyncToolRepository.class).stream()
                    .flatMap(repository -> repository.list(null, null).tools().stream())
                    .map(tool -> tool.name()).toList();
            default -> context.getBean(McpSyncServer.class).listTools().stream()
                    .map(McpSchema.Tool::name).toList();
        };
    }

    private static List<String> resourceUris(ConfigurableApplicationContext context, String protocol) {
        return switch (protocol) {
            case "STATELESS" -> context.getBean(McpStatelessSyncServer.class).listResources().stream()
                    .map(McpSchema.Resource::uri).toList();
            case "MODERN" -> context.getBean(InMemorySyncResourceRepository.class).list(null, null).resources().stream()
                    .map(resource -> resource.uri()).toList();
            default -> context.getBean(McpSyncServer.class).listResources().stream()
                    .map(McpSchema.Resource::uri).toList();
        };
    }

    private static List<String> promptNames(ConfigurableApplicationContext context, String protocol) {
        return switch (protocol) {
            case "STATELESS" -> context.getBean(McpStatelessSyncServer.class).listPrompts().stream()
                    .map(McpSchema.Prompt::name).toList();
            case "MODERN" -> context.getBean(InMemorySyncPromptRepository.class).list(null, null).prompts().stream()
                    .map(prompt -> prompt.name()).toList();
            default -> context.getBean(McpSyncServer.class).listPrompts().stream()
                    .map(McpSchema.Prompt::name).toList();
        };
    }

    @Configuration
    @EnableAutoConfiguration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("ai-tool:crm_lookup?tags=crm&description=Look up a customer")
                            .routeId("crm-tool")
                            .setBody(constant("customer"));

                    from("ai-resource:crm_config?resourceUri=camel:///crm/config&tags=crm"
                         + "&description=The CRM configuration&mimeType=text/plain")
                            .routeId("crm-resource")
                            .setBody(constant("config"));

                    from("ai-prompt:crm_prompt?tags=crm&description=A CRM prompt")
                            .routeId("crm-prompt")
                            .setBody(constant("Summarize the customer"));
                }
            };
        }
    }
}
