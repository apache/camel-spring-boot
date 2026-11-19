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

import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.ServerHttpHeaderValidator;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcModernServerTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * Adds Camel's Origin validation and the authenticated caller identity to the Spring AI WebMVC transport of MCP
 * protocol version 2026-07-28 ({@code spring.ai.mcp.server.protocol=MODERN}), the counterpart of
 * {@link CamelMcpServerTransportAutoConfiguration} for the Streamable HTTP transport.
 * <p>
 * Spring AI builds that transport with the {@link ServerHttpHeaderValidator} and the
 * {@link McpTransportContextExtractor} beans of the application context, so this configuration provides them and
 * keeps Spring AI's endpoint and settings. A validator or an extractor of the application takes precedence, and so
 * does a {@link WebMvcModernServerTransport} of the application.
 */
@AutoConfiguration(
        beforeName = "org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerModernWebMvcAutoConfiguration")
@ConditionalOnClass(WebMvcModernServerTransport.class)
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
        havingValue = "MODERN",
        matchIfMissing = false)
@EnableConfigurationProperties(McpServerConfigurationProperties.class)
public class CamelMcpServerModernTransportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ServerHttpHeaderValidator camelMcpServerHttpHeaderValidator(McpServerConfigurationProperties camelProperties) {
        return CamelMcpServerTransportAutoConfiguration.createOriginHeaderValidator(camelProperties.getAllowedOrigins());
    }

    @Bean
    @ConditionalOnMissingBean
    McpTransportContextExtractor<ServerRequest> camelMcpServerContextExtractor() {
        return SpringAiMcpTransportContext::extract;
    }
}
