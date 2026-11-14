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

import java.security.Principal;
import java.util.Optional;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mcp.server.McpServerBridge;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerJsonMapperAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.function.ServerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringBootMcpServerTransportAutoConfigurationTest {

    private static final String CALL_CONTEXT_KEY =
            SpringAiMcpTransportContext.class.getName() + ".toolCallContext";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withBean(CamelContext.class, () -> mock(CamelContext.class))
            .withBean(McpSyncServer.class, () -> mock(McpSyncServer.class))
            .withConfiguration(AutoConfigurations.of(
                    McpServerJsonMapperAutoConfiguration.class,
                    McpServerStreamableHttpWebMvcAutoConfiguration.class,
                    CamelMcpServerAutoConfiguration.class,
                    CamelMcpServerTransportAutoConfiguration.class))
            .withPropertyValues("spring.ai.mcp.server.protocol=STREAMABLE");

    @Test
    void camelTransportIsUsedByDefaultAndExtractsCallerContext() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WebMvcStreamableServerTransportProvider.class);

            WebMvcStreamableServerTransportProvider provider =
                    context.getBean(WebMvcStreamableServerTransportProvider.class);

            McpTransportContext transportContext = extractor(provider).extract(requestWithPrincipal());

            assertThat(transportContext.get(CALL_CONTEXT_KEY))
                    .isInstanceOf(McpToolCallContext.class);
            assertThat(context).hasSingleBean(McpServerEngine.class);
            assertThat(context).hasSingleBean(McpServerBridge.class);
        });
    }

    @Test
    void disablingCamelIntegrationLeavesSpringAiTransportInControl() {
        contextRunner.withPropertyValues("camel.mcp-server.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WebMvcStreamableServerTransportProvider.class);

            WebMvcStreamableServerTransportProvider provider =
                    context.getBean(WebMvcStreamableServerTransportProvider.class);

            McpTransportContext transportContext = extractor(provider).extract(requestWithPrincipal());

            // Spring AI's default extractor returns EMPTY; Camel's identity extractor was backed off.
            assertThat(transportContext.get(CALL_CONTEXT_KEY)).isNull();
            assertThat(context).doesNotHaveBean(McpServerEngine.class);
            assertThat(context).doesNotHaveBean(McpServerBridge.class);
        });
    }

    @Test
    void applicationProvidedProviderTakesPrecedence() {
        WebMvcStreamableServerTransportProvider custom =
                WebMvcStreamableServerTransportProvider.builder()
                        .mcpEndpoint("/custom")
                        .build();

        contextRunner.withBean(
                "applicationProvidedProvider",
                WebMvcStreamableServerTransportProvider.class,
                () -> custom)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(WebMvcStreamableServerTransportProvider.class);
                    assertThat(context.getBean(WebMvcStreamableServerTransportProvider.class))
                            .isSameAs(custom);
                });
    }

    @Test
    void wildcardOriginFailsApplicationStartup() {
        contextRunner.withPropertyValues("camel.mcp-server.allowed-origins=*").run(context -> {
            assertThat(context).hasFailed();
            assertThat(exceptionMessages(context.getStartupFailure()))
                    .contains("MCP allowed origin * is not supported");
        });
    }

    @SuppressWarnings("unchecked")
    private static McpTransportContextExtractor<ServerRequest> extractor(
            WebMvcStreamableServerTransportProvider provider) {

        Object value = ReflectionTestUtils.getField(provider, "contextExtractor");
        assertThat(value).isInstanceOf(McpTransportContextExtractor.class);
        return (McpTransportContextExtractor<ServerRequest>) value;
    }

    private static ServerRequest requestWithPrincipal() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.principal()).thenReturn(Optional.of((Principal) () -> "review-test-user"));
        return request;
    }

    private static String exceptionMessages(Throwable failure) {
        StringBuilder result = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            result.append(current.getMessage()).append('\n');
        }
        return result.toString();
    }
}
