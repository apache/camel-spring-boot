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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bridge-owned configuration of the Camel MCP server. Serving concerns (endpoint path, protocol, server identity)
 * are owned by the Spring AI MCP server and configured via {@code spring.ai.mcp.server.*}.
 * <p/>
 * Note that {@code spring.ai.mcp.server.*} provides no authentication: the MCP endpoint is served on the
 * application's own HTTP port and must be protected by the application, for example with a Spring Security filter
 * chain matching the endpoint path.
 */
@ConfigurationProperties(prefix = "camel.mcp-server")
public class McpServerConfigurationProperties {

    /**
     * Whether to expose ai-tool routes as MCP tools through the Spring AI MCP server. Enabled by default when the
     * starter is on the classpath.
     */
    private boolean enabled = true;

    /**
     * Comma-separated list of ai-tool tags to expose as MCP tools. Only tools registered under one of these tags are
     * exposed; the untagged default pool is never exposed. When not set, no tools are exposed.
     */
    private String tags;

    /**
     * Per-call tool execution timeout in milliseconds. A call exceeding the timeout returns an error result to the MCP
     * client; the underlying route keeps running until it completes on its own.
     */
    private long toolTimeout = 20000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public long getToolTimeout() {
        return toolTimeout;
    }

    public void setToolTimeout(long toolTimeout) {
        this.toolTimeout = toolTimeout;
    }
}
