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

import org.apache.camel.component.mcp.server.McpServerConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bridge-owned configuration of the Camel MCP server. Serving concerns (endpoint path, protocol, server identity)
 * are owned by the Spring AI MCP server and configured via {@code spring.ai.mcp.server.*}.
 * The Streamable HTTP transport also uses {@code allowed-origins} to validate browser request origins.
 * <p/>
 * Note that {@code spring.ai.mcp.server.*} provides no authentication: the MCP endpoint is served on the
 * application's own HTTP port and must be protected by the application, for example with a Spring Security filter
 * chain matching the endpoint path.
 */
@ConfigurationProperties(prefix = "camel.mcp-server")
public class McpServerConfigurationProperties {

    /**
     * Whether to enable Camel MCP integration for ai-tool, ai-resource, and ai-prompt routes through the Spring AI
     * MCP server. Enabled by default when the starter is on the classpath.
     */
    private boolean enabled = true;

    /**
     * Whether to replace Spring AI's default Streamable HTTP transport with Camel's transport.
     * When unset, replacement is enabled only when {@code tags} is configured. Set this to true
     * to enable Camel Origin validation and caller-context extraction without Camel routes, or
     * false to keep Spring AI's provider while leaving the Camel bridge independently enabled.
     */
    private Boolean transportEnabled;

    /**
     * Comma-separated tag patterns used to select ai-tool, ai-resource, and ai-prompt routes for MCP exposure.
     * Only matching routes are published; the untagged default pools are never exposed. When not set,
     * no Camel tools, resources, resource templates, or prompts are published.
     */
    private String tags;

    /**
     * Per-call tool execution timeout in milliseconds. A call exceeding the timeout returns an error result to the MCP
     * client; the underlying route keeps running until it completes on its own.
          * The default is 20000 milliseconds.
*/
    private long toolTimeout = McpServerConstants.DEFAULT_TOOL_TIMEOUT;

    /**
     * Per-read resource execution timeout in milliseconds. A read exceeding the timeout returns an error
     * to the MCP client; the underlying route keeps running until it completes on its own.
     */
    private long resourceTimeout = McpServerConstants.DEFAULT_RESOURCE_TIMEOUT;

    /**
     * Per-request prompt execution timeout in milliseconds. A request exceeding the timeout returns an error
     * to the MCP client; the underlying route may continue running until it completes.
     */
    private long promptTimeout = McpServerConstants.DEFAULT_PROMPT_TIMEOUT;

    /**
     * Comma-separated list of origins allowed to call the Streamable HTTP MCP endpoint.
     * Entries may be exact origins or use a wildcard port, for example http://localhost:*.
     * When unset, only loopback origins are allowed. Requests without an Origin header are accepted.
     * This setting is applied when Camel's Streamable HTTP transport provider is selected.
     */
    private String allowedOrigins;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getTransportEnabled() {
        return transportEnabled;
    }

    public void setTransportEnabled(Boolean transportEnabled) {
        this.transportEnabled = transportEnabled;
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

    public long getResourceTimeout() {
        return resourceTimeout;
    }

    public void setResourceTimeout(long resourceTimeout) {
        this.resourceTimeout = resourceTimeout;
    }

    public long getPromptTimeout() {
        return promptTimeout;
    }

    public void setPromptTimeout(long promptTimeout) {
        this.promptTimeout = promptTimeout;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
