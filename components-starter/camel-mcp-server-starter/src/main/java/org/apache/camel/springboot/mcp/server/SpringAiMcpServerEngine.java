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

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.tool.AiToolAnnotations;
import org.apache.camel.component.mcp.server.McpPromptMessage;
import org.apache.camel.component.mcp.server.McpPromptResult;
import org.apache.camel.component.mcp.server.McpResourceReadResult;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.component.mcp.server.McpServerInfo;
import org.apache.camel.component.mcp.server.McpServerPrompt;
import org.apache.camel.component.mcp.server.McpServerResource;
import org.apache.camel.component.mcp.server.McpServerResourceTemplate;
import org.apache.camel.component.mcp.server.McpServerTool;
import org.apache.camel.component.mcp.server.McpToolCallResult;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link McpServerEngine} publishing tools into the Spring AI MCP server: {@code toolAdded}/{@code toolRemoved} map to
 * the auto-configured {@link McpSyncServer}'s {@code addTool}/{@code removeTool}, which emit
 * {@code notifications/tools/list_changed} to connected clients. Serving concerns (endpoint path, protocol, server
 * identity) are owned by the Spring AI MCP server configuration ({@code spring.ai.mcp.server.*}), which provides
 * no authentication - the endpoint must be protected by the application.
 */
public class SpringAiMcpServerEngine extends ServiceSupport implements McpServerEngine {

    private static final Logger LOG = LoggerFactory.getLogger(SpringAiMcpServerEngine.class);

    private static final String EMPTY_OBJECT_SCHEMA = """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": false
            }
            """;

    private final McpSyncServer server;
    private final McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();
    private CamelContext camelContext;

    public SpringAiMcpServerEngine(McpSyncServer server) {
        this.server = server;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void initialize(McpServerInfo info) {
        // serving identity is owned by spring.ai.mcp.server.*; the hint is deliberately ignored
        LOG.debug("Spring AI MCP server engine initialized; serving configuration is owned by spring.ai.mcp.server.*");
    }

    @Override
    public void toolAdded(McpServerTool tool) {
        String schema = tool.inputSchemaJson() != null ? tool.inputSchemaJson() : EMPTY_OBJECT_SCHEMA;
        McpSchema.Tool.Builder builder = McpSchema.Tool.builder(tool.name(), jsonMapper, schema)
                .description(tool.description());

        if (tool.outputSchemaJson() != null && !tool.outputSchemaJson().isBlank()) {
            builder.outputSchema(jsonMapper, tool.outputSchemaJson());
        }

        applyAnnotations(builder, tool.annotations());
        McpSchema.Tool mcpTool = builder.build();
        McpServerFeatures.SyncToolSpecification spec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(mcpTool)
                .callHandler((exchange, request) -> {
                    Map<String, Object> arguments = request.arguments() != null ? request.arguments() : Map.of();
                    McpToolCallResult result = tool.handler().call(
                            arguments, SpringAiMcpTransportContext.from(exchange));
                    McpSchema.CallToolResult.Builder resultBuilder = McpSchema.CallToolResult.builder()
                            .addTextContent(result.text())
                            .isError(result.isError());

                    if (result.structuredContent() != null) {
                        resultBuilder.structuredContent(result.structuredContent());
                    }

                    return resultBuilder.build();
                })
                .build();
        server.addTool(spec);
        LOG.debug("MCP tool added: {}", tool.name());
    }

    @Override
    public void toolRemoved(String toolName) {
        try {
            server.removeTool(toolName);
            LOG.debug("MCP tool removed: {}", toolName);
        } catch (Exception e) {
            LOG.debug("Failed to remove MCP tool {}: {}", toolName, e.getMessage());
        }
    }

    @Override
    public void resourceAdded(McpServerResource resource) {
        McpSchema.Resource mcpResource = buildMcpResource(resource);

        McpServerFeatures.SyncResourceSpecification spec =
                new McpServerFeatures.SyncResourceSpecification(
                        mcpResource,
                        (exchange, request) -> {
                            McpResourceReadResult result = resource.handler().read();

                            if (result.isError()) {
                                throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                        .message(result.errorMessage())
                                        .build();
                            }

                            McpSchema.ResourceContents contents;

                            if (result.blob() != null) {
                                contents = new McpSchema.BlobResourceContents(
                                        resource.uri(),
                                        resource.mimeType(),
                                        Base64.getEncoder().encodeToString(result.blob()));
                            } else {
                                contents = new McpSchema.TextResourceContents(
                                        resource.uri(),
                                        resource.mimeType(),
                                        result.text());
                            }

                            return new McpSchema.ReadResourceResult(List.of(contents));
                        });

        server.addResource(spec);
        LOG.debug("MCP resource added: {}", resource.uri());
    }

    @Override
    public void resourceRemoved(String resourceUri) {
        try {
            server.removeResource(resourceUri);
            LOG.debug("MCP resource removed: {}", resourceUri);
        } catch (Exception e) {
            LOG.debug("Failed to remove MCP resource {}: {}", resourceUri, e.getMessage());
        }
    }

    @Override
    public boolean supportsResources() {
        return true;
    }

    @Override
    public void resourceTemplateAdded(McpServerResourceTemplate resourceTemplate) {
        McpSchema.ResourceTemplate.Builder builder = McpSchema.ResourceTemplate
                .builder(resourceTemplate.uriTemplate(), resourceTemplate.name())
                .description(resourceTemplate.description())
                .mimeType(resourceTemplate.mimeType());

        if (resourceTemplate.title() != null && !resourceTemplate.title().isBlank()) {
            builder.title(resourceTemplate.title());
        }

        McpServerFeatures.SyncResourceTemplateSpecification spec =
                new McpServerFeatures.SyncResourceTemplateSpecification(
                        builder.build(),
                        (exchange, request) -> {
                            McpResourceReadResult result =
                                    resourceTemplate.handler().read(request.uri());

                            if (result.isError()) {
                                throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                        .message(result.errorMessage())
                                        .build();
                            }

                            McpSchema.ResourceContents contents;

                            if (result.blob() != null) {
                                contents = new McpSchema.BlobResourceContents(
                                        request.uri(),
                                        resourceTemplate.mimeType(),
                                        Base64.getEncoder().encodeToString(result.blob()));
                            } else {
                                contents = new McpSchema.TextResourceContents(
                                        request.uri(),
                                        resourceTemplate.mimeType(),
                                        result.text());
                            }

                            return new McpSchema.ReadResourceResult(List.of(contents));
                        });

        server.addResourceTemplate(spec);
        LOG.debug("MCP resource template added: {}", resourceTemplate.uriTemplate());
    }

    @Override
    public void resourceTemplateRemoved(String uriTemplate) {
        try {
            server.removeResourceTemplate(uriTemplate);

            // The MCP SDK does not emit resources/list_changed for template removal.
            // Keep this explicit notification so clients see route lifecycle changes.
            server.notifyResourcesListChanged();

            LOG.debug("MCP resource template removed: {}", uriTemplate);
        } catch (Exception e) {
            LOG.debug(
                    "Failed to remove MCP resource template {} or notify clients: {}",
                    uriTemplate,
                    e.getMessage());
        }
    }

    @Override
    public boolean supportsResourceTemplates() {
        return true;
    }

    @Override
    public void promptAdded(McpServerPrompt prompt) {
        server.addPrompt(buildPromptSpecification(prompt));
        LOG.debug("MCP prompt added: {}", prompt.name());
    }

    @Override
    public void promptRemoved(String promptName) {
        try {
            server.removePrompt(promptName);
            LOG.debug("MCP prompt removed: {}", promptName);
        } catch (Exception e) {
            LOG.debug("Failed to remove MCP prompt {}: {}", promptName, e.getMessage());
        }
    }

    @Override
    public boolean supportsPrompts() {
        return true;
    }

    private static McpSchema.Resource buildMcpResource(McpServerResource resource) {
        McpSchema.Resource.Builder builder = McpSchema.Resource.builder()
                .uri(resource.uri())
                .name(resource.name())
                .description(resource.description())
                .mimeType(resource.mimeType());

        if (resource.title() != null && !resource.title().isBlank()) {
            builder.title(resource.title());
        }

        return builder.build();
    }

    /*
     * This adapter uses the MCP Java SDK types directly. The equivalent Camel Main
     * helper lives in camel-mcp-server, which is deliberately not a dependency here.
     */
    private McpServerFeatures.SyncPromptSpecification buildPromptSpecification(
            McpServerPrompt prompt) {

        McpSchema.Prompt.Builder builder = McpSchema.Prompt.builder(prompt.name())
                .description(prompt.description())
                .arguments(prompt.arguments().stream()
                        .map(argument -> McpSchema.PromptArgument.builder(argument.name())
                                .description(argument.description())
                                .required(argument.required())
                                .build())
                        .toList());

        if (prompt.title() != null && !prompt.title().isBlank()) {
            builder.title(prompt.title());
        }

        return new McpServerFeatures.SyncPromptSpecification(
                builder.build(),
                (exchange, request) -> {
                    Map<String, String> arguments = new LinkedHashMap<>();

                    if (request.arguments() != null) {
                        request.arguments().forEach((name, value) -> {
                            if (!(value instanceof String stringValue)) {
                                throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                                        .message("Prompt arguments must be strings")
                                        .build();
                            }

                            arguments.put(name, stringValue);
                        });
                    }

                    McpPromptResult result = prompt.handler().get(arguments);

                    if (result == null) {
                        throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                .message("Prompt execution failed")
                                .build();
                    }

                    if (result.isError()) {
                        int code = result.argumentError()
                                ? McpSchema.ErrorCodes.INVALID_PARAMS
                                : McpSchema.ErrorCodes.INTERNAL_ERROR;

                        throw McpError.builder(code)
                                .message(result.errorMessage())
                                .build();
                    }

                    List<McpSchema.PromptMessage> messages = result.messages().stream()
                            .map(this::toPromptMessage)
                            .toList();

                    return McpSchema.GetPromptResult.builder(messages)
                            .description(result.description())
                            .build();
                });
    }

    private McpSchema.PromptMessage toPromptMessage(McpPromptMessage message) {
        McpSchema.Role role;

        try {
            role = McpSchema.Role.valueOf(message.role().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message("Prompt returned an invalid message role")
                    .build();
        }

        Object value = message.content();
        McpSchema.Content content;

        if (value instanceof String text) {
            content = McpSchema.TextContent.builder(text).build();
        } else if (value instanceof McpSchema.Content mcpContent) {
            content = mcpContent;
        } else if (value instanceof Map<?, ?> map) {
            try {
                content = jsonMapper.convertValue(map, McpSchema.Content.class);
            } catch (RuntimeException e) {
                throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                        .message("Prompt returned an invalid MCP content block")
                        .build();
            }
        } else {
            throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message("Prompt returned an unsupported content value")
                    .build();
        }

        return McpSchema.PromptMessage.builder(role, content).build();
    }

    /**
     * Maps the ai-tool annotation hints to the MCP tool: {@code title} to the tool's top-level title and the boolean
     * hints to {@code ToolAnnotations}. Unset hints are not published, mirroring the Vert.x engine, so MCP clients
     * apply their own spec defaults.
     */
    private static void applyAnnotations(McpSchema.Tool.Builder builder, AiToolAnnotations annotations) {
        if (annotations == null) {
            return;
        }
        if (annotations.title() != null) {
            builder.title(annotations.title());
        }
        McpSchema.ToolAnnotations.Builder hintBuilder = McpSchema.ToolAnnotations.builder();
        boolean hasHints = false;
        if (annotations.readOnlyHint() != null) {
            hintBuilder.readOnlyHint(annotations.readOnlyHint());
            hasHints = true;
        }
        if (annotations.destructiveHint() != null) {
            hintBuilder.destructiveHint(annotations.destructiveHint());
            hasHints = true;
        }
        if (annotations.idempotentHint() != null) {
            hintBuilder.idempotentHint(annotations.idempotentHint());
            hasHints = true;
        }
        if (annotations.openWorldHint() != null) {
            hintBuilder.openWorldHint(annotations.openWorldHint());
            hasHints = true;
        }
        if (annotations.returnDirect() != null) {
            hintBuilder.returnDirect(annotations.returnDirect());
            hasHints = true;
        }
        if (hasHints) {
            builder.annotations(hintBuilder.build());
        }
    }
}
