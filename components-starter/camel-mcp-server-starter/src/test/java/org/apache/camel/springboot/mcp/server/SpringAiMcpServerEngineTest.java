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

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.component.mcp.server.McpResourceTemplateReadHandler;
import org.apache.camel.component.mcp.server.McpServerRefusedException;
import org.apache.camel.component.mcp.server.McpServerResourceTemplate;
import org.apache.camel.component.mcp.server.McpServerTool;
import org.apache.camel.component.mcp.server.McpToolCallHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiMcpServerEngineTest {

    @Test
    void reportsResourcesAndPromptsFromActualServerCapabilities() {
        McpSyncServer server = mock(McpSyncServer.class);
        SpringAiMcpServerEngine engine = new SpringAiMcpServerEngine(server);

        when(server.getServerCapabilities()).thenReturn(
                new McpSchema.ServerCapabilities(null, null, null, null, null, null));

        assertThat(engine.supportsResources()).isFalse();
        assertThat(engine.supportsResourceTemplates()).isFalse();
        assertThat(engine.supportsPrompts()).isFalse();

        when(server.getServerCapabilities()).thenReturn(
                new McpSchema.ServerCapabilities(
                        null,
                        null,
                        null,
                        new McpSchema.ServerCapabilities.PromptCapabilities(true),
                        new McpSchema.ServerCapabilities.ResourceCapabilities(false, true),
                        null));

        assertThat(engine.supportsResources()).isTrue();
        assertThat(engine.supportsResourceTemplates()).isTrue();
        assertThat(engine.supportsPrompts()).isTrue();
    }
    @Test
    void refusesOverlappingSpringDefinedResourceTemplates() {
        McpSyncServer server = mock(McpSyncServer.class);
        SpringAiMcpServerEngine engine = new SpringAiMcpServerEngine(server);
        McpSchema.ResourceTemplate existing = McpSchema.ResourceTemplate
                .builder("camel:///customers/{id}", "spring-customers")
                .build();
        when(server.listResourceTemplates()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> engine.resourceTemplateAdded(
                resourceTemplate("camel:///customers/{customerId}")))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("overlaps")
                .hasMessageContaining("camel:///customers/{id}");

        verify(server, never()).addResourceTemplate(any());
    }

    @Test
    void ignoresUnsupportedSpringTemplateWhenUriSchemesDiffer() {
        McpSyncServer server = mock(McpSyncServer.class);
        SpringAiMcpServerEngine engine = new SpringAiMcpServerEngine(server);
        McpSchema.ResourceTemplate existing = McpSchema.ResourceTemplate
                .builder("document://{id}", "spring-document")
                .build();
        when(server.listResourceTemplates()).thenReturn(List.of(existing));

        engine.resourceTemplateAdded(resourceTemplate("camel:///customers/{customerId}"));

        verify(server).addResourceTemplate(any());
    }

    @Test
    void refusesUnsupportedSpringTemplateWhenUriSchemesMatch() {
        McpSyncServer server = mock(McpSyncServer.class);
        SpringAiMcpServerEngine engine = new SpringAiMcpServerEngine(server);
        McpSchema.ResourceTemplate existing = McpSchema.ResourceTemplate
                .builder("camel://{id}", "spring-camel")
                .build();
        when(server.listResourceTemplates()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> engine.resourceTemplateAdded(
                resourceTemplate("camel:///customers/{customerId}")))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("Cannot safely check")
                .hasCauseInstanceOf(IllegalArgumentException.class);

        verify(server, never()).addResourceTemplate(any());
    }

    @Test
    void translatesInvalidToolSchemaIntoServerRefusal() {
        McpSyncServer server = mock(McpSyncServer.class);
        SpringAiMcpServerEngine engine = new SpringAiMcpServerEngine(server);
        when(server.listTools()).thenReturn(List.of());
        doThrow(new IllegalArgumentException("invalid JSON schema"))
                .when(server).addTool(any(McpServerFeatures.SyncToolSpecification.class));

        assertThatThrownBy(() -> engine.toolAdded(toolWithInvalidOutputSchema()))
                .isInstanceOf(McpServerRefusedException.class)
                .hasMessageContaining("invalid input or output schema")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removesOnlyARegisteredTemplateAndLeavesTheNotificationToTheSdk() {
        String uriTemplate = "camel:///customers/{id}";
        McpSchema.ResourceTemplate template = McpSchema.ResourceTemplate
                .builder(uriTemplate, "customers")
                .build();

        // The MCP Java SDK sends resources/list_changed itself when it removes a template
        McpSyncServer registeredServer = mock(McpSyncServer.class);
        when(registeredServer.listResourceTemplates()).thenReturn(List.of(template));
        when(registeredServer.getServerCapabilities()).thenReturn(
                new McpSchema.ServerCapabilities(
                        null, null, null, null,
                        new McpSchema.ServerCapabilities.ResourceCapabilities(false, true), null));
        new SpringAiMcpServerEngine(registeredServer).resourceTemplateRemoved(uriTemplate);
        verify(registeredServer).removeResourceTemplate(uriTemplate);
        verify(registeredServer, never()).notifyResourcesListChanged();

        McpSyncServer absentServer = mock(McpSyncServer.class);
        when(absentServer.listResourceTemplates()).thenReturn(List.of());
        when(absentServer.getServerCapabilities()).thenReturn(
                new McpSchema.ServerCapabilities(
                        null, null, null, null,
                        new McpSchema.ServerCapabilities.ResourceCapabilities(false, true), null));
        new SpringAiMcpServerEngine(absentServer).resourceTemplateRemoved(uriTemplate);
        verify(absentServer, never()).removeResourceTemplate(uriTemplate);
        verify(absentServer, never()).notifyResourcesListChanged();
    }

    private static McpServerResourceTemplate resourceTemplate(String uriTemplate) {
        return new McpServerResourceTemplate() {
            @Override
            public String uriTemplate() {
                return uriTemplate;
            }

            @Override
            public String name() {
                return "customer-template";
            }

            @Override
            public String description() {
                return "Customer selected by id";
            }

            @Override
            public String mimeType() {
                return "text/plain";
            }

            @Override
            public McpResourceTemplateReadHandler handler() {
                return null;
            }
        };
    }

    private static McpServerTool toolWithInvalidOutputSchema() {
        return new McpServerTool() {
            @Override
            public String name() {
                return "invalid_schema_tool";
            }

            @Override
            public String description() {
                return "Tool with invalid output schema";
            }

            @Override
            public String inputSchemaJson() {
                return "{\"type\":\"object\"}";
            }

            @Override
            public Map<String, org.apache.camel.component.ai.tool.AiToolParameterHelper.ParameterDef> parameters() {
                return Map.of();
            }

            @Override
            public McpToolCallHandler handler() {
                return null;
            }

            @Override
            public String outputSchemaJson() {
                return "{\"type\":\"obj\"}";
            }
        };
    }

}
