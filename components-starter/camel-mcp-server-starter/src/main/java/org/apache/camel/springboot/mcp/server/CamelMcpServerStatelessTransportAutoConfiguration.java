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

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import org.springframework.ai.mcp.customizer.McpServerCustomizer;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.beans.factory.ObjectProvider;
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
 * Installs a principal-aware Spring AI WebMVC stateless MCP transport ({@code spring.ai.mcp.server.protocol=STATELESS}),
 * the counterpart of {@link CamelMcpServerTransportAutoConfiguration} for the Streamable HTTP transport.
 * <p>
 * This configuration runs before Spring AI's default WebMVC stateless transport configuration. It keeps the Spring AI
 * endpoint, and adds Camel's Origin validation and a transport context extractor for the authenticated caller
 * identity.
 * <p>
 * As Spring AI does for its own transport, every {@code McpServerCustomizer} of the
 * {@link WebMvcStatelessServerTransport.Builder} is applied, in order, after Camel's settings: a customizer can
 * override them, for example by setting its own security validator or context extractor, which then replaces Camel's.
 * <p>
 * An application-defined {@link WebMvcStatelessServerTransport} takes precedence.
 */
@AutoConfiguration(
        beforeName = "org.springframework.ai.mcp.server.webmvc.autoconfigure."
                + "McpServerStatelessWebMvcAutoConfiguration")
@ConditionalOnClass({
        WebMvcStatelessServerTransport.class,
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
        havingValue = "STATELESS",
        matchIfMissing = false)
@EnableConfigurationProperties({
        McpServerStreamableHttpProperties.class,
        McpServerConfigurationProperties.class
})
public class CamelMcpServerStatelessTransportAutoConfiguration {

    // Spring AI 2.1's builder still takes the deprecated ServerTransportSecurityValidator, see
    // CamelMcpServerTransportAutoConfiguration#createOriginValidator
    @SuppressWarnings("deprecation")
    @Bean
    @ConditionalOnMissingBean(WebMvcStatelessServerTransport.class)
    WebMvcStatelessServerTransport webMvcStatelessServerTransport(
            @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
            McpServerStreamableHttpProperties serverProperties,
            McpServerConfigurationProperties camelProperties,
            ObjectProvider<McpServerCustomizer<WebMvcStatelessServerTransport.Builder>> customizers) {

        WebMvcStatelessServerTransport.Builder builder = WebMvcStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .messageEndpoint(serverProperties.getMcpEndpoint())
                .securityValidator(
                        CamelMcpServerTransportAutoConfiguration.createOriginValidator(camelProperties.getAllowedOrigins()))
                .contextExtractor(SpringAiMcpTransportContext::extract);
        // the customizers of the application come last, as with Spring AI's own transport bean
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }
}
