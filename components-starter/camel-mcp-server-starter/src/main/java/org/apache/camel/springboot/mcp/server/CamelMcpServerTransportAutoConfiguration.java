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

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HeaderAccessor;
import io.modelcontextprotocol.server.transport.ServerHttpHeaderValidator;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;
import org.apache.camel.component.mcp.server.McpServerOrigins;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Installs a principal-aware Spring AI WebMVC MCP transport.
 * <p>
 * This configuration runs before Spring AI's default WebMVC transport configuration.
 * It preserves the Spring AI endpoint and keep-alive settings while adding a transport
 * context extractor for authenticated caller identity.
 * <p>
 * An application-defined {@link WebMvcStreamableServerTransportProvider} takes precedence.
 */
@AutoConfiguration(
        beforeName = "org.springframework.ai.mcp.server.webmvc.autoconfigure."
                + "McpServerStreamableHttpWebMvcAutoConfiguration")
@ConditionalOnClass({
        WebMvcStreamableServerTransportProvider.class,
        JacksonMcpJsonMapper.class
})
@Conditional({
        McpServerStdioDisabledCondition.class,
        CamelMcpServerTransportCondition.class
})
@ConditionalOnProperty(
        prefix = "camel.mcp-server",
        name = "enabled",
        matchIfMissing = true)
@ConditionalOnProperty(
        prefix = "spring.ai.mcp.server",
        name = "protocol",
        havingValue = "STREAMABLE",
        matchIfMissing = false)
@EnableConfigurationProperties({
        McpServerStreamableHttpProperties.class,
        McpServerConfigurationProperties.class
})
public class CamelMcpServerTransportAutoConfiguration {

    /**
     * Default loopback origins accepted by the Camel Streamable HTTP transport.
     * Applications that provide their own Spring AI transport can reuse this immutable list.
     */
    public static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:*", "https://localhost:*",
            "http://127.0.0.1:*", "https://127.0.0.1:*",
            "http://[::1]:*", "https://[::1]:*");

    @SuppressWarnings("deprecation")
    @Bean
    @ConditionalOnMissingBean(WebMvcStreamableServerTransportProvider.class)
    WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
            @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
            McpServerStreamableHttpProperties serverProperties,
            McpServerConfigurationProperties camelProperties) {

        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .mcpEndpoint(serverProperties.getMcpEndpoint())
                .keepAliveInterval(serverProperties.getKeepAliveInterval())
                .disallowDelete(serverProperties.isDisallowDelete())
                .securityValidator(createOriginValidator(camelProperties.getAllowedOrigins()))
                .contextExtractor(SpringAiMcpTransportContext::extract)
                .build();
    }

    /**
     * Creates the non-deprecated MCP SDK HTTP header validator using Camel's
     * shared Origin parser and normalizer.
     *
     * @param configuredAllowedOrigins comma-separated allowed origins, or null for loopback defaults
     * @return the MCP SDK HTTP header validator
     */
    public static ServerHttpHeaderValidator createOriginHeaderValidator(String configuredAllowedOrigins) {
        List<String> allowedOrigins = McpServerOrigins.parseAllowedOrigins(configuredAllowedOrigins);
        if (allowedOrigins == null) {
            allowedOrigins = DEFAULT_ALLOWED_ORIGINS;
        }

        DefaultServerTransportSecurityValidator delegate = DefaultServerTransportSecurityValidator.builder()
                .allowedOrigins(allowedOrigins)
                .build();

        return headers -> delegate.validate(new HeaderAccessor() {
            @Override
            public List<String> getHeader(String name) {
                List<String> values = headers.getHeader(name);
                if (name != null && "Origin".equalsIgnoreCase(name) && values != null) {
                    return values.stream().map(McpServerOrigins::normalizeOrigin).toList();
                }
                return values;
            }

            @Override
            public List<String> getHeaderNames() {
                return headers.getHeaderNames();
            }
        });
    }

    /**
     * Compatibility adapter for Spring AI 2.0.2. Its WebMVC provider builder
     * still accepts ServerTransportSecurityValidator rather than the newer
     * ServerHttpHeaderValidator. The validation itself is performed by the
     * non-deprecated SDK validator above.
     *
     * Remove this adapter when Spring AI exposes httpHeaderValidator(...) on
     * WebMvcStreamableServerTransportProvider.Builder.
     */
    @SuppressWarnings("deprecation")
    public static ServerTransportSecurityValidator createOriginValidator(String configuredAllowedOrigins) {
        ServerHttpHeaderValidator validator = createOriginHeaderValidator(configuredAllowedOrigins);
        return headers -> validator.validate(new MapHeaderAccessor(headers));
    }

    /**
     * Adapts Spring AI 2.0.2's legacy map-based callback to the MCP SDK
     * HeaderAccessor API.
     */
    private static final class MapHeaderAccessor implements HeaderAccessor {

        private final Map<String, List<String>> headers;

        private MapHeaderAccessor(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        @Override
        public List<String> getHeader(String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(name))
                    .map(Map.Entry::getValue)
                    .filter(values -> values != null)
                    .findFirst()
                    .orElse(List.of());
        }

        @Override
        public List<String> getHeaderNames() {
            return headers.keySet().stream()
                    .filter(name -> name != null)
                    .toList();
        }
    }
}
