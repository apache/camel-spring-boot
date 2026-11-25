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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Warns that the Camel routes are not served when the Spring AI MCP server is of the {@code ASYNC} type
 * ({@code spring.ai.mcp.server.type=ASYNC}), whatever the protocol: the Camel routes are served only on a {@code SYNC}
 * server, so {@link CamelMcpServerAutoConfiguration} backs off, and Camel's HTTP transport is installed only with
 * {@code camel.mcp-server.transport-enabled=true} (see {@link CamelMcpServerTransportCondition}).
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "camel.mcp-server",
        name = "enabled",
        matchIfMissing = true)
@ConditionalOnProperty(
        prefix = "spring.ai.mcp.server",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnProperty(
        prefix = "spring.ai.mcp.server",
        name = "type",
        havingValue = "ASYNC")
@EnableConfigurationProperties(McpServerConfigurationProperties.class)
public class CamelMcpServerAsyncTypeAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CamelMcpServerAsyncTypeAutoConfiguration.class);

    // a SmartInitializingSingleton is created even with spring.main.lazy-initialization=true
    @Bean
    SmartInitializingSingleton camelMcpServerAsyncTypeWarning(
            McpServerConfigurationProperties properties, Environment environment) {
        return () -> warnIfCamelRoutesAreSelected(properties, environment);
    }

    static void warnIfCamelRoutesAreSelected(McpServerConfigurationProperties properties, Environment environment) {
        if (!StringUtils.hasText(properties.getTags())) {
            return;
        }
        if (isCamelTransportInstalled(properties, environment)) {
            LOG.warn("Camel MCP routes tagged [{}] are configured, but spring.ai.mcp.server.type is ASYNC: the"
                     + " Camel routes are served only on a SYNC Spring AI MCP server, so they are not served. Set"
                     + " spring.ai.mcp.server.type=SYNC (the default) to serve them.",
                    properties.getTags());
        } else {
            LOG.warn("Camel MCP routes tagged [{}] are configured, but spring.ai.mcp.server.type is ASYNC: the"
                     + " Camel routes are served only on a SYNC Spring AI MCP server, so they are not served, and"
                     + " Camel does not install its Origin validation and caller-context extraction on the MCP"
                     + " transport. Set spring.ai.mcp.server.type=SYNC (the default) to serve them.",
                    properties.getTags());
        }
    }

    /**
     * Whether Camel's transport is installed although the server is {@code ASYNC}: only when it is explicitly enabled,
     * for an HTTP protocol it supports and without stdio, as the transport auto-configurations require (stdio is off
     * when the property is missing or {@code false}, as in Spring AI's {@code McpServerStdioDisabledCondition}).
     */
    private static boolean isCamelTransportInstalled(
            McpServerConfigurationProperties properties, Environment environment) {
        String protocol = environment.getProperty("spring.ai.mcp.server.protocol");
        return Boolean.TRUE.equals(properties.getTransportEnabled())
                && ("STREAMABLE".equalsIgnoreCase(protocol) || "STATELESS".equalsIgnoreCase(protocol)
                        || "MODERN".equalsIgnoreCase(protocol))
                && "false".equalsIgnoreCase(environment.getProperty("spring.ai.mcp.server.stdio", "false"));
    }
}
