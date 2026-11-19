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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.modern.McpException;
import io.modelcontextprotocol.modern.McpSchema;
import io.modelcontextprotocol.modern.McpSchema.ErrorCodes;
import io.modelcontextprotocol.modern.server.McpSyncResponse;
import io.modelcontextprotocol.modern.server.feature.McpSyncPrompt;
import io.modelcontextprotocol.modern.server.feature.McpSyncTool;
import io.modelcontextprotocol.modern.server.feature.ResourceTemplatesPage;
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
import org.springframework.ai.mcp.modern.InMemorySyncPromptRepository;
import org.springframework.ai.mcp.modern.InMemorySyncResourceRepository;
import org.springframework.ai.mcp.modern.InMemorySyncToolRepository;
import org.springframework.ai.mcp.modern.SyncResourceRegistration;
import org.springframework.ai.mcp.modern.SyncResourceTemplateRegistration;

/**
 * Publishes the Camel {@code ai-tool}, {@code ai-resource} and {@code ai-prompt} routes into the Spring AI MCP server
 * for protocol version 2026-07-28 ({@code spring.ai.mcp.server.protocol=MODERN}).
 * <p>
 * That server has no session: it takes its tools, resources and prompts from repositories. The routes go into the
 * in-memory repositories Spring AI auto-configures, next to the application's own tools, resources and prompts, and
 * those repositories tell the clients listening with {@code subscriptions/listen} when a list changes. A route with the
 * name or URI of an entry already served is refused. A repository the application defines itself cannot be added to,
 * so the routes of that kind are refused too.
 * <p>
 * The conversions mirror {@link SpringAiMcpServerEngine}, with the types of the 2026-07-28 schema of the MCP Java SDK.
 */
public class SpringAiModernMcpServerEngine extends ServiceSupport implements McpServerEngine {

    private static final Logger LOG = LoggerFactory.getLogger(SpringAiModernMcpServerEngine.class);

    private static final String EMPTY_OBJECT_SCHEMA = """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": false
            }
            """;

    private final InMemorySyncToolRepository tools;
    private final InMemorySyncResourceRepository resources;
    private final InMemorySyncPromptRepository prompts;
    private final McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();
    private CamelContext camelContext;

    /**
     * @param tools     the repository the tools are added to, or null when the server serves no Camel tools
     * @param resources the repository the resources and resource templates are added to, or null when the server
     *                  serves no Camel resources
     * @param prompts   the repository the prompts are added to, or null when the server serves no Camel prompts
     */
    public SpringAiModernMcpServerEngine(
            InMemorySyncToolRepository tools, InMemorySyncResourceRepository resources,
            InMemorySyncPromptRepository prompts) {
        this.tools = tools;
        this.resources = resources;
        this.prompts = prompts;
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
        LOG.debug("Spring AI MCP server engine for protocol version 2026-07-28 initialized; serving configuration is"
                  + " owned by spring.ai.mcp.server.*");
    }

    // ---- tools

    @Override
    public void toolAdded(McpServerTool tool) {
        if (tools == null) {
            throw new McpServerRefusedException(
                    "MCP tool '" + tool.name() + "' is not served: the Spring AI MCP server serves no tools, or"
                                                + " serves them from a repository of the application");
        }
        McpSchema.Tool definition;
        try {
            definition = buildTool(tool);
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException(
                    "MCP tool '" + tool.name() + "' has an invalid input or output schema: " + e.getMessage(), e);
        }
        try {
            tools.add(McpSyncTool.of(definition, (ctx, request) -> {
                Map<String, Object> arguments = request.arguments() != null ? request.arguments() : Map.of();
                McpToolCallResult result = tool.handler().call(
                        arguments, SpringAiMcpTransportContext.from(ctx.transportContext()));
                McpSchema.CallToolResult.Builder builder = McpSchema.CallToolResult.builder()
                        .addContent(McpSchema.TextContent.builder(result.text()).build())
                        .isError(result.isError());
                if (result.structuredContent() != null) {
                    builder.structuredContent(result.structuredContent());
                }
                return McpSyncResponse.result(builder.build());
            }));
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException("An MCP tool named '" + tool.name() + "' is already registered", e);
        }
        LOG.debug("MCP tool added: {}", tool.name());
    }

    @Override
    public void toolRemoved(String toolName) {
        if (tools != null && tools.remove(toolName)) {
            LOG.debug("MCP tool removed: {}", toolName);
        }
    }

    private McpSchema.Tool buildTool(McpServerTool tool) {
        String schema = tool.inputSchemaJson() != null ? tool.inputSchemaJson() : EMPTY_OBJECT_SCHEMA;
        McpSchema.Tool.Builder builder = McpSchema.Tool.builder(tool.name(), jsonMapper, schema)
                .description(tool.description());
        if (tool.outputSchemaJson() != null && !tool.outputSchemaJson().isBlank()) {
            builder.outputSchema(jsonMapper, tool.outputSchemaJson());
        }
        AiToolAnnotations annotations = tool.annotations();
        if (annotations != null) {
            if (annotations.title() != null) {
                builder.title(annotations.title());
            }
            // returnDirect is not part of the 2026-07-28 tool annotations; unset hints are not published, so MCP
            // clients apply their own spec defaults
            if (annotations.readOnlyHint() != null || annotations.destructiveHint() != null
                    || annotations.idempotentHint() != null || annotations.openWorldHint() != null) {
                builder.annotations(McpSchema.ToolAnnotations.builder()
                        .readOnlyHint(annotations.readOnlyHint())
                        .destructiveHint(annotations.destructiveHint())
                        .idempotentHint(annotations.idempotentHint())
                        .openWorldHint(annotations.openWorldHint())
                        .build());
            }
        }
        return builder.build();
    }

    // ---- resources and resource templates

    @Override
    public void resourceAdded(McpServerResource resource) {
        if (resources == null) {
            throw new McpServerRefusedException(
                    "MCP resource '" + resource.uri() + "' is not served: the Spring AI MCP server serves no"
                                                + " resources, or serves them from a repository of the application");
        }
        McpSchema.Resource.Builder builder = McpSchema.Resource.builder(resource.uri(), resource.name())
                .description(resource.description())
                .mimeType(resource.mimeType());
        if (resource.title() != null && !resource.title().isBlank()) {
            builder.title(resource.title());
        }
        try {
            resources.add(SyncResourceRegistration.of(builder.build(), (ctx, request) -> McpSyncResponse.result(
                    readResult(resource.uri(), resource.mimeType(), resource.handler().read()))));
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException(
                    "An MCP resource with URI '" + resource.uri() + "' is already registered", e);
        }
        LOG.debug("MCP resource added: {}", resource.uri());
    }

    @Override
    public void resourceRemoved(String resourceUri) {
        if (resources != null && resources.remove(resourceUri)) {
            LOG.debug("MCP resource removed: {}", resourceUri);
        }
    }

    @Override
    public boolean supportsResources() {
        return resources != null;
    }

    @Override
    public void resourceTemplateAdded(McpServerResourceTemplate resourceTemplate) {
        String uriTemplate = resourceTemplate.uriTemplate();
        if (resources == null) {
            throw new McpServerRefusedException(
                    "MCP resource template '" + uriTemplate + "' is not served: the Spring AI MCP server serves no"
                                                + " resources, or serves them from a repository of the application");
        }
        List<McpSchema.ResourceTemplate> existingTemplates = listResourceTemplates();
        if (existingTemplates.stream().anyMatch(existing -> existing.uriTemplate().equals(uriTemplate))) {
            throw new McpServerRefusedException(
                    "An MCP resource template '" + uriTemplate + "' is already registered");
        }
        checkNoOverlap(uriTemplate, existingTemplates);

        McpSchema.ResourceTemplate.Builder builder = McpSchema.ResourceTemplate.builder(uriTemplate,
                resourceTemplate.name())
                .description(resourceTemplate.description())
                .mimeType(resourceTemplate.mimeType());
        if (resourceTemplate.title() != null && !resourceTemplate.title().isBlank()) {
            builder.title(resourceTemplate.title());
        }
        try {
            resources.add(SyncResourceTemplateRegistration.of(builder.build(),
                    (ctx, request) -> McpSyncResponse.result(readResult(request.uri(), resourceTemplate.mimeType(),
                            resourceTemplate.handler().read(request.uri())))));
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException(
                    "MCP resource template '" + uriTemplate + "' cannot be registered: " + e.getMessage(), e);
        }
        LOG.debug("MCP resource template added: {}", uriTemplate);
    }

    @Override
    public void resourceTemplateRemoved(String uriTemplate) {
        if (resources != null && resources.remove(uriTemplate)) {
            LOG.debug("MCP resource template removed: {}", uriTemplate);
        }
    }

    @Override
    public boolean supportsResourceTemplates() {
        return resources != null;
    }

    /**
     * All the resource templates served, the application's included, across the pages of the repository.
     */
    private List<McpSchema.ResourceTemplate> listResourceTemplates() {
        List<McpSchema.ResourceTemplate> all = new ArrayList<>();
        String cursor = null;
        do {
            // the in-memory repository does not read the request context
            ResourceTemplatesPage page = resources.listTemplates(null, cursor);
            all.addAll(page.resourceTemplates());
            cursor = page.nextCursor();
        } while (cursor != null);
        return all;
    }

    /**
     * Refuses a template that matches a URI another template matches, as {@link SpringAiMcpServerEngine} does: which
     * template a read goes to would otherwise depend on the order the templates were registered in.
     */
    private static void checkNoOverlap(String uriTemplate, List<McpSchema.ResourceTemplate> existingTemplates) {
        AiResourceUriTemplate candidate;
        try {
            candidate = new AiResourceUriTemplate(uriTemplate);
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException(
                    "Invalid MCP resource template '" + uriTemplate + "': " + e.getMessage(), e);
        }
        for (McpSchema.ResourceTemplate existing : existingTemplates) {
            AiResourceUriTemplate existingMatcher;
            try {
                existingMatcher = new AiResourceUriTemplate(existing.uriTemplate());
            } catch (IllegalArgumentException e) {
                String candidateScheme = uriScheme(uriTemplate);
                String existingScheme = uriScheme(existing.uriTemplate());
                // a template of the application in a syntax Camel does not support cannot overlap a Camel template
                // of another scheme; for the same or an unknown scheme, refuse
                if (candidateScheme != null && existingScheme != null
                        && !candidateScheme.equalsIgnoreCase(existingScheme)) {
                    LOG.warn("Skipping overlap check between MCP resource template '{}' and unsupported Spring"
                             + " template '{}' because their URI schemes differ",
                            uriTemplate, existing.uriTemplate());
                    continue;
                }
                throw new McpServerRefusedException(
                        "Cannot safely check whether MCP resource template '" + uriTemplate
                                                    + "' overlaps existing Spring template '" + existing.uriTemplate()
                                                    + "'",
                        e);
            }
            if (candidate.overlaps(existingMatcher)) {
                throw new McpServerRefusedException(
                        "MCP resource template '" + uriTemplate + "' overlaps existing template '"
                                                    + existing.uriTemplate() + "'");
            }
        }
    }

    private static String uriScheme(String uriTemplate) {
        int colon = uriTemplate.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        String scheme = uriTemplate.substring(0, colon);
        return scheme.matches("[A-Za-z][A-Za-z0-9+.-]*") ? scheme : null;
    }

    private static McpSchema.ReadResourceResult readResult(String uri, String mimeType, McpResourceReadResult result) {
        if (result.isError()) {
            throw new McpException(ErrorCodes.INTERNAL_ERROR, result.errorMessage());
        }
        McpSchema.ResourceContents contents;
        if (result.blob() != null) {
            contents = McpSchema.BlobResourceContents.builder(uri, Base64.getEncoder().encodeToString(result.blob()))
                    .mimeType(mimeType)
                    .build();
        } else {
            contents = McpSchema.TextResourceContents.builder(uri, result.text()).mimeType(mimeType).build();
        }
        return McpSchema.ReadResourceResult.builder(List.of(contents)).build();
    }

    // ---- prompts

    @Override
    public void promptAdded(McpServerPrompt prompt) {
        if (prompts == null) {
            throw new McpServerRefusedException(
                    "MCP prompt '" + prompt.name() + "' is not served: the Spring AI MCP server serves no prompts,"
                                                + " or serves them from a repository of the application");
        }
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
        try {
            prompts.add(McpSyncPrompt.of(builder.build(), (ctx, request) -> getPrompt(prompt, request)));
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException("An MCP prompt named '" + prompt.name() + "' is already registered", e);
        }
        LOG.debug("MCP prompt added: {}", prompt.name());
    }

    @Override
    public void promptRemoved(String promptName) {
        if (prompts != null && prompts.remove(promptName)) {
            LOG.debug("MCP prompt removed: {}", promptName);
        }
    }

    @Override
    public boolean supportsPrompts() {
        return prompts != null;
    }

    private McpSyncResponse<McpSchema.GetPromptOutcome> getPrompt(
            McpServerPrompt prompt, McpSchema.GetPromptRequest request) {
        Map<String, String> arguments = request.arguments() != null ? request.arguments() : Map.of();
        McpPromptResult result = prompt.handler().get(arguments);
        if (result == null) {
            throw new McpException(ErrorCodes.INTERNAL_ERROR, "Prompt execution failed");
        }
        if (result.isError()) {
            throw new McpException(
                    result.argumentError() ? ErrorCodes.INVALID_PARAMS : ErrorCodes.INTERNAL_ERROR,
                    result.errorMessage());
        }
        List<McpSchema.PromptMessage> messages = result.messages().stream().map(this::toPromptMessage).toList();
        return McpSyncResponse.result(
                McpSchema.GetPromptResult.builder(messages).description(result.description()).build());
    }

    private McpSchema.PromptMessage toPromptMessage(McpPromptMessage message) {
        McpSchema.Role role;
        try {
            role = McpSchema.Role.valueOf(message.role().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new McpException(ErrorCodes.INTERNAL_ERROR, "Prompt returned an invalid message role");
        }
        Object value = message.content();
        McpSchema.Content content;
        if (value instanceof String text) {
            content = McpSchema.TextContent.builder(text).build();
        } else if (value instanceof McpSchema.Content modern) {
            content = modern;
        } else if (value != null) {
            // a map, or a content block of the MCP SDK's other schema: both have the same JSON form
            try {
                content = jsonMapper.convertValue(value, McpSchema.Content.class);
            } catch (RuntimeException e) {
                throw new McpException(ErrorCodes.INTERNAL_ERROR, "Prompt returned an invalid MCP content block");
            }
        } else {
            throw new McpException(ErrorCodes.INTERNAL_ERROR, "Prompt returned an unsupported content value");
        }
        return McpSchema.PromptMessage.builder(role, content).build();
    }
}
