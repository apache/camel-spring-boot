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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
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
@Conditional(McpServerStdioDisabledCondition.class)
@ConditionalOnProperty(
        prefix = "camel.mcp-server",
        name = "enabled",
        havingValue = "true",
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

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:*", "https://localhost:*",
            "http://127.0.0.1:*", "https://127.0.0.1:*",
            "http://[::1]:*", "https://[::1]:*");

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
     * Creates the MCP SDK validator using Camel's shared origin parser and normalizer.
     * Requests without an Origin header are accepted by the SDK validator.
     */
    // Spring AI 2.0.2 exposes only this deprecated validator type on the
    // WebMVC transport builder.
    @SuppressWarnings("deprecation")
    private static ServerTransportSecurityValidator createOriginValidator(String configuredAllowedOrigins) {
        List<String> allowedOrigins = McpServerOrigins.parseAllowedOrigins(configuredAllowedOrigins);
        if (allowedOrigins == null) {
            allowedOrigins = DEFAULT_ALLOWED_ORIGINS;
        }

        DefaultServerTransportSecurityValidator delegate = DefaultServerTransportSecurityValidator.builder()
                .allowedOrigins(allowedOrigins)
                .build();

        return headers -> {
            Map<String, List<String>> normalizedHeaders = new LinkedHashMap<>(headers);
            headers.forEach((name, values) -> {
                if (name != null && "Origin".equalsIgnoreCase(name) && values != null) {
                    normalizedHeaders.put(name,
                            values.stream().map(McpServerOrigins::normalizeOrigin).toList());
                }
            });
            delegate.validateHeaders(normalizedHeaders);
        };
    }
}
