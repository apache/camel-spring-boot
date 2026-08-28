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

import io.modelcontextprotocol.server.McpSyncServer;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mcp.server.McpServerBridge;
import org.apache.camel.component.mcp.server.McpServerConfiguration;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configures the Camel MCP server bridge over the Spring AI MCP server: every ai-tool route whose tags match
 * {@code camel.mcp-server.tags} is exposed as an MCP tool of the auto-configured {@link McpSyncServer}.
 */
@AutoConfiguration(after = CamelAutoConfiguration.class,
                   afterName = "org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration")
@ConditionalOnBean(McpSyncServer.class)
@ConditionalOnProperty(value = "camel.mcp-server.enabled", matchIfMissing = true)
@EnableConfigurationProperties(McpServerConfigurationProperties.class)
public class CamelMcpServerAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CamelMcpServerAutoConfiguration.class);

    @Bean(initMethod = "", destroyMethod = "")
    // Camel handles the lifecycle of this bean
    @ConditionalOnMissingBean(McpServerEngine.class)
    McpServerEngine camelMcpServerEngine(McpSyncServer mcpSyncServer) {
        return new SpringAiMcpServerEngine(mcpSyncServer);
    }

    @Bean(initMethod = "", destroyMethod = "")
    // Camel handles the lifecycle of this bean
    @ConditionalOnMissingBean(McpServerBridge.class)
    McpServerBridge camelMcpServerBridge(
            CamelContext camelContext, McpServerConfigurationProperties properties)
            throws Exception {
        McpServerConfiguration configuration = new McpServerConfiguration();
        configuration.setTags(properties.getTags());
        configuration.setToolTimeout(properties.getToolTimeout());
        McpServerBridge bridge = new McpServerBridge(configuration);
        camelContext.addService(bridge);
        if (properties.getTags() != null && !properties.getTags().isBlank()) {
            // exposing tools is opt-in, but once opted in the endpoint is reachable by anything that can reach
            // the application HTTP port - spring.ai.mcp.server.* has no authentication of its own
            LOG.info("Exposing ai-tool routes tagged [{}] as MCP tools. The MCP endpoint is served on the"
                     + " application HTTP port and is not authenticated by the Spring AI MCP server; secure it in"
                     + " the application, for example with a Spring Security filter chain on its path.",
                    properties.getTags());
        }
        return bridge;
    }
}
