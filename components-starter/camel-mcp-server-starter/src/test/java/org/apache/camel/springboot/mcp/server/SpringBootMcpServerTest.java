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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The engine conformance scenarios (CAMEL-24313) against the Spring AI MCP server engine, driven by the official MCP
 * SDK client over streamable HTTP. Mirrors {@code McpServerConformanceTestSupport} from camel-mcp-server-api, which
 * cannot be reused as-is here because it manages its own CamelContext outside Spring Boot.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                // the auto-configurations (Camel, Spring AI MCP server, this starter) are activated by
                // @EnableAutoConfiguration so their ordering and @ConditionalOnBean conditions apply
                classes = { SpringBootMcpServerTest.TestConfiguration.class },
                properties = {
                        "spring.main.banner-mode=off",
                        "spring.ai.mcp.server.protocol=STREAMABLE",
                        "camel.mcp-server.tags=conformance",
                        "camel.mcp-server.tool-timeout=2000",
                        "camel.springboot.main-run-controller=false" })
public class SpringBootMcpServerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CamelContext camelContext;

    private McpSyncClient client;

    /**
     * A plain Spring-defined MCP tool ({@code @McpTool} annotation, registered by the Spring AI annotation scanner)
     * living on the same MCP server as the Camel ai-tool routes.
     */
    public static class SpringDefinedTools {
        @McpTool(name = "add_numbers", description = "Add two numbers")
        public String add(
                @McpToolParam(description = "First addend", required = true) int a,
                @McpToolParam(description = "Second addend", required = true) int b) {
            return String.valueOf(a + b);
        }
    }

    @Configuration
    public static class TestConfiguration {
        @Bean
        public SpringDefinedTools springDefinedTools() {
            return new SpringDefinedTools();
        }

        @Bean
        public RouteBuilder routes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("ai-tool:say_hello?tags=conformance&description=Say hello"
                         + "&parameter.name=string&parameter.name.required=true")
                            .routeId("say-hello-route")
                            .setBody(simple("Hello ${header.name}"));

                    from("ai-tool:fail_tool?tags=conformance&description=Always fails")
                            .process(e -> {
                                throw new IllegalStateException("secret internal detail");
                            });

                    from("ai-tool:slow_tool?tags=conformance&description=Exceeds the tool timeout")
                            .delay(6000)
                            .setBody(constant("done"));

                    from("ai-tool:hidden_tool?description=Untagged tool, must not be exposed")
                            .setBody(constant("hidden"));

                    from("ai-tool:other_tool?tags=untrusted&description=Not a selected tag, must not be exposed")
                            .setBody(constant("other"));
                }
            };
        }
    }

    private McpSyncClient client() {
        if (client == null) {
            client = McpClient
                    .sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port).build())
                    .requestTimeout(Duration.ofSeconds(10))
                    .initializationTimeout(Duration.ofSeconds(10))
                    .build();
            client.initialize();
        }
        return client;
    }

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.closeGracefully();
            client = null;
        }
    }

    @Test
    void testListToolsExposesOnlySelectedTags() {
        List<McpSchema.Tool> tools = client().listTools().tools();

        assertThat(tools).extracting(McpSchema.Tool::name)
                .contains("say_hello", "fail_tool", "slow_tool")
                .doesNotContain("hidden_tool", "other_tool");
    }

    @Test
    void testSpringAnnotatedToolsCoexistWithCamelTools() {
        // both tool sources are served by the same MCP server
        assertThat(client().listTools().tools()).extracting(McpSchema.Tool::name)
                .contains("add_numbers", "say_hello");

        McpSchema.CallToolResult result
                = client().callTool(new McpSchema.CallToolRequest("add_numbers", Map.of("a", 17, "b", 25)));
        assertThat(result.isError()).as(textOf(result)).isNotEqualTo(Boolean.TRUE);
        assertThat(textOf(result)).isEqualTo("42");
    }

    @Test
    void testCallToolSuccess() {
        McpSchema.CallToolResult result
                = client().callTool(new McpSchema.CallToolRequest("say_hello", Map.of("name", "World")));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(textOf(result)).isEqualTo("Hello World");
    }

    @Test
    void testCallToolMissingRequiredArgument() {
        McpSchema.CallToolResult result = client().callTool(new McpSchema.CallToolRequest("say_hello", Map.of()));

        assertThat(result.isError()).isEqualTo(Boolean.TRUE);
        assertThat(textOf(result)).contains("name");
    }

    @Test
    void testCallToolExecutionErrorIsSanitized() {
        McpSchema.CallToolResult result = client().callTool(new McpSchema.CallToolRequest("fail_tool", Map.of()));

        assertThat(result.isError()).isEqualTo(Boolean.TRUE);
        assertThat(textOf(result))
                .doesNotContain("secret internal detail")
                .isEqualTo("Tool execution failed");
    }

    @Test
    void testCallToolTimeout() {
        McpSchema.CallToolResult result = client().callTool(new McpSchema.CallToolRequest("slow_tool", Map.of()));

        assertThat(result.isError()).isEqualTo(Boolean.TRUE);
        assertThat(textOf(result)).contains("timed out");
    }

    @Test
    void testToolsListReflectsRouteStopAndStart() throws Exception {
        assertThat(client().listTools().tools()).extracting(McpSchema.Tool::name).contains("say_hello");

        camelContext.getRouteController().stopRoute("say-hello-route");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(client().listTools().tools())
                .extracting(McpSchema.Tool::name).doesNotContain("say_hello"));

        camelContext.getRouteController().startRoute("say-hello-route");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(client().listTools().tools())
                .extracting(McpSchema.Tool::name).contains("say_hello"));
    }

    private static String textOf(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(c -> ((McpSchema.TextContent) c).text())
                .collect(Collectors.joining());
    }
}
