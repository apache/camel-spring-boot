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
import java.util.Map;

import io.modelcontextprotocol.modern.McpSchema;
import io.modelcontextprotocol.modern.server.McpSyncResponse;
import io.modelcontextprotocol.util.DefaultMcpUriTemplateManagerFactory;
import io.modelcontextprotocol.util.McpUriTemplateManager;
import org.apache.camel.component.mcp.server.McpServerPrompt;
import org.apache.camel.component.mcp.server.McpServerRefusedException;
import org.apache.camel.component.mcp.server.McpServerResource;
import org.apache.camel.component.mcp.server.McpServerResourceTemplate;
import org.apache.camel.component.mcp.server.McpServerTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.modern.InMemorySyncResourceRepository;
import org.springframework.ai.mcp.modern.InMemorySyncToolRepository;
import org.springframework.ai.mcp.modern.SyncResourceRegistration;
import org.springframework.ai.mcp.modern.SyncResourceTemplateRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiModernMcpServerEngineTest {

    private static final String OBJECT_SCHEMA = "{\"type\":\"object\"}";

    private static final String INVALID_SCHEMA = "{\"type\":\"obj\"}";

    @Test
    void refusesAToolWithAnInvalidOutputSchemaAndServesTheOtherTools() {
        InMemorySyncToolRepository tools = InMemorySyncToolRepository.builder().build();
        SpringAiModernMcpServerEngine engine = new SpringAiModernMcpServerEngine(tools, null, null);

        assertThatThrownBy(() -> engine.toolAdded(tool("bad_output", OBJECT_SCHEMA, INVALID_SCHEMA)))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("MCP tool 'bad_output' has an invalid input or output schema")
                .hasMessageContaining("outputSchema is not a valid JSON Schema 2020-12 document")
                .hasCauseInstanceOf(IllegalArgumentException.class);

        engine.toolAdded(tool("good", OBJECT_SCHEMA, OBJECT_SCHEMA));
        assertThat(tools.list(null, null).tools()).extracting(McpSchema.Tool::name).containsExactly("good");
    }

    @Test
    void refusesAToolWithAnInvalidInputSchema() {
        InMemorySyncToolRepository tools = InMemorySyncToolRepository.builder().build();
        SpringAiModernMcpServerEngine engine = new SpringAiModernMcpServerEngine(tools, null, null);

        assertThatThrownBy(() -> engine.toolAdded(tool("bad_input", INVALID_SCHEMA, null)))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("inputSchema is not a valid JSON Schema 2020-12 document");

        assertThat(tools.list(null, null).tools()).isEmpty();
    }

    @Test
    void refusesAResourceWithTheUriOfAnApplicationTemplateAndKeepsTheTemplate() {
        InMemorySyncResourceRepository resources = InMemorySyncResourceRepository.builder()
                .resourceTemplate(applicationTemplate("docs://guide"))
                .build();
        SpringAiModernMcpServerEngine engine = new SpringAiModernMcpServerEngine(null, resources, null);

        assertThatThrownBy(() -> engine.resourceAdded(resource("docs://guide")))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("MCP resource 'docs://guide' is not served")
                .hasMessageContaining("a resource template with the same URI is already registered");

        // another Camel resource is served, and removing it leaves the application's template alone
        engine.resourceAdded(resource("docs://camel"));
        assertThat(resources.list(null, null).resources()).extracting(McpSchema.Resource::uri)
                .containsExactly("docs://camel");
        engine.resourceRemoved("docs://camel");
        assertThat(resources.list(null, null).resources()).isEmpty();
        assertThat(resources.listTemplates(null, null).resourceTemplates())
                .extracting(McpSchema.ResourceTemplate::uriTemplate).containsExactly("docs://guide");
    }

    @Test
    void refusesATemplateWithTheUriOfAnApplicationResourceAndKeepsTheResource() {
        InMemorySyncResourceRepository resources = InMemorySyncResourceRepository.builder()
                .resource(applicationResource("docs://guide/{id}"))
                .build();
        SpringAiModernMcpServerEngine engine = new SpringAiModernMcpServerEngine(null, resources, null);

        assertThatThrownBy(() -> engine.resourceTemplateAdded(resourceTemplate("docs://guide/{id}")))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("MCP resource template 'docs://guide/{id}' is not served")
                .hasMessageContaining("a resource with the same URI is already registered");

        // another Camel template is served, and removing it leaves the application's resource alone
        engine.resourceTemplateAdded(resourceTemplate("camel:///customers/{id}"));
        assertThat(resources.listTemplates(null, null).resourceTemplates())
                .extracting(McpSchema.ResourceTemplate::uriTemplate).containsExactly("camel:///customers/{id}");
        engine.resourceTemplateRemoved("camel:///customers/{id}");
        assertThat(resources.listTemplates(null, null).resourceTemplates()).isEmpty();
        assertThat(resources.list(null, null).resources()).extracting(McpSchema.Resource::uri)
                .containsExactly("docs://guide/{id}");
    }

    @Test
    void refusesACamelResourceAndTemplateWithTheSameUri() {
        InMemorySyncResourceRepository resources = InMemorySyncResourceRepository.builder().build();
        SpringAiModernMcpServerEngine engine = new SpringAiModernMcpServerEngine(null, resources, null);

        engine.resourceAdded(resource("camel:///customers/{id}"));
        assertThatThrownBy(() -> engine.resourceTemplateAdded(resourceTemplate("camel:///customers/{id}")))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("a resource with the same URI is already registered");
        engine.resourceRemoved("camel:///customers/{id}");

        engine.resourceTemplateAdded(resourceTemplate("camel:///customers/{id}"));
        assertThatThrownBy(() -> engine.resourceAdded(resource("camel:///customers/{id}")))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("a resource template with the same URI is already registered");
        assertThat(resources.listTemplates(null, null).resourceTemplates())
                .extracting(McpSchema.ResourceTemplate::uriTemplate).containsExactly("camel:///customers/{id}");
        assertThat(resources.list(null, null).resources()).isEmpty();
    }

    @Test
    void removingARouteLeavesWhatTheApplicationRegisteredUnderItsUriSince() {
        InMemorySyncResourceRepository resources = InMemorySyncResourceRepository.builder().build();
        SpringAiModernMcpServerEngine engine = new SpringAiModernMcpServerEngine(null, resources, null);

        // the application replaces the route's template with a resource of the same URI
        engine.resourceTemplateAdded(resourceTemplate("camel:///customers/{id}"));
        resources.remove("camel:///customers/{id}");
        resources.add(applicationResource("camel:///customers/{id}"));
        engine.resourceTemplateRemoved("camel:///customers/{id}");
        assertThat(resources.list(null, null).resources()).extracting(McpSchema.Resource::uri)
                .containsExactly("camel:///customers/{id}");

        // and the route's resource with a template of the same URI
        engine.resourceAdded(resource("docs://guide"));
        resources.remove("docs://guide");
        resources.add(applicationTemplate("docs://guide"));
        engine.resourceRemoved("docs://guide");
        assertThat(resources.listTemplates(null, null).resourceTemplates())
                .extracting(McpSchema.ResourceTemplate::uriTemplate).containsExactly("docs://guide");
    }

    @Test
    void removingARouteRemovesItsResourceEvenWhenTheApplicationRegisteredATemplateWithTheSameUriAfterIt() {
        InMemorySyncResourceRepository resources = InMemorySyncResourceRepository.builder().build();
        SpringAiModernMcpServerEngine engine = new SpringAiModernMcpServerEngine(null, resources, null);

        engine.resourceAdded(resource("docs://guide"));
        resources.add(applicationTemplate("docs://guide"));
        engine.resourceRemoved("docs://guide");

        // the route's resource is no longer served; the repository cannot keep the template (a WARN is logged)
        assertThat(resources.list(null, null).resources()).isEmpty();
        assertThat(resources.listTemplates(null, null).resourceTemplates()).isEmpty();
    }

    @Test
    void servesTemplatesWithATemplateMatcherThatRejectsUnexpandedUris() {
        DefaultMcpUriTemplateManagerFactory defaults = new DefaultMcpUriTemplateManagerFactory();
        InMemorySyncResourceRepository resources = InMemorySyncResourceRepository.builder()
                .uriTemplateManagerFactory(uriTemplate -> {
                    McpUriTemplateManager delegate = defaults.create(uriTemplate);
                    return new McpUriTemplateManager() {
                        @Override
                        public List<String> getVariableNames() {
                            return delegate.getVariableNames();
                        }

                        @Override
                        public Map<String, String> extractVariableValues(String uri) {
                            return delegate.extractVariableValues(uri);
                        }

                        @Override
                        public boolean matches(String uri) {
                            if (uri.contains("{")) {
                                throw new IllegalArgumentException("Not a read URI: " + uri);
                            }
                            return delegate.matches(uri);
                        }

                        @Override
                        public boolean isUriTemplate(String uri) {
                            return delegate.isUriTemplate(uri);
                        }
                    };
                })
                .build();
        SpringAiModernMcpServerEngine engine = new SpringAiModernMcpServerEngine(null, resources, null);

        engine.resourceTemplateAdded(resourceTemplate("camel:///customers/{id}"));
        engine.resourceTemplateAdded(resourceTemplate("camel:///orders/{id}"));
        assertThat(resources.listTemplates(null, null).resourceTemplates()).hasSize(2);

        engine.resourceTemplateRemoved("camel:///customers/{id}");
        engine.resourceTemplateRemoved("camel:///orders/{id}");
        assertThat(resources.listTemplates(null, null).resourceTemplates()).isEmpty();
    }

    @Test
    void refusesEveryFeatureTheServerDoesNotServeNamingTheCapability() {
        SpringAiModernMcpServerEngine engine = new SpringAiModernMcpServerEngine(null, null, null);

        assertThatThrownBy(() -> engine.toolAdded(tool("crm_lookup", OBJECT_SCHEMA, null)))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("MCP tool 'crm_lookup' is not served")
                .hasMessageContaining("spring.ai.mcp.server.capabilities.tool");
        assertThatThrownBy(() -> engine.resourceAdded(resource("camel:///crm/config")))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("spring.ai.mcp.server.capabilities.resource");
        assertThatThrownBy(() -> engine.resourceTemplateAdded(resourceTemplate("camel:///crm/customers/{id}")))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("spring.ai.mcp.server.capabilities.resource");
        assertThatThrownBy(() -> engine.promptAdded(prompt("crm_prompt")))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("spring.ai.mcp.server.capabilities.prompt");
    }

    private static SyncResourceRegistration applicationResource(String uri) {
        return SyncResourceRegistration.of(McpSchema.Resource.builder(uri, "app_resource").build(),
                (ctx, request) -> McpSyncResponse.result(applicationContent(request.uri())));
    }

    private static SyncResourceTemplateRegistration applicationTemplate(String uriTemplate) {
        return SyncResourceTemplateRegistration.of(
                McpSchema.ResourceTemplate.builder(uriTemplate, "app_template").build(),
                (ctx, request) -> McpSyncResponse.result(applicationContent(request.uri())));
    }

    private static McpSchema.ReadResourceResult applicationContent(String uri) {
        return McpSchema.ReadResourceResult.builder(
                List.of(McpSchema.TextResourceContents.builder(uri, "app").build())).build();
    }

    private static McpServerTool tool(String name, String inputSchema, String outputSchema) {
        McpServerTool tool = mock(McpServerTool.class);
        when(tool.name()).thenReturn(name);
        when(tool.description()).thenReturn("A tool");
        when(tool.inputSchemaJson()).thenReturn(inputSchema);
        when(tool.outputSchemaJson()).thenReturn(outputSchema);
        return tool;
    }

    private static McpServerResource resource(String uri) {
        McpServerResource resource = mock(McpServerResource.class);
        when(resource.uri()).thenReturn(uri);
        when(resource.name()).thenReturn("camel_resource");
        when(resource.mimeType()).thenReturn("text/plain");
        return resource;
    }

    private static McpServerResourceTemplate resourceTemplate(String uriTemplate) {
        McpServerResourceTemplate template = mock(McpServerResourceTemplate.class);
        when(template.uriTemplate()).thenReturn(uriTemplate);
        when(template.name()).thenReturn("camel_template");
        when(template.mimeType()).thenReturn("text/plain");
        return template;
    }

    private static McpServerPrompt prompt(String name) {
        McpServerPrompt prompt = mock(McpServerPrompt.class);
        when(prompt.name()).thenReturn(name);
        when(prompt.arguments()).thenReturn(List.of());
        return prompt;
    }
}
