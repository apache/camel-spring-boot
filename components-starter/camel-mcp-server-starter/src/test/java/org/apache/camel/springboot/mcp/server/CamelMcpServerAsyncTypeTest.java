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
import java.util.List;
import java.util.Optional;

import io.modelcontextprotocol.modern.server.McpServer;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.HeaderAccessor;
import io.modelcontextprotocol.server.transport.ServerHttpHeaderValidator;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityException;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mcp.server.McpServerBridge;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerJsonMapperAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerModernWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStatelessWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcModernServerTransport;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.function.ServerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * With {@code spring.ai.mcp.server.type=ASYNC}, the Camel routes are not served on any protocol: the bridge backs off,
 * Spring AI's transport is kept unless {@code camel.mcp-server.transport-enabled=true}, and a warning is logged when
 * Camel tags are configured.
 */
@ExtendWith(OutputCaptureExtension.class)
class CamelMcpServerAsyncTypeTest {

    private static final String NOT_SERVED = "spring.ai.mcp.server.type is ASYNC";
    private static final String TRANSPORT_NOT_INSTALLED = "Camel does not install its Origin validation and"
                                                          + " caller-context extraction";

    private final WebApplicationContextRunner streamable = new WebApplicationContextRunner()
            .withBean(CamelContext.class, () -> mock(CamelContext.class))
            .withBean(McpAsyncServer.class, () -> mock(McpAsyncServer.class))
            .withConfiguration(AutoConfigurations.of(
                    McpServerJsonMapperAutoConfiguration.class,
                    McpServerStreamableHttpWebMvcAutoConfiguration.class,
                    CamelMcpServerAutoConfiguration.class,
                    CamelMcpServerTransportAutoConfiguration.class,
                    CamelMcpServerAsyncTypeAutoConfiguration.class))
            .withPropertyValues("spring.ai.mcp.server.protocol=STREAMABLE", "spring.ai.mcp.server.type=ASYNC");

    private final WebApplicationContextRunner stateless = new WebApplicationContextRunner()
            .withBean(CamelContext.class, () -> mock(CamelContext.class))
            .withBean(McpStatelessAsyncServer.class, () -> mock(McpStatelessAsyncServer.class))
            .withConfiguration(AutoConfigurations.of(
                    McpServerJsonMapperAutoConfiguration.class,
                    McpServerStatelessWebMvcAutoConfiguration.class,
                    CamelMcpServerAutoConfiguration.class,
                    CamelMcpServerStatelessTransportAutoConfiguration.class,
                    CamelMcpServerAsyncTypeAutoConfiguration.class))
            .withPropertyValues("spring.ai.mcp.server.protocol=STATELESS", "spring.ai.mcp.server.type=ASYNC");

    private final WebApplicationContextRunner modern = new WebApplicationContextRunner()
            .withBean(McpServer.class, () -> mock(McpServer.class))
            .withConfiguration(AutoConfigurations.of(
                    McpServerJsonMapperAutoConfiguration.class,
                    CamelMcpServerModernTransportAutoConfiguration.class,
                    McpServerModernWebMvcAutoConfiguration.class,
                    CamelMcpServerAsyncTypeAutoConfiguration.class))
            .withPropertyValues("spring.ai.mcp.server.protocol=MODERN", "spring.ai.mcp.server.type=ASYNC");

    @Test
    void streamableKeepsSpringAiTransportAndWarns(CapturedOutput output) {
        streamable.withPropertyValues("camel.mcp-server.tags=crm").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(McpServerEngine.class);
            assertThat(context).doesNotHaveBean(McpServerBridge.class);
            WebMvcStreamableServerTransportProvider provider
                    = context.getBean(WebMvcStreamableServerTransportProvider.class);
            assertThat(callContext(provider)).isNull();
            assertThat(ReflectionTestUtils.getField(provider, "securityValidator"))
                    .isSameAs(ServerTransportSecurityValidator.NOOP);
        });
        assertThat(output).contains("Camel MCP routes tagged [crm] are configured, but " + NOT_SERVED)
                .contains(TRANSPORT_NOT_INSTALLED);
    }

    @Test
    void statelessKeepsSpringAiTransportAndWarns(CapturedOutput output) {
        stateless.withPropertyValues("camel.mcp-server.tags=crm").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(McpServerBridge.class);
            WebMvcStatelessServerTransport transport = context.getBean(WebMvcStatelessServerTransport.class);
            assertThat(callContext(transport)).isNull();
            assertThat(ReflectionTestUtils.getField(transport, "securityValidator"))
                    .isSameAs(ServerTransportSecurityValidator.NOOP);
        });
        assertThat(output).contains(NOT_SERVED).contains(TRANSPORT_NOT_INSTALLED);
    }

    @Test
    void modernKeepsSpringAiValidatorAndExtractorAndWarns(CapturedOutput output) {
        modern.withPropertyValues("camel.mcp-server.tags=crm").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ServerHttpHeaderValidator.class);
            assertThat(context).doesNotHaveBean(McpTransportContextExtractor.class);
            WebMvcModernServerTransport transport = context.getBean(WebMvcModernServerTransport.class);
            assertThat(callContext(transport)).isNull();
            // Spring AI's default validator allows no Origin, where Camel's allows the loopback origins
            ServerHttpHeaderValidator validator
                    = (ServerHttpHeaderValidator) ReflectionTestUtils.getField(transport, "httpHeaderValidator");
            assertThatThrownBy(() -> validator.validate(origin("http://localhost:8080")))
                    .isInstanceOf(ServerTransportSecurityException.class);
        });
        assertThat(output).contains(NOT_SERVED).contains(TRANSPORT_NOT_INSTALLED);
    }

    @Test
    void syncServerOfTheApplicationOverAsyncServerDoesNotServeCamelRoutes(CapturedOutput output) {
        // a synchronous facade of the application over Spring AI's ASYNC server
        streamable.withBean(McpSyncServer.class, () -> mock(McpSyncServer.class))
                .withPropertyValues("camel.mcp-server.tags=crm")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(McpServerEngine.class);
                    assertThat(context).doesNotHaveBean(McpServerBridge.class);
                    assertThat(callContext(context.getBean(WebMvcStreamableServerTransportProvider.class))).isNull();
                });
        assertThat(output).contains(NOT_SERVED).contains(TRANSPORT_NOT_INSTALLED);
    }

    @Test
    void explicitlyEnabledTransportIsStillInstalled(CapturedOutput output) {
        streamable.withPropertyValues("camel.mcp-server.tags=crm", "camel.mcp-server.transport-enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(McpServerBridge.class);
                    assertThat(callContext(context.getBean(WebMvcStreamableServerTransportProvider.class)))
                            .isInstanceOf(McpToolCallContext.class);
                });
        assertThat(output).contains(NOT_SERVED).doesNotContain(TRANSPORT_NOT_INSTALLED);
    }

    @Test
    void explicitlyEnabledTransportIsNotInstalledWithoutHttpProtocol(CapturedOutput output) {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelMcpServerAsyncTypeAutoConfiguration.class))
                .withPropertyValues("spring.ai.mcp.server.type=ASYNC", "camel.mcp-server.tags=crm",
                        "camel.mcp-server.transport-enabled=true")
                .run(context -> assertThat(context).hasNotFailed());
        assertThat(output).contains(NOT_SERVED).contains(TRANSPORT_NOT_INSTALLED);
    }

    @Test
    void explicitlyEnabledTransportIsNotInstalledWithStdio(CapturedOutput output) {
        streamable.withPropertyValues("camel.mcp-server.tags=crm", "camel.mcp-server.transport-enabled=true",
                "spring.ai.mcp.server.stdio=on")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WebMvcStreamableServerTransportProvider.class);
                });
        assertThat(output).contains(NOT_SERVED).contains(TRANSPORT_NOT_INSTALLED);
    }

    @Test
    void noWarningWhenSpringAiServerIsDisabled(CapturedOutput output) {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelMcpServerAsyncTypeAutoConfiguration.class))
                .withPropertyValues("spring.ai.mcp.server.type=ASYNC", "spring.ai.mcp.server.enabled=false",
                        "camel.mcp-server.tags=crm")
                .run(context -> assertThat(context).hasNotFailed());
        assertThat(output).doesNotContain(NOT_SERVED);
    }

    @Test
    void noWarningWithoutCamelTags(CapturedOutput output) {
        streamable.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(callContext(context.getBean(WebMvcStreamableServerTransportProvider.class))).isNull();
        });
        assertThat(output).doesNotContain(NOT_SERVED);
    }

    @Test
    void noWarningForSyncServer(CapturedOutput output) {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelMcpServerAsyncTypeAutoConfiguration.class))
                .withPropertyValues("camel.mcp-server.tags=crm", "spring.ai.mcp.server.type=SYNC")
                .run(context -> assertThat(context).hasNotFailed());
        assertThat(output).doesNotContain(NOT_SERVED);
    }

    @Test
    void noWarningWhenCamelIntegrationIsDisabled(CapturedOutput output) {
        streamable.withPropertyValues("camel.mcp-server.tags=crm", "camel.mcp-server.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
        assertThat(output).doesNotContain(NOT_SERVED);
    }

    private static HeaderAccessor origin(String origin) {
        return new HeaderAccessor() {
            @Override
            public List<String> getHeader(String name) {
                return "Origin".equalsIgnoreCase(name) ? List.of(origin) : List.of();
            }

            @Override
            public List<String> getHeaderNames() {
                return List.of("Origin");
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Object callContext(Object transport) {
        McpTransportContextExtractor<ServerRequest> extractor
                = (McpTransportContextExtractor<ServerRequest>) ReflectionTestUtils.getField(transport,
                        "contextExtractor");
        ServerRequest request = mock(ServerRequest.class);
        when(request.principal()).thenReturn(Optional.of((Principal) () -> "async-test-user"));
        return extractor.extract(request).get(SpringAiMcpTransportContext.CALL_CONTEXT_KEY);
    }
}
