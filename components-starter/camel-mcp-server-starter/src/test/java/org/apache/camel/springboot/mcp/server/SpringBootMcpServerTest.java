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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.CamelAuthorizationException;
import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.apache.camel.spi.AuthorizationPolicy;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The engine conformance scenarios (CAMEL-24313) against the Spring AI MCP server engine, driven by the official MCP
 * SDK client over streamable HTTP. Mirrors {@code McpServerConformanceTestSupport} from camel-mcp-server-api, which
 * cannot be reused as-is here because it manages its own CamelContext outside Spring Boot.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest(
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                // the auto-configurations (Camel, Spring AI MCP server, this starter) are activated by
                // @EnableAutoConfiguration so their ordering and @ConditionalOnBean conditions apply
                classes = { SpringBootMcpServerTest.TestConfiguration.class },
                properties = {
                        "spring.main.banner-mode=off",
                        "spring.ai.mcp.server.protocol=STREAMABLE",
                        "spring.ai.mcp.server.capabilities.resource=true",
                        "spring.ai.mcp.server.capabilities.prompt=true",
                        "spring.ai.mcp.server.resource-change-notification=true",
                        "spring.ai.mcp.server.prompt-change-notification=true",
                        "camel.mcp-server.tags=conformance",
                        "camel.mcp-server.tool-timeout=2000",
                        "camel.springboot.main-run-controller=false" })
public class SpringBootMcpServerTest {

    private static final String MCP_TEST_AUTHORIZATION = "Basic " + Base64.getEncoder()
            .encodeToString("mcp-user:mcp-secret".getBytes(StandardCharsets.UTF_8));
    private static final String MCP_READ_ONLY_TEST_AUTHORIZATION = "Basic " + Base64.getEncoder()
            .encodeToString("mcp-readonly:mcp-readonly-secret".getBytes(StandardCharsets.UTF_8));

    @LocalServerPort
    private int port;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private AtomicInteger originGuardedToolInvocations;

    @Autowired
    @Qualifier("mcpAdminToolInvocations")
    private AtomicInteger mcpAdminToolInvocations;

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
        public InMemoryUserDetailsManager mcpTestUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("mcp-user")
                            .password("{noop}mcp-secret")
                            .roles("MCP_USER", "MCP_ADMIN")
                            .build(),
                    User.withUsername("mcp-readonly")
                            .password("{noop}mcp-readonly-secret")
                            .roles("MCP_USER")
                            .build());
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
        public AuthorizationPolicy mcpAdminPolicy() {
            return new RequireMcpAdminPolicy();
        }

        @Bean
        public AtomicInteger originGuardedToolInvocations() {
            return new AtomicInteger();
        }

        @Bean
        public AtomicInteger mcpAdminToolInvocations() {
            return new AtomicInteger();
        }

        @Bean
        public RouteBuilder routes(
                @Qualifier("originGuardedToolInvocations") AtomicInteger originGuardedToolInvocations,
                @Qualifier("mcpAdminToolInvocations") AtomicInteger mcpAdminToolInvocations) {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("ai-tool:say_hello?tags=conformance&description=Say hello"
                         + "&parameter.name=string&parameter.name.required=true")
                            .routeId("say-hello-route")
                            .setBody(simple("Hello ${header.name}"));

                    from("ai-tool:who_am_i?tags=conformance&description=Return authenticated caller identity")
                            .routeId("who-am-i-route")
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

                    from("ai-tool:origin_guarded_tool?tags=conformance"
                         + "&description=Tool used to verify rejected Origin requests")
                            .routeId("origin-guarded-tool-route")
                            .process(exchange -> {
                                originGuardedToolInvocations.incrementAndGet();
                                exchange.getMessage().setBody("executed");
                            });

                    from("ai-tool:admin_only_tool?tags=conformance"
                         + "&description=Requires the MCP admin role"
                         + "&authorizationPolicy=#mcpAdminPolicy")
                            .routeId("admin-only-tool-route")
                            .process(exchange -> {
                                mcpAdminToolInvocations.incrementAndGet();
                                exchange.getMessage().setBody("admin access granted");
                            });

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

                    from("ai-tool:get_weather?tags=conformance&description=Get weather"
                         + "&outputSchema={\"type\":\"object\",\"properties\":{\"temperature\":{\"type\":\"number\"},\"unit\":{\"type\":\"string\"}},\"required\":[\"temperature\",\"unit\"]}")
                            .routeId("get-weather-route")
                            .setBody(constant("{\"temperature\":21.5,\"unit\":\"celsius\"}"));

                    from("ai-resource:app_config?resourceUri=camel:///config/app.json&tags=conformance"
                         + "&description=Application configuration&mimeType=application/json")
                            .routeId("app-config-route")
                            .setBody(constant("{\"env\":\"test\"}"));

                    from("ai-resource:latest_report?resourceUri=camel:///reports/latest.pdf&tags=conformance"
                         + "&description=Latest report&mimeType=application/pdf")
                            .routeId("latest-report-route")
                            .setBody(constant(new byte[] { 0x25, 0x50, 0x44, 0x46 }));

                    from("ai-resource:customer_template?uriTemplate=camel:///customers/{customerId}"
                         + "&tags=conformance&description=Customer selected by id&mimeType=text/plain")
                            .routeId("customer-template-route")
                            .process(exchange -> {
                                String id = exchange.getMessage().getHeader(
                                        "CamelAiResourceTemplateVariable.customerId", String.class);
                                exchange.getMessage().setBody(id);
                            });

                    from("ai-prompt:greeting?tags=conformance&description=Say hello&title=Greeting"
                         + "&argument.name.description=Who to greet&argument.name.required=true")
                            .routeId("greeting-prompt-route")
                            .setBody(simple("Hello ${header.name}"));

                    from("ai-prompt:multi_turn?tags=conformance"
                         + "&description=Return a user and assistant message")
                            .routeId("multi-turn-prompt-route")
                            .setBody(constant(List.of(
                                    "Plain user message",
                                    Map.of("role", "assistant", "content", "Assistant reply"))));

                    from("ai-tool:annotated_tool?tags=conformance&description=Tool with annotation hints"
                         + "&title=Annotated tool"
                         + "&readOnlyHint=true"
                         + "&idempotentHint=true"
                         + "&returnDirect=true")
                            .setBody(constant("annotated"));
                }
            };
        }
    }

    /**
     * Allows tool execution only when Spring Security supplied ROLE_MCP_ADMIN
     * and the MCP transport propagated it to the Camel exchange.
     */
    private static final class RequireMcpAdminPolicy implements AuthorizationPolicy {
        @Override
        public void beforeWrap(Route route, NamedNode definition) {
            // No route-specific setup is required.
        }

        @Override
        public Processor wrap(Route route, Processor processor) {
            return exchange -> {
                Object roleValue = exchange.getProperty(
                        McpToolCallContext.SECURITY_PRINCIPAL_ROLES_PROPERTY);
                if (!(roleValue instanceof Collection<?> roles) || !roles.contains("ROLE_MCP_ADMIN")) {
                    throw new CamelAuthorizationException("Caller is not authorized", exchange);
                }
                processor.process(exchange);
            };
        }
    }

    private McpSyncClient client() {
        if (client == null) {
            client = createMcpClient(MCP_TEST_AUTHORIZATION);
            client.initialize();
        }
        return client;
    }

    private McpSyncClient createMcpClient(String authorization) {
        return McpClient
                .sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                        .addHttpRequestCustomizer((builder, method, endpoint, body, context) -> {
                            builder.header("Authorization", authorization);
                        })
                        .build())
                .requestTimeout(Duration.ofSeconds(10))
                .initializationTimeout(Duration.ofSeconds(10))
                .build();
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
    void testToolAnnotationHintsArePublished() {
        McpSchema.Tool tool = client().listTools().tools().stream()
                .filter(t -> "annotated_tool".equals(t.name()))
                .findFirst()
                .orElseThrow();

        assertThat(tool.title()).isEqualTo("Annotated tool");
        assertThat(tool.annotations()).isNotNull();
        assertThat(tool.annotations().readOnlyHint()).isTrue();
        assertThat(tool.annotations().idempotentHint()).isTrue();
        assertThat(tool.annotations().returnDirect()).isTrue();
    }

    @Test
    void testToolWithoutHintsHasNoAnnotations() {
        McpSchema.Tool tool = client().listTools().tools().stream()
                .filter(t -> "say_hello".equals(t.name()))
                .findFirst()
                .orElseThrow();

        assertThat(tool.annotations()).isNull();
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
    void testAuthenticatedCallerIdentityAndRolesArePropagated() {
        McpSchema.CallToolResult result = client().callTool(
                new McpSchema.CallToolRequest("who_am_i", Map.of()));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(textOf(result))
                .contains("principalPresent=true", "name=mcp-user", "ROLE_MCP_USER", "ROLE_MCP_ADMIN");
    }

    @Test
    void testAuthorizationPolicyAllowsOnlyMcpAdminRole() {
        mcpAdminToolInvocations.set(0);

        // The Spring Security user with ROLE_MCP_ADMIN must pass the real Camel AuthorizationPolicy.
        McpSchema.CallToolResult adminResult = client().callTool(
                new McpSchema.CallToolRequest("admin_only_tool", Map.of()));

        assertThat(adminResult.isError()).as(textOf(adminResult)).isNotEqualTo(Boolean.TRUE);
        assertThat(textOf(adminResult)).isEqualTo("admin access granted");
        assertThat(mcpAdminToolInvocations.get()).isEqualTo(1);

        // This authenticated user has ROLE_MCP_USER but deliberately has no ROLE_MCP_ADMIN.
        McpSyncClient readOnlyClient = createMcpClient(MCP_READ_ONLY_TEST_AUTHORIZATION);
        try {
            readOnlyClient.initialize();

            McpSchema.CallToolResult identityResult = readOnlyClient.callTool(
                    new McpSchema.CallToolRequest("who_am_i", Map.of()));
            assertThat(identityResult.isError()).as(textOf(identityResult)).isNotEqualTo(Boolean.TRUE);
            assertThat(textOf(identityResult))
                    .contains("principalPresent=true", "name=mcp-readonly", "ROLE_MCP_USER")
                    .doesNotContain("ROLE_MCP_ADMIN");

            McpSchema.CallToolResult deniedResult = readOnlyClient.callTool(
                    new McpSchema.CallToolRequest("admin_only_tool", Map.of()));

            assertThat(deniedResult.isError()).isEqualTo(Boolean.TRUE);
            assertThat(textOf(deniedResult)).contains("admin_only_tool");
            assertThat(mcpAdminToolInvocations.get())
                    .as("The denied call must not execute the protected route body")
                    .isEqualTo(1);
        } finally {
            readOnlyClient.closeGracefully();
        }
    }

    @Test
    void testOriginValidationRejectsUntrustedOriginForAllHttpMethods() throws Exception {
        // POST: an untrusted browser origin must be rejected before initialization.
        HttpResponse<String> rejected = initializeRawMcpSession("https://attacker.example");
        assertThat(rejected.statusCode())
                .as("Untrusted POST Origin should be rejected: " + rejected.body())
                .isEqualTo(403);
        assertThat(rejected.body()).contains("Invalid Origin header");

        // POST: loopback is accepted; normalization handles case and the default port.
        HttpResponse<String> loopback = initializeRawMcpSession("http://LOCALHOST:80");
        assertThat(loopback.statusCode())
                .as("Loopback Origin should be accepted: " + loopback.body())
                .isEqualTo(200);

        String loopbackSession = loopback.headers()
                .firstValue("Mcp-Session-Id")
                .orElseThrow();

        // GET: the same Origin check applies to session event streams.
        assertThat(getRawMcpSession(loopbackSession, "https://attacker.example"))
                .isEqualTo(403);

        // DELETE: the same Origin check applies to session deletion.
        assertThat(deleteRawMcpSession(loopbackSession, "https://attacker.example"))
                .isEqualTo(403);

        assertThat(deleteRawMcpSession(loopbackSession, "http://localhost:5173"))
                .isEqualTo(200);

        // Non-browser MCP clients that omit Origin remain supported.
        HttpResponse<String> noOrigin = initializeRawMcpSession(null);
        assertThat(noOrigin.statusCode())
                .as("Requests without Origin should be accepted: " + noOrigin.body())
                .isEqualTo(200);

        String noOriginSession = noOrigin.headers()
                .firstValue("Mcp-Session-Id")
                .orElseThrow();

        assertThat(deleteRawMcpSession(noOriginSession, null)).isEqualTo(200);
    }

    @Test
    void testUntrustedOriginDoesNotInvokeToolRoute() throws Exception {
        originGuardedToolInvocations.set(0);

        McpSchema.CallToolResult trustedResult = client().callTool(
                new McpSchema.CallToolRequest("origin_guarded_tool", Map.of()));

        assertThat(trustedResult.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(textOf(trustedResult)).isEqualTo("executed");
        assertThat(originGuardedToolInvocations.get()).isEqualTo(1);

        // Reset after proving the route is registered and callable.
        originGuardedToolInvocations.set(0);

        HttpResponse<String> initialized = initializeRawMcpSession("http://localhost:5173");
        assertThat(initialized.statusCode())
                .as("A loopback Origin should be allowed: " + initialized.body())
                .isEqualTo(200);

        String sessionId = initialized.headers()
                .firstValue("Mcp-Session-Id")
                .orElseThrow();

        HttpResponse<String> rejected = callRawMcpTool(
                sessionId, "https://attacker.example");

        assertThat(rejected.statusCode())
                .as("Untrusted Origin must be rejected before tool execution: " + rejected.body())
                .isEqualTo(403);
        assertThat(rejected.body()).contains("Invalid Origin header");
        assertThat(originGuardedToolInvocations.get())
                .as("The rejected request must not invoke the Camel tool route")
                .isZero();

        assertThat(deleteRawMcpSession(sessionId, "http://localhost:5173")).isEqualTo(200);
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
    void testToolOutputSchemaAndStructuredContentArePublished() {
        McpSchema.Tool weather = client().listTools().tools().stream()
                .filter(t -> "get_weather".equals(t.name()))
                .findFirst()
                .orElseThrow();

        assertThat(weather.outputSchema()).isNotNull();
        assertThat(weather.outputSchema()).containsKey("properties");

        McpSchema.CallToolResult result = client().callTool(
                new McpSchema.CallToolRequest("get_weather", Map.of()));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.structuredContent()).isNotNull();
        assertThat(result.structuredContent()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) result.structuredContent()).get("temperature"))
                .isEqualTo(21.5);
    }

    @Test
    void testResourcesArePublishedAndReadable() {
        List<McpSchema.Resource> resources = client().listResources().resources();

        assertThat(resources).extracting(McpSchema.Resource::uri)
                .contains("camel:///config/app.json", "camel:///reports/latest.pdf");

        McpSchema.Resource config = resources.stream()
                .filter(resource -> "camel:///config/app.json".equals(resource.uri()))
                .findFirst()
                .orElseThrow();

        assertThat(config.name()).isEqualTo("app_config");
        assertThat(config.mimeType()).isEqualTo("application/json");

        McpSchema.ReadResourceResult result = client().readResource(
                new McpSchema.ReadResourceRequest("camel:///config/app.json"));

        assertThat(result.contents()).hasSize(1);

        McpSchema.TextResourceContents contents =
                (McpSchema.TextResourceContents) result.contents().get(0);

        assertThat(contents.text()).isEqualTo("{\"env\":\"test\"}");
        assertThat(contents.mimeType()).isEqualTo("application/json");
    }

    @Test
    void testBinaryResourceIsPublishedAndReadable() {
        McpSchema.ReadResourceResult result = client().readResource(
                new McpSchema.ReadResourceRequest("camel:///reports/latest.pdf"));

        assertThat(result.contents()).hasSize(1);

        McpSchema.BlobResourceContents contents =
                (McpSchema.BlobResourceContents) result.contents().get(0);

        assertThat(contents.mimeType()).isEqualTo("application/pdf");
        assertThat(Base64.getDecoder().decode(contents.blob()))
                .containsExactly((byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46);
    }

    @Test
    void testResourceTemplateIsPublishedAndReadable() {
        McpSchema.ResourceTemplate template = client().listResourceTemplates()
                .resourceTemplates().stream()
                .filter(t -> "camel:///customers/{customerId}".equals(t.uriTemplate()))
                .findFirst()
                .orElseThrow();

        assertThat(template.name()).isEqualTo("customer_template");
        assertThat(template.description()).isEqualTo("Customer selected by id");

        McpSchema.ReadResourceResult result = client().readResource(
                new McpSchema.ReadResourceRequest("camel:///customers/42"));

        assertThat(result.contents()).hasSize(1);

        McpSchema.TextResourceContents contents =
                (McpSchema.TextResourceContents) result.contents().get(0);

        assertThat(contents.uri()).isEqualTo("camel:///customers/42");
        assertThat(contents.text()).isEqualTo("42");
    }

    @Test
    void testPromptIsPublishedAndCanBeRendered() {
        McpSchema.Prompt prompt = client().listPrompts().prompts().stream()
                .filter(p -> "greeting".equals(p.name()))
                .findFirst()
                .orElseThrow();

        assertThat(prompt.title()).isEqualTo("Greeting");
        assertThat(prompt.description()).isEqualTo("Say hello");
        assertThat(prompt.arguments()).hasSize(1);
        assertThat(prompt.arguments().get(0).name()).isEqualTo("name");
        assertThat(prompt.arguments().get(0).required()).isTrue();

        McpSchema.GetPromptResult result = client().getPrompt(
                new McpSchema.GetPromptRequest("greeting", Map.of("name", "World")));

        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().get(0).role()).isEqualTo(McpSchema.Role.USER);
        assertThat(result.messages().get(0).content()).isInstanceOf(McpSchema.TextContent.class);

        McpSchema.TextContent content =
                (McpSchema.TextContent) result.messages().get(0).content();

        assertThat(content.text()).isEqualTo("Hello World");
    }

    @Test
    void testPromptSupportsMultipleUserAndAssistantMessages() {
        McpSchema.GetPromptResult result = client().getPrompt(
                new McpSchema.GetPromptRequest("multi_turn", Map.of()));

        assertThat(result.messages()).hasSize(2);

        assertThat(result.messages().get(0).role()).isEqualTo(McpSchema.Role.USER);
        assertThat(result.messages().get(0).content()).isInstanceOf(McpSchema.TextContent.class);
        assertThat(((McpSchema.TextContent) result.messages().get(0).content()).text())
                .isEqualTo("Plain user message");

        assertThat(result.messages().get(1).role()).isEqualTo(McpSchema.Role.ASSISTANT);
        assertThat(result.messages().get(1).content()).isInstanceOf(McpSchema.TextContent.class);
        assertThat(((McpSchema.TextContent) result.messages().get(1).content()).text())
                .isEqualTo("Assistant reply");
    }

    @Test
    void testServerAdvertisesResourceAndPromptChangeCapabilities() {
        McpSchema.ServerCapabilities capabilities = client().getServerCapabilities();

        assertThat(capabilities.resources()).isNotNull();
        assertThat(capabilities.resources().listChanged()).isTrue();

        assertThat(capabilities.prompts()).isNotNull();
        assertThat(capabilities.prompts().listChanged()).isTrue();
    }

    @Test
    void testResourceAndPromptChangeNotificationsAreDelivered() throws Exception {
        AtomicInteger toolsNotificationCount = new AtomicInteger();
        AtomicBoolean resourceNotificationReceived = new AtomicBoolean();
        AtomicBoolean promptNotificationReceived = new AtomicBoolean();

        withRouteRestoredToStarted("say-hello-route", () ->
                withRouteRestoredToStarted("app-config-route", () ->
                        withRouteRestoredToStarted("greeting-prompt-route", () -> {
                            McpSyncClient notificationClient = notificationClient(
                                    toolsNotificationCount,
                                    resourceNotificationReceived,
                                    promptNotificationReceived);

                            try {
                                notificationClient.initialize();

                                // Use two real tools/list_changed notifications as a protocol-level
                                // readiness barrier. The client has received server-to-client SSE
                                // notifications before resource and prompt changes are triggered.
                                camelContext.getRouteController().stopRoute("say-hello-route");

                                await().atMost(10, TimeUnit.SECONDS)
                                        .until(() -> toolsNotificationCount.get() >= 1);

                                camelContext.getRouteController().startRoute("say-hello-route");

                                await().atMost(10, TimeUnit.SECONDS)
                                        .until(() -> toolsNotificationCount.get() >= 2);

                                camelContext.getRouteController().stopRoute("app-config-route");

                                await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                                        assertThat(resourceNotificationReceived.get()).isTrue());

                                camelContext.getRouteController().stopRoute("greeting-prompt-route");

                                await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                                        assertThat(promptNotificationReceived.get()).isTrue());
                            } finally {
                                notificationClient.closeGracefully();
                            }
                        })));
    }

    @Test
    void testResourceRouteLifecycleUpdatesMcpList() throws Exception {
        withRouteRestoredToStarted("app-config-route", () -> {
            assertThat(client().listResources().resources())
                    .extracting(McpSchema.Resource::uri)
                    .contains("camel:///config/app.json");

            camelContext.getRouteController().stopRoute("app-config-route");

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(client().listResources().resources())
                            .extracting(McpSchema.Resource::uri)
                            .doesNotContain("camel:///config/app.json"));

            camelContext.getRouteController().startRoute("app-config-route");

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(client().listResources().resources())
                            .extracting(McpSchema.Resource::uri)
                            .contains("camel:///config/app.json"));
        });
    }

    @Test
    void testResourceTemplateRouteLifecycleUpdatesMcpList() throws Exception {
        withRouteRestoredToStarted("customer-template-route", () -> {
            assertThat(client().listResourceTemplates().resourceTemplates())
                    .extracting(McpSchema.ResourceTemplate::uriTemplate)
                    .contains("camel:///customers/{customerId}");

            camelContext.getRouteController().stopRoute("customer-template-route");

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(client().listResourceTemplates().resourceTemplates())
                            .extracting(McpSchema.ResourceTemplate::uriTemplate)
                            .doesNotContain("camel:///customers/{customerId}"));

            camelContext.getRouteController().startRoute("customer-template-route");

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(client().listResourceTemplates().resourceTemplates())
                            .extracting(McpSchema.ResourceTemplate::uriTemplate)
                            .contains("camel:///customers/{customerId}"));
        });
    }

    @Test
    void testPromptRouteLifecycleUpdatesMcpList() throws Exception {
        withRouteRestoredToStarted("greeting-prompt-route", () -> {
            assertThat(client().listPrompts().prompts())
                    .extracting(McpSchema.Prompt::name)
                    .contains("greeting");

            camelContext.getRouteController().stopRoute("greeting-prompt-route");

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(client().listPrompts().prompts())
                            .extracting(McpSchema.Prompt::name)
                            .doesNotContain("greeting"));

            camelContext.getRouteController().startRoute("greeting-prompt-route");

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(client().listPrompts().prompts())
                            .extracting(McpSchema.Prompt::name)
                            .contains("greeting"));
        });
    }

    @Test
    void testToolsListReflectsRouteStopAndStart() throws Exception {
        withRouteRestoredToStarted("say-hello-route", () -> {
            assertThat(client().listTools().tools())
                    .extracting(McpSchema.Tool::name)
                    .contains("say_hello");

            camelContext.getRouteController().stopRoute("say-hello-route");

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(client().listTools().tools())
                            .extracting(McpSchema.Tool::name)
                            .doesNotContain("say_hello"));

            camelContext.getRouteController().startRoute("say-hello-route");

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(client().listTools().tools())
                            .extracting(McpSchema.Tool::name)
                            .contains("say_hello"));
        });
    }

    @FunctionalInterface
    private interface RouteLifecycleAction {
        void run() throws Exception;
    }

    /**
     * Runs a route lifecycle scenario and restores the route to its initially
     * started state, even when an assertion or lifecycle operation fails.
     *
     * The Spring test application context is shared by test methods, so route
     * state must not leak from one test method into another.
     */
    private void withRouteRestoredToStarted(
            String routeId, RouteLifecycleAction action) throws Exception {

        var routeController = camelContext.getRouteController();
        ServiceStatus initialStatus = routeController.getRouteStatus(routeId);

        assertThat(initialStatus)
                .as("Route '%s' must be started before its lifecycle test", routeId)
                .isEqualTo(ServiceStatus.Started);

        Throwable testFailure = null;

        try {
            action.run();
        } catch (Exception | Error failure) {
            testFailure = failure;
            throw failure;
        } finally {
            try {
                restoreRouteToStarted(routeId);
            } catch (Exception | Error cleanupFailure) {
                if (testFailure != null) {
                    testFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }

    /**
     * Restores a route to Started from a stable Stopped or Suspended state.
     */
    private void restoreRouteToStarted(String routeId) throws Exception {
        var routeController = camelContext.getRouteController();

        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            ServiceStatus status = routeController.getRouteStatus(routeId);

            if (status == null) {
                return true;
            }

            return switch (status.name()) {
                case "Starting", "Stopping", "Suspending", "Resuming" -> false;
                default -> true;
            };
        });

        ServiceStatus currentStatus = routeController.getRouteStatus(routeId);

        if (currentStatus == null) {
            throw new IllegalStateException(
                    "MCP test route '" + routeId + "' no longer exists");
        }

        if (currentStatus == ServiceStatus.Stopped) {
            routeController.startRoute(routeId);
        } else if ("Suspended".equals(currentStatus.name())) {
            routeController.resumeRoute(routeId);
        } else if (currentStatus != ServiceStatus.Started) {
            throw new IllegalStateException(
                    "Cannot restore MCP test route '" + routeId
                    + "' from state " + currentStatus);
        }

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(routeController.getRouteStatus(routeId))
                        .as("Route '%s' must be restored to Started", routeId)
                        .isEqualTo(ServiceStatus.Started));
    }

    private HttpResponse<String> initializeRawMcpSession(String origin) throws Exception {
        String body = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-06-18",
                    "capabilities": {},
                    "clientInfo": {
                      "name": "camel-origin-test",
                      "version": "1.0"
                    }
                  }
                }
                """;

        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/mcp"))
                .header("Authorization", MCP_TEST_AUTHORIZATION)
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json");

        if (origin != null) {
            request.header("Origin", origin);
        }

        request.POST(HttpRequest.BodyPublishers.ofString(body));

        return HttpClient.newHttpClient().send(
                request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> callRawMcpTool(String sessionId, String origin) throws Exception {
        String body = """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "method": "tools/call",
                  "params": {
                    "name": "origin_guarded_tool",
                    "arguments": {}
                  }
                }
                """;

        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/mcp"))
                .header("Authorization", MCP_TEST_AUTHORIZATION)
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header("Mcp-Session-Id", sessionId)
                .header("MCP-Protocol-Version", "2025-06-18");

        if (origin != null) {
            request.header("Origin", origin);
        }

        request.POST(HttpRequest.BodyPublishers.ofString(body));

        return HttpClient.newHttpClient().send(
                request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private int getRawMcpSession(String sessionId, String origin) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/mcp"))
                .header("Authorization", MCP_TEST_AUTHORIZATION)
                .header("Accept", "text/event-stream")
                .header("Mcp-Session-Id", sessionId);

        if (origin != null) {
            request.header("Origin", origin);
        }

        request.GET();

        return HttpClient.newHttpClient().send(
                request.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private int deleteRawMcpSession(String sessionId, String origin) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/mcp"))
                .header("Authorization", MCP_TEST_AUTHORIZATION)
                .header("Mcp-Session-Id", sessionId);

        if (origin != null) {
            request.header("Origin", origin);
        }

        request.DELETE();

        return HttpClient.newHttpClient().send(
                request.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private McpSyncClient notificationClient(
            AtomicInteger toolsNotificationCount,
            AtomicBoolean resourceNotificationReceived,
            AtomicBoolean promptNotificationReceived) {

        return McpClient.sync(
                HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                        .addHttpRequestCustomizer((builder, method, endpoint, body, context) -> {
                            builder.header("Authorization", MCP_TEST_AUTHORIZATION);
                        })
                        .build())
                .requestTimeout(Duration.ofSeconds(10))
                .initializationTimeout(Duration.ofSeconds(10))
                .toolsChangeConsumer(tools -> toolsNotificationCount.incrementAndGet())
                .resourcesChangeConsumer(resources -> resourceNotificationReceived.set(true))
                .promptsChangeConsumer(prompts -> promptNotificationReceived.set(true))
                .build();
    }

    private static String textOf(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(c -> ((McpSchema.TextContent) c).text())
                .collect(Collectors.joining());
    }
}
