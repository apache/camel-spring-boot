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
import java.util.function.BiFunction;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.resource.AiResourceUriTemplate;
import org.apache.camel.component.ai.tool.AiToolAnnotations;
import org.apache.camel.component.mcp.server.McpPromptMessage;
import org.apache.camel.component.mcp.server.McpPromptResult;
import org.apache.camel.component.mcp.server.McpResourceReadResult;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.component.mcp.server.McpServerInfo;
import org.apache.camel.component.mcp.server.McpServerPrompt;
import org.apache.camel.component.mcp.server.McpServerRefusedException;
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
 * {@code notifications/tools/list_changed} to connected clients. With {@code spring.ai.mcp.server.protocol=STATELESS},
 * Spring AI auto-configures an {@link McpStatelessSyncServer} instead: every request is served on its own, without a
 * session, and clients are not notified of changes. Serving concerns (endpoint path, protocol, server identity) are
 * owned by the Spring AI MCP server configuration ({@code spring.ai.mcp.server.*}), which provides no authentication -
 * the endpoint must be protected by the application.
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

    private final ServerOperations server;
    private final McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();
    private CamelContext camelContext;

    public SpringAiMcpServerEngine(McpSyncServer server) {
        this.server = new SessionServerOperations(server);
    }

    /**
     * Publishes into the Spring AI stateless MCP server ({@code spring.ai.mcp.server.protocol=STATELESS}).
     */
    public SpringAiMcpServerEngine(McpStatelessSyncServer server) {
        this.server = new StatelessServerOperations(server);
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
        if (server.listTools().stream().anyMatch(existing -> existing.name().equals(tool.name()))) {
            throw new McpServerRefusedException(
                    "An MCP tool named '" + tool.name() + "' is already registered");
        }

        String schema = tool.inputSchemaJson() != null ? tool.inputSchemaJson() : EMPTY_OBJECT_SCHEMA;
        try {
            McpSchema.Tool.Builder builder = McpSchema.Tool.builder(tool.name(), jsonMapper, schema)
                    .description(tool.description());

            if (tool.outputSchemaJson() != null && !tool.outputSchemaJson().isBlank()) {
                builder.outputSchema(jsonMapper, tool.outputSchemaJson());
            }

            applyAnnotations(builder, tool.annotations());
            McpSchema.Tool mcpTool = builder.build();
            server.addTool(mcpTool, (transportContext, request) -> {
                Map<String, Object> arguments = request.arguments() != null ? request.arguments() : Map.of();
                McpToolCallResult result = tool.handler().call(
                        arguments, SpringAiMcpTransportContext.from(transportContext));
                McpSchema.CallToolResult.Builder resultBuilder = McpSchema.CallToolResult.builder()
                        .addTextContent(result.text())
                        .isError(result.isError());

                if (result.structuredContent() != null) {
                    resultBuilder.structuredContent(result.structuredContent());
                }

                return resultBuilder.build();
            });
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException(
                    "MCP tool '" + tool.name() + "' has an invalid input or output schema: " + e.getMessage(), e);
        }
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
        if (server.listResources().stream().anyMatch(existing -> existing.uri().equals(resource.uri()))) {
            throw new McpServerRefusedException(
                    "An MCP resource with URI '" + resource.uri() + "' is already registered");
        }

        McpSchema.Resource mcpResource = buildMcpResource(resource);

        server.addResource(
                        mcpResource,
                        (transportContext, request) -> {
                            McpResourceReadResult result = resource.handler().read();

                            if (result.isError()) {
                                throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                        .message(result.errorMessage())
                                        .build();
                            }

                            McpSchema.ResourceContents contents;

                            if (result.blob() != null) {
                                contents = McpSchema.BlobResourceContents.builder(
                                resource.uri(),
                                Base64.getEncoder().encodeToString(result.blob()))
                                .mimeType(resource.mimeType())
                                .build();
                            } else {
                                contents = McpSchema.TextResourceContents.builder(resource.uri(), result.text())
                                .mimeType(resource.mimeType())
                                .build();
                            }

                            return McpSchema.ReadResourceResult.builder(List.of(contents)).build();
                        });
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
        McpSchema.ServerCapabilities capabilities = server.capabilities();
        return capabilities != null && capabilities.resources() != null;
    }

    @Override
    public void resourceTemplateAdded(McpServerResourceTemplate resourceTemplate) {
        List<McpSchema.ResourceTemplate> existingTemplates = server.listResourceTemplates();
        if (existingTemplates.stream()
                .anyMatch(existing -> existing.uriTemplate().equals(resourceTemplate.uriTemplate()))) {
            throw new McpServerRefusedException(
                    "An MCP resource template '" + resourceTemplate.uriTemplate() + "' is already registered");
        }

        AiResourceUriTemplate candidate;
        try {
            candidate = new AiResourceUriTemplate(resourceTemplate.uriTemplate());
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException(
                    "Invalid MCP resource template '" + resourceTemplate.uriTemplate() + "': " + e.getMessage(), e);
        }

        for (McpSchema.ResourceTemplate existing : existingTemplates) {
            AiResourceUriTemplate existingMatcher;
            try {
                existingMatcher = new AiResourceUriTemplate(existing.uriTemplate());
            } catch (IllegalArgumentException e) {
                String candidateScheme = uriScheme(resourceTemplate.uriTemplate());
                String existingScheme = uriScheme(existing.uriTemplate());

                // Unsupported Spring URI-template syntax in another scheme cannot
                // overlap with this Camel template. For the same or an unknown
                // scheme, retain the fail-closed behavior.
                if (candidateScheme != null && existingScheme != null
                    && !candidateScheme.equalsIgnoreCase(existingScheme)) {
                    LOG.warn(
                            "Skipping overlap check between MCP resource template '{}' and unsupported "
                                    + "Spring template '{}' because their URI schemes differ",
                            resourceTemplate.uriTemplate(), existing.uriTemplate());
                    continue;
                }

                throw new McpServerRefusedException(
                    "Cannot safely check whether MCP resource template '" + resourceTemplate.uriTemplate() +
                    "' overlaps existing Spring template '" + existing.uriTemplate() + "'", e);
            }
            if (candidate.overlaps(existingMatcher)) {
                throw new McpServerRefusedException(
                        "MCP resource template '" + resourceTemplate.uriTemplate()
                                + "' overlaps existing template '" + existing.uriTemplate() + "'");
            }
        }

        McpSchema.ResourceTemplate.Builder builder = McpSchema.ResourceTemplate
                .builder(resourceTemplate.uriTemplate(), resourceTemplate.name())
                .description(resourceTemplate.description())
                .mimeType(resourceTemplate.mimeType());

        if (resourceTemplate.title() != null && !resourceTemplate.title().isBlank()) {
            builder.title(resourceTemplate.title());
        }

        server.addResourceTemplate(
                        builder.build(),
                        (transportContext, request) -> {
                            McpResourceReadResult result =
                                    resourceTemplate.handler().read(request.uri());

                            if (result.isError()) {
                                throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                        .message(result.errorMessage())
                                        .build();
                            }

                            McpSchema.ResourceContents contents;

                            if (result.blob() != null) {
                                contents = McpSchema.BlobResourceContents.builder(
                                request.uri(),
                                Base64.getEncoder().encodeToString(result.blob()))
                                .mimeType(resourceTemplate.mimeType())
                                .build();
                            } else {
                                contents = McpSchema.TextResourceContents.builder(request.uri(), result.text())
                                .mimeType(resourceTemplate.mimeType())
                                .build();
                            }

                            return McpSchema.ReadResourceResult.builder(List.of(contents)).build();
                        });
        LOG.debug("MCP resource template added: {}", resourceTemplate.uriTemplate());
    }

    @Override
    public void resourceTemplateRemoved(String uriTemplate) {
        try {
            boolean registered = server.listResourceTemplates().stream()
                    .anyMatch(existing -> existing.uriTemplate().equals(uriTemplate));
            if (!registered) {
                LOG.debug("MCP resource template was not registered; nothing to remove: {}", uriTemplate);
                return;
            }

            server.removeResourceTemplate(uriTemplate);

            McpSchema.ServerCapabilities capabilities = server.capabilities();
            if (capabilities != null && capabilities.resources() != null
                    && Boolean.TRUE.equals(capabilities.resources().listChanged())) {
                // MCP Java SDK 2.1.0 does not notify on template removal; notify only when advertised.
                server.notifyResourcesListChanged();
            }

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
        McpSchema.ServerCapabilities capabilities = server.capabilities();
        return capabilities != null && capabilities.resources() != null;
    }

    @Override
    public void promptAdded(McpServerPrompt prompt) {
        if (server.listPrompts().stream().anyMatch(existing -> existing.name().equals(prompt.name()))) {
            throw new McpServerRefusedException(
                    "An MCP prompt named '" + prompt.name() + "' is already registered");
        }

        server.addPrompt(buildPrompt(prompt), (transportContext, request) -> getPrompt(prompt, request));
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
        McpSchema.ServerCapabilities capabilities = server.capabilities();
        return capabilities != null && capabilities.prompts() != null;
    }

    private static McpSchema.Resource buildMcpResource(McpServerResource resource) {
        McpSchema.Resource.Builder builder = McpSchema.Resource.builder(resource.uri(), resource.name())
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
    private static McpSchema.Prompt buildPrompt(McpServerPrompt prompt) {

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

        return builder.build();
    }

    private McpSchema.GetPromptResult getPrompt(McpServerPrompt prompt, McpSchema.GetPromptRequest request) {
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

    private static String uriScheme(String uriTemplate) {
        if (uriTemplate == null) {
            return null;
        }

        int colon = uriTemplate.indexOf(':');
        if (colon <= 0) {
            return null;
        }

        String scheme = uriTemplate.substring(0, colon);
        return scheme.matches("[A-Za-z][A-Za-z0-9+.-]*") ? scheme : null;
    }

    /**
     * The calls the engine makes on a Spring AI MCP server, whether it keeps sessions or not. Every handler receives
     * the MCP transport context of the request, which carries the caller identity.
     */
    private interface ServerOperations {

        McpSchema.ServerCapabilities capabilities();

        List<McpSchema.Tool> listTools();

        void addTool(
                McpSchema.Tool tool,
                BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler);

        void removeTool(String name);

        List<McpSchema.Resource> listResources();

        void addResource(
                McpSchema.Resource resource,
                BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler);

        void removeResource(String uri);

        List<McpSchema.ResourceTemplate> listResourceTemplates();

        void addResourceTemplate(
                McpSchema.ResourceTemplate template,
                BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler);

        void removeResourceTemplate(String uriTemplate);

        List<McpSchema.Prompt> listPrompts();

        void addPrompt(
                McpSchema.Prompt prompt,
                BiFunction<McpTransportContext, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> handler);

        void removePrompt(String name);

        void notifyResourcesListChanged();
    }

    /**
     * The Spring AI MCP server that keeps a session per client ({@code SSE} or {@code STREAMABLE}).
     */
    private record SessionServerOperations(McpSyncServer server) implements ServerOperations {

        private static McpTransportContext transportContext(
                McpSyncServerExchange exchange) {
            return exchange != null ? exchange.transportContext() : null;
        }

        @Override
        public McpSchema.ServerCapabilities capabilities() {
            return server.getServerCapabilities();
        }

        @Override
        public List<McpSchema.Tool> listTools() {
            return server.listTools();
        }

        @Override
        public void addTool(
                McpSchema.Tool tool,
                BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler) {
            server.addTool(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler((exchange, request) -> handler.apply(transportContext(exchange), request))
                    .build());
        }

        @Override
        public void removeTool(String name) {
            server.removeTool(name);
        }

        @Override
        public List<McpSchema.Resource> listResources() {
            return server.listResources();
        }

        @Override
        public void addResource(
                McpSchema.Resource resource,
                BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler) {
            server.addResource(new McpServerFeatures.SyncResourceSpecification(
                    resource, (exchange, request) -> handler.apply(transportContext(exchange), request)));
        }

        @Override
        public void removeResource(String uri) {
            server.removeResource(uri);
        }

        @Override
        public List<McpSchema.ResourceTemplate> listResourceTemplates() {
            return server.listResourceTemplates();
        }

        @Override
        public void addResourceTemplate(
                McpSchema.ResourceTemplate template,
                BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler) {
            server.addResourceTemplate(new McpServerFeatures.SyncResourceTemplateSpecification(
                    template, (exchange, request) -> handler.apply(transportContext(exchange), request)));
        }

        @Override
        public void removeResourceTemplate(String uriTemplate) {
            server.removeResourceTemplate(uriTemplate);
        }

        @Override
        public List<McpSchema.Prompt> listPrompts() {
            return server.listPrompts();
        }

        @Override
        public void addPrompt(
                McpSchema.Prompt prompt,
                BiFunction<McpTransportContext, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> handler) {
            server.addPrompt(new McpServerFeatures.SyncPromptSpecification(
                    prompt, (exchange, request) -> handler.apply(transportContext(exchange), request)));
        }

        @Override
        public void removePrompt(String name) {
            server.removePrompt(name);
        }

        @Override
        public void notifyResourcesListChanged() {
            server.notifyResourcesListChanged();
        }
    }

    /**
     * The Spring AI stateless MCP server ({@code STATELESS}): no session, so no notification can reach a client.
     */
    private record StatelessServerOperations(McpStatelessSyncServer server) implements ServerOperations {

        @Override
        public McpSchema.ServerCapabilities capabilities() {
            return server.getServerCapabilities();
        }

        @Override
        public List<McpSchema.Tool> listTools() {
            return server.listTools();
        }

        @Override
        public void addTool(
                McpSchema.Tool tool,
                BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler) {
            server.addTool(McpStatelessServerFeatures.SyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler(handler)
                    .build());
        }

        @Override
        public void removeTool(String name) {
            server.removeTool(name);
        }

        @Override
        public List<McpSchema.Resource> listResources() {
            return server.listResources();
        }

        @Override
        public void addResource(
                McpSchema.Resource resource,
                BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler) {
            server.addResource(new McpStatelessServerFeatures.SyncResourceSpecification(resource, handler));
        }

        @Override
        public void removeResource(String uri) {
            server.removeResource(uri);
        }

        @Override
        public List<McpSchema.ResourceTemplate> listResourceTemplates() {
            return server.listResourceTemplates();
        }

        @Override
        public void addResourceTemplate(
                McpSchema.ResourceTemplate template,
                BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler) {
            server.addResourceTemplate(new McpStatelessServerFeatures.SyncResourceTemplateSpecification(template, handler));
        }

        @Override
        public void removeResourceTemplate(String uriTemplate) {
            server.removeResourceTemplate(uriTemplate);
        }

        @Override
        public List<McpSchema.Prompt> listPrompts() {
            return server.listPrompts();
        }

        @Override
        public void addPrompt(
                McpSchema.Prompt prompt,
                BiFunction<McpTransportContext, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> handler) {
            server.addPrompt(new McpStatelessServerFeatures.SyncPromptSpecification(prompt, handler));
        }

        @Override
        public void removePrompt(String name) {
            server.removePrompt(name);
        }

        @Override
        public void notifyResourcesListChanged() {
            // a stateless server has no session to notify
        }
    }

}
