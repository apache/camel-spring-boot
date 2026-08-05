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

import java.util.Map;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.component.mcp.server.McpServerInfo;
import org.apache.camel.component.mcp.server.McpServerTool;
import org.apache.camel.component.mcp.server.McpToolCallResult;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link McpServerEngine} publishing tools into the Spring AI MCP server: {@code toolAdded}/{@code toolRemoved} map to
 * the auto-configured {@link McpSyncServer}'s {@code addTool}/{@code removeTool}, which emit
 * {@code notifications/tools/list_changed} to connected clients. Serving concerns (endpoint path, protocol, server
 * identity, authentication) are owned by the Spring AI MCP server configuration ({@code spring.ai.mcp.server.*}).
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
        McpSchema.Tool mcpTool = McpSchema.Tool.builder(tool.name(), jsonMapper, schema)
                .description(tool.description())
                .build();
        McpServerFeatures.SyncToolSpecification spec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(mcpTool)
                .callHandler((exchange, request) -> {
                    Map<String, Object> arguments = request.arguments() != null ? request.arguments() : Map.of();
                    McpToolCallResult result = tool.handler().call(arguments);
                    return McpSchema.CallToolResult.builder()
                            .addTextContent(result.text())
                            .isError(result.isError())
                            .build();
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
}
