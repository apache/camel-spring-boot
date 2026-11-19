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

import io.modelcontextprotocol.modern.server.McpServer;
import io.modelcontextprotocol.modern.server.feature.McpSyncPromptRepository;
import io.modelcontextprotocol.modern.server.feature.McpSyncResourceRepository;
import io.modelcontextprotocol.modern.server.feature.McpSyncToolRepository;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mcp.server.McpServerBridge;
import org.apache.camel.component.mcp.server.McpServerConfiguration;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.modern.InMemorySyncPromptRepository;
import org.springframework.ai.mcp.modern.InMemorySyncResourceRepository;
import org.springframework.ai.mcp.modern.InMemorySyncToolRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Auto-configures the Camel MCP server bridge over the Spring AI MCP server: every ai-tool route whose tags match
 * {@code camel.mcp-server.tags} is exposed as an MCP tool of the auto-configured {@link McpSyncServer}, or of the
 * {@link McpStatelessSyncServer} when {@code spring.ai.mcp.server.protocol=STATELESS}, or of the server for MCP
 * protocol version 2026-07-28 when {@code spring.ai.mcp.server.protocol=MODERN}.
 */
@AutoConfiguration(after = CamelAutoConfiguration.class,
                   afterName = {
                           "org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration",
                           "org.springframework.ai.mcp.server.common.autoconfigure.McpServerStatelessAutoConfiguration",
                           "org.springframework.ai.mcp.server.common.autoconfigure.McpServerModernAutoConfiguration" })
@Conditional(CamelMcpServerAutoConfiguration.OnSpringAiMcpServerCondition.class)
@ConditionalOnProperty(value = "camel.mcp-server.enabled", matchIfMissing = true)
@EnableConfigurationProperties(McpServerConfigurationProperties.class)
public class CamelMcpServerAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CamelMcpServerAutoConfiguration.class);

    @Bean(initMethod = "", destroyMethod = "")
    // Camel handles the lifecycle of this bean
    @ConditionalOnMissingBean(McpServerEngine.class)
    McpServerEngine camelMcpServerEngine(
            ObjectProvider<McpSyncServer> mcpSyncServer, ObjectProvider<McpStatelessSyncServer> mcpStatelessSyncServer,
            ObjectProvider<McpSyncToolRepository> toolRepository,
            ObjectProvider<McpSyncResourceRepository> resourceRepository,
            ObjectProvider<McpSyncPromptRepository> promptRepository) {
        McpSyncServer server = mcpSyncServer.getIfAvailable();
        if (server != null) {
            return new SpringAiMcpServerEngine(server);
        }
        McpStatelessSyncServer statelessServer = mcpStatelessSyncServer.getIfAvailable();
        if (statelessServer != null) {
            return new SpringAiMcpServerEngine(statelessServer);
        }
        return new SpringAiModernMcpServerEngine(
                inMemoryRepository(toolRepository, InMemorySyncToolRepository.class, "tools", "ai-tool"),
                inMemoryRepository(resourceRepository, InMemorySyncResourceRepository.class, "resources",
                        "ai-resource"),
                inMemoryRepository(promptRepository, InMemorySyncPromptRepository.class, "prompts", "ai-prompt"));
    }

    /**
     * The in-memory repository Spring AI auto-configured for the server of MCP protocol version 2026-07-28, which the
     * Camel routes are added to; null when the server serves none of these, or serves them from a repository of the
     * application, which Camel cannot add to.
     */
    private static <R, T extends R> T inMemoryRepository(
            ObjectProvider<R> repository, Class<T> inMemoryType, String kind, String routes) {
        R available = repository.getIfAvailable();
        if (available == null || inMemoryType.isInstance(available)) {
            return inMemoryType.cast(available);
        }
        LOG.warn("The Spring AI MCP server serves its {} from {}, a repository of the application: Camel {} routes"
                 + " cannot be added to it and are not served.",
                kind, available.getClass().getName(), routes);
        return null;
    }

    @Bean(initMethod = "", destroyMethod = "")
    // Camel handles the lifecycle of this bean
    @ConditionalOnMissingBean(McpServerBridge.class)
    McpServerBridge camelMcpServerBridge(
            CamelContext camelContext, McpServerConfigurationProperties properties, Environment environment)
            throws Exception {
        warnIfTransportIsNotActive(properties, environment);
        McpServerConfiguration configuration = new McpServerConfiguration();
        configuration.setTags(properties.getTags());
        configuration.setToolTimeout(properties.getToolTimeout());
        configuration.setResourceTimeout(properties.getResourceTimeout());
        configuration.setPromptTimeout(properties.getPromptTimeout());
        McpServerBridge bridge = new McpServerBridge(configuration);
        camelContext.addService(bridge);
        if (properties.getTags() != null && !properties.getTags().isBlank()) {
            // exposing tools is opt-in, but once opted in the endpoint is reachable by anything that can reach
            // the application HTTP port - spring.ai.mcp.server.* has no authentication of its own
            LOG.info("Exposing Camel MCP routes tagged [{}] as MCP capabilities. The MCP endpoint is served on the"
                     + " application HTTP port and is not authenticated by the Spring AI MCP server; secure it in"
                     + " the application, for example with a Spring Security filter chain on its path.",
                    properties.getTags());
        }
        return bridge;
    }
    private static void warnIfTransportIsNotActive(
            McpServerConfigurationProperties properties, Environment environment) {

        boolean tagsConfigured = StringUtils.hasText(properties.getTags());
        boolean originsConfigured = StringUtils.hasText(properties.getAllowedOrigins());
        boolean transportExplicitlyEnabled = Boolean.TRUE.equals(properties.getTransportEnabled());
        if (!tagsConfigured && !originsConfigured && !transportExplicitlyEnabled) {
            return;
        }

        String protocol = environment.getProperty("spring.ai.mcp.server.protocol");
        if (!"STREAMABLE".equalsIgnoreCase(protocol) && !"STATELESS".equalsIgnoreCase(protocol)
                && !"MODERN".equalsIgnoreCase(protocol)) {
            LOG.warn("Camel MCP routes or Origin settings are configured, but "
                     + "spring.ai.mcp.server.protocol is not STREAMABLE, STATELESS or MODERN. Camel's Origin "
                     + "validation and authenticated caller-context extraction are not active; configure "
                     + "spring.ai.mcp.server.protocol=STREAMABLE, STATELESS or MODERN to enable them.");
            return;
        }

        boolean transportActive = properties.getTransportEnabled() != null
                ? properties.getTransportEnabled()
                : tagsConfigured;
        if (!transportActive) {
            LOG.warn("Camel MCP routes or Origin settings are configured, but Camel's HTTP "
                     + "transport replacement is disabled. Origin validation and caller-context "
                     + "extraction are not installed. Set camel.mcp-server.transport-enabled=true "
                     + "or configure those features on the application-provided transport.");
        }
    }

    /**
     * Matches when Spring AI auto-configured a synchronous MCP server: with sessions ({@code SSE},
     * {@code STREAMABLE}), stateless ({@code STATELESS}), or for MCP protocol version 2026-07-28 ({@code MODERN}).
     */
    static final class OnSpringAiMcpServerCondition extends AnyNestedCondition {

        OnSpringAiMcpServerCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(McpSyncServer.class)
        static final class OnSessionServer {
        }

        @ConditionalOnBean(McpStatelessSyncServer.class)
        static final class OnStatelessServer {
        }

        // the server of 2026-07-28 is the same bean for both API types: the Camel routes need the synchronous
        // repositories
        @ConditionalOnBean(McpServer.class)
        @ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "type", havingValue = "SYNC",
                               matchIfMissing = true)
        static final class OnModernServer {
        }
    }

}
