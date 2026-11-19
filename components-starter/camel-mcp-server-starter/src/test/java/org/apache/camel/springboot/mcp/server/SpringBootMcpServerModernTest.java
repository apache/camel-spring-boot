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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.modern.McpSchema;
import io.modelcontextprotocol.modern.client.McpClient;
import io.modelcontextprotocol.modern.client.McpSyncClient;
import io.modelcontextprotocol.modern.client.SubscriptionEvent;
import io.modelcontextprotocol.modern.client.transport.HttpClientMcpTransport;
import io.modelcontextprotocol.modern.client.transport.McpHttpRequestCustomizer;
import io.modelcontextprotocol.modern.server.McpSyncResponse;
import io.modelcontextprotocol.modern.server.feature.McpSyncTool;
import org.apache.camel.CamelContext;
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
import static org.awaitility.Awaitility.await;

/**
 * The Spring AI MCP server for protocol version 2026-07-28 ({@code spring.ai.mcp.server.protocol=MODERN}) serves the
 * Camel routes (CAMEL-27174): no handshake and no session, a client listening with {@code subscriptions/listen} is told
 * when a route adds a tool, Camel's Origin validation applies, and the authenticated caller reaches the route.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                classes = { SpringBootMcpServerModernTest.TestConfiguration.class },
                properties = {
                        "spring.main.banner-mode=off",
                        "spring.ai.mcp.server.protocol=MODERN",
                        "spring.ai.mcp.server.name=camel-modern",
                        "camel.mcp-server.tags=modern",
                        "camel.main.run-controller=false" })
public class SpringBootMcpServerModernTest {

    private static final String AUTHORIZATION = "Basic " + Base64.getEncoder()
            .encodeToString("mcp-user:mcp-secret".getBytes(StandardCharsets.UTF_8));

    private static final String INITIALIZE = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25",
            "capabilities":{},"clientInfo":{"name":"test","version":"1"}}}""";

    private static final String DISCOVER = """
            {"jsonrpc":"2.0","id":2,"method":"server/discover","params":{"_meta":{
            "io.modelcontextprotocol/protocolVersion":"2026-07-28",
            "io.modelcontextprotocol/clientCapabilities":{},
            "io.modelcontextprotocol/clientInfo":{"name":"test","version":"1"}}}}""";

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private McpServerEngine engine;

    @Autowired
    private CamelContext camelContext;

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

        /**
         * A tool of the application itself, served next to the Camel routes.
         */
        @Bean
        public List<McpSyncTool> springTools() {
            McpSchema.Tool tool = McpSchema.Tool.builder("add_numbers", McpJsonDefaults.getMapper(), """
                    {"type":"object","properties":{}}""").description("A tool of the application").build();
            return List.of(McpSyncTool.of(tool, (ctx, request) -> McpSyncResponse.result(McpSchema.CallToolResult
                    .builder().addContent(McpSchema.TextContent.builder("from spring").build()).build())));
        }

        @Bean
        public RouteBuilder routes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("ai-tool:say_hello?tags=modern&description=Say hello"
                         + "&parameter.name=string&parameter.name.required=true")
                            .setBody(simple("Hello ${header.name}"));

                    from("ai-tool:who_am_i?tags=modern&description=Return the authenticated caller")
                            .process(exchange -> exchange.getMessage().setBody("name=" + exchange.getProperty(
                                    McpToolCallContext.SECURITY_PRINCIPAL_NAME_PROPERTY)));

                    // refused: the application already serves a tool with this name
                    from("ai-tool:add_numbers?tags=modern&description=Same name as the application tool")
                            .setBody(constant("from camel"));

                    from("ai-resource:readme?resourceUri=camel:///readme.txt&tags=modern"
                         + "&description=Read me&mimeType=text/plain")
                            .setBody(constant("read me"));

                    from("ai-resource:order?uriTemplate=camel:///orders/{orderId}&tags=modern"
                         + "&description=An order&mimeType=text/plain")
                            .process(exchange -> exchange.getMessage().setBody("order " + exchange.getMessage()
                                    .getHeader("CamelAiResourceTemplateVariable.orderId", String.class)));

                    from("ai-prompt:greet?tags=modern&description=Greet someone"
                         + "&argument.name.description=Who to greet&argument.name.required=true")
                            .setBody(simple("Hello ${header.name}"));
                }
            };
        }
    }

    @Test
    void engineIsBoundToTheModernServer() {
        assertThat(engine).isInstanceOf(SpringAiModernMcpServerEngine.class);
    }

    @Test
    void callsToolsWithoutHandshake() {
        McpSyncClient client = client();

        assertThat(client.tools().list().tools()).extracting(McpSchema.Tool::name)
                .contains("say_hello", "who_am_i", "add_numbers");
        assertThat(text(client.tool("say_hello").arguments(Map.of("name", "Camel")).call()))
                .isEqualTo("Hello Camel");
        assertThat(text(client.tool("who_am_i").arguments(Map.of()).call())).isEqualTo("name=mcp-user");
        assertThat(text(client.tool("add_numbers").arguments(Map.of()).call())).isEqualTo("from spring");
    }

    @Test
    void readsResourcesAndTemplates() {
        McpSyncClient client = client();

        assertThat(client.resources().list().resources()).extracting(McpSchema.Resource::uri)
                .containsExactly("camel:///readme.txt");
        assertThat(client.resourceTemplates().list().resourceTemplates())
                .extracting(McpSchema.ResourceTemplate::uriTemplate).containsExactly("camel:///orders/{orderId}");
        assertThat(client.resource("camel:///readme.txt").read().contents()).singleElement()
                .isInstanceOfSatisfying(McpSchema.TextResourceContents.class,
                        text -> assertThat(text.text()).isEqualTo("read me"));
        assertThat(client.resource("camel:///orders/42").read().contents()).singleElement()
                .isInstanceOfSatisfying(McpSchema.TextResourceContents.class,
                        text -> assertThat(text.text()).isEqualTo("order 42"));
    }

    @Test
    void getsAPrompt() {
        McpSyncClient client = client();

        assertThat(client.prompts().list().prompts()).extracting(McpSchema.Prompt::name).containsExactly("greet");
        McpSchema.GetPromptResult result = client.prompt("greet").arguments(Map.of("name", "Camel")).get();
        assertThat(result.messages()).singleElement().satisfies(message -> {
            assertThat(message.role()).isEqualTo(McpSchema.Role.USER);
            assertThat(message.content()).isInstanceOfSatisfying(McpSchema.TextContent.class,
                    text -> assertThat(text.text()).isEqualTo("Hello Camel"));
        });
    }

    @Test
    void listenerIsToldWhenARouteAddsAndRemovesATool() throws Exception {
        McpSyncClient client = client();
        List<SubscriptionEvent> events = new CopyOnWriteArrayList<>();
        try (McpSyncClient.Listening listening = client.listen().toolsChanged().open(events::add)) {
            await().atMost(10, TimeUnit.SECONDS)
                    .until(() -> events.stream().anyMatch(SubscriptionEvent.Subscribed.class::isInstance));

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("ai-tool:late_tool?tags=modern&description=Added while the client listens")
                            .routeId("late-tool")
                            .setBody(constant("late"));
                }
            });

            await().atMost(10, TimeUnit.SECONDS)
                    .until(() -> events.stream().anyMatch(SubscriptionEvent.ToolsListChanged.class::isInstance));
        }
        assertThat(client.tools().list().tools()).extracting(McpSchema.Tool::name).contains("late_tool");

        camelContext.getRouteController().stopRoute("late-tool");
        camelContext.removeRoute("late-tool");
        assertThat(client.tools().list().tools()).extracting(McpSchema.Tool::name).doesNotContain("late_tool");
    }

    @Test
    void initializeIsRefusedWithoutSession() throws Exception {
        HttpResponse<String> initialize = post(INITIALIZE, Map.of());

        // the error code is Spring AI's: its transport checks the 2026-07-28 headers first (-32020), where the MCP
        // SDK's servlet transport answers an initialize with the supported versions (-32022)
        assertThat(initialize.statusCode()).isEqualTo(400);
        assertThat(initialize.body()).contains("\"error\"");
        assertThat(initialize.headers().firstValue("Mcp-Session-Id")).isEmpty();
    }

    @Test
    void originIsValidated() throws Exception {
        Map<String, String> discover = Map.of("MCP-Protocol-Version", "2026-07-28", "Mcp-Method", "server/discover");

        assertThat(post(DISCOVER, discover).statusCode()).isEqualTo(200);
        assertThat(post(DISCOVER, withOrigin(discover, "http://localhost:3000")).statusCode()).isEqualTo(200);
        assertThat(post(DISCOVER, withOrigin(discover, "https://attacker.example")).statusCode()).isEqualTo(403);
    }

    private McpSyncClient client() {
        return McpClient.sync(HttpClientMcpTransport.builder("http://localhost:" + port + "/mcp")
                .requestCustomizer(McpHttpRequestCustomizer.fromSync(
                        (request, headers, context) -> headers.set("Authorization", AUTHORIZATION)))
                .build())
                .clientInfo(McpSchema.Implementation.builder("test", "1").build())
                .build();
    }

    private static String text(McpSchema.CallToolResult result) {
        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.content()).singleElement().isInstanceOf(McpSchema.TextContent.class);
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    private static Map<String, String> withOrigin(Map<String, String> headers, String origin) {
        Map<String, String> copy = new HashMap<>(headers);
        copy.put("Origin", origin);
        return copy;
    }

    private HttpResponse<String> post(String body, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", AUTHORIZATION)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
