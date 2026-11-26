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
import java.util.Map;
import java.util.Optional;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.modern.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.ServerHttpHeaderValidator;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityException;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.customizer.McpServerCustomizer;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerJsonMapperAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerModernWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStatelessWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcModernServerTransport;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.function.ServerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The {@code McpServerCustomizer} beans of the Spring AI transport builders are applied to Camel's transports as
 * Spring AI applies them to its own: in order, after Camel's settings, so that a customizer can override them.
 */
class CamelMcpServerTransportCustomizerTest {

    private static final ServerTransportSecurityValidator APPLICATION_VALIDATOR = headers -> {
    };

    private static final McpTransportContextExtractor<ServerRequest> APPLICATION_EXTRACTOR
            = request -> McpTransportContext.create(Map.of("application", "my-app"));

    private final WebApplicationContextRunner streamable = new WebApplicationContextRunner()
            .withBean(CamelContext.class, () -> mock(CamelContext.class))
            .withBean(McpSyncServer.class, () -> mock(McpSyncServer.class))
            .withConfiguration(AutoConfigurations.of(
                    McpServerJsonMapperAutoConfiguration.class,
                    McpServerStreamableHttpWebMvcAutoConfiguration.class,
                    CamelMcpServerAutoConfiguration.class,
                    CamelMcpServerTransportAutoConfiguration.class))
            .withPropertyValues("spring.ai.mcp.server.protocol=STREAMABLE");

    private final WebApplicationContextRunner stateless = new WebApplicationContextRunner()
            .withBean(CamelContext.class, () -> mock(CamelContext.class))
            .withBean(McpStatelessSyncServer.class, () -> mock(McpStatelessSyncServer.class))
            .withConfiguration(AutoConfigurations.of(
                    McpServerJsonMapperAutoConfiguration.class,
                    McpServerStatelessWebMvcAutoConfiguration.class,
                    CamelMcpServerAutoConfiguration.class,
                    CamelMcpServerStatelessTransportAutoConfiguration.class))
            .withPropertyValues("spring.ai.mcp.server.protocol=STATELESS");

    private final WebApplicationContextRunner modern = new WebApplicationContextRunner()
            .withBean(McpServer.class, () -> mock(McpServer.class))
            .withConfiguration(AutoConfigurations.of(
                    McpServerJsonMapperAutoConfiguration.class,
                    CamelMcpServerModernTransportAutoConfiguration.class,
                    McpServerModernWebMvcAutoConfiguration.class))
            .withPropertyValues("spring.ai.mcp.server.protocol=MODERN");

    @Configuration(proxyBeanMethods = false)
    static class StreamableMaxSessionsCustomizer {

        @Bean
        McpServerCustomizer<WebMvcStreamableServerTransportProvider.Builder> maxSessionsCustomizer() {
            return builder -> builder.maxSessions(7);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class StreamableOrderedCustomizers {

        @Bean
        @Order(2)
        McpServerCustomizer<WebMvcStreamableServerTransportProvider.Builder> lastCustomizer() {
            return builder -> builder.maxSessions(2);
        }

        @Bean
        @Order(1)
        McpServerCustomizer<WebMvcStreamableServerTransportProvider.Builder> firstCustomizer() {
            return builder -> builder.maxSessions(1);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class StreamableSecurityCustomizer {

        @SuppressWarnings("deprecation")
        @Bean
        McpServerCustomizer<WebMvcStreamableServerTransportProvider.Builder> securityCustomizer() {
            return builder -> builder.securityValidator(APPLICATION_VALIDATOR).contextExtractor(APPLICATION_EXTRACTOR);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class StatelessEndpointCustomizer {

        @Bean
        McpServerCustomizer<WebMvcStatelessServerTransport.Builder> endpointCustomizer() {
            return builder -> builder.messageEndpoint("/custom-mcp");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class StatelessSecurityCustomizer {

        @SuppressWarnings("deprecation")
        @Bean
        McpServerCustomizer<WebMvcStatelessServerTransport.Builder> securityCustomizer() {
            return builder -> builder.securityValidator(APPLICATION_VALIDATOR).contextExtractor(APPLICATION_EXTRACTOR);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ModernValidatorCustomizer {

        static final ServerHttpHeaderValidator VALIDATOR = headers -> {
        };

        @Bean
        McpServerCustomizer<WebMvcModernServerTransport.Builder> validatorCustomizer() {
            return builder -> builder.httpHeaderValidator(VALIDATOR);
        }
    }

    @Test
    void streamableCustomizerIsAppliedToSpringAiTransportWithoutCamelTags() {
        streamable.withUserConfiguration(StreamableMaxSessionsCustomizer.class).run(context -> {
            assertThat(context).hasNotFailed();
            WebMvcStreamableServerTransportProvider provider
                    = context.getBean(WebMvcStreamableServerTransportProvider.class);
            assertThat(ReflectionTestUtils.getField(provider, "maxSessions")).isEqualTo(7L);
            assertThat(callContext(extractor(provider))).isNull();
        });
    }

    @Test
    void streamableCustomizerIsAppliedToCamelTransport() {
        streamable.withUserConfiguration(StreamableMaxSessionsCustomizer.class)
                .withPropertyValues("camel.mcp-server.tags=crm")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    WebMvcStreamableServerTransportProvider provider
                            = context.getBean(WebMvcStreamableServerTransportProvider.class);
                    assertThat(ReflectionTestUtils.getField(provider, "maxSessions")).isEqualTo(7L);
                    // the customizer does not replace them: Camel's caller identity and Origin validation remain
                    assertThat(callContext(extractor(provider))).isInstanceOf(McpToolCallContext.class);
                    assertRejectsRemoteOrigin(securityValidator(provider));
                });
    }

    @Test
    void streamableCustomizersAreAppliedInOrder() {
        streamable.withUserConfiguration(StreamableOrderedCustomizers.class)
                .withPropertyValues("camel.mcp-server.tags=crm")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(ReflectionTestUtils.getField(
                            context.getBean(WebMvcStreamableServerTransportProvider.class), "maxSessions"))
                            .isEqualTo(2L);
                });
    }

    @Test
    void streamableCustomizerOverridesCamelValidatorAndExtractor() {
        streamable.withUserConfiguration(StreamableSecurityCustomizer.class)
                .withPropertyValues("camel.mcp-server.tags=crm")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    WebMvcStreamableServerTransportProvider provider
                            = context.getBean(WebMvcStreamableServerTransportProvider.class);
                    assertThat(securityValidator(provider)).isSameAs(APPLICATION_VALIDATOR);
                    assertThat(extractor(provider)).isSameAs(APPLICATION_EXTRACTOR);
                });
    }

    @Test
    void statelessCustomizerIsAppliedToSpringAiTransportWithoutCamelTags() {
        stateless.withUserConfiguration(StatelessEndpointCustomizer.class).run(context -> {
            assertThat(context).hasNotFailed();
            WebMvcStatelessServerTransport transport = context.getBean(WebMvcStatelessServerTransport.class);
            assertThat(ReflectionTestUtils.getField(transport, "mcpEndpoint")).isEqualTo("/custom-mcp");
            assertThat(callContext(extractor(transport))).isNull();
        });
    }

    @Test
    void statelessCustomizerIsAppliedToCamelTransport() {
        stateless.withUserConfiguration(StatelessEndpointCustomizer.class)
                .withPropertyValues("camel.mcp-server.tags=crm")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    WebMvcStatelessServerTransport transport = context.getBean(WebMvcStatelessServerTransport.class);
                    assertThat(ReflectionTestUtils.getField(transport, "mcpEndpoint")).isEqualTo("/custom-mcp");
                    // the customizer does not replace them: Camel's caller identity and Origin validation remain
                    assertThat(callContext(extractor(transport))).isInstanceOf(McpToolCallContext.class);
                    assertRejectsRemoteOrigin(securityValidator(transport));
                });
    }

    @Test
    void statelessCustomizerOverridesCamelValidatorAndExtractor() {
        stateless.withUserConfiguration(StatelessSecurityCustomizer.class)
                .withPropertyValues("camel.mcp-server.tags=crm")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    WebMvcStatelessServerTransport transport = context.getBean(WebMvcStatelessServerTransport.class);
                    assertThat(securityValidator(transport)).isSameAs(APPLICATION_VALIDATOR);
                    assertThat(extractor(transport)).isSameAs(APPLICATION_EXTRACTOR);
                });
    }

    @Test
    void modernTransportUsesCamelValidatorWithoutCustomizer() {
        modern.withPropertyValues("camel.mcp-server.tags=crm").run(context -> {
            assertThat(context).hasNotFailed();
            WebMvcModernServerTransport transport = context.getBean(WebMvcModernServerTransport.class);
            assertThat(ReflectionTestUtils.getField(transport, "httpHeaderValidator"))
                    .isSameAs(context.getBean(ServerHttpHeaderValidator.class));
            assertThat(callContext(extractor(transport))).isInstanceOf(McpToolCallContext.class);
        });
    }

    @Test
    void modernCustomizerOverridesCamelValidator() {
        modern.withUserConfiguration(ModernValidatorCustomizer.class)
                .withPropertyValues("camel.mcp-server.tags=crm")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    WebMvcModernServerTransport transport = context.getBean(WebMvcModernServerTransport.class);
                    assertThat(ReflectionTestUtils.getField(transport, "httpHeaderValidator"))
                            .isSameAs(ModernValidatorCustomizer.VALIDATOR);
                    assertThat(callContext(extractor(transport))).isInstanceOf(McpToolCallContext.class);
                });
    }

    private static void assertRejectsRemoteOrigin(ServerTransportSecurityValidator validator) {
        assertThatThrownBy(() -> validator.validateHeaders(Map.of("Origin", List.of("https://evil.example"))))
                .isInstanceOf(ServerTransportSecurityException.class);
        assertThatCode(() -> validator.validateHeaders(Map.of("Origin", List.of("http://localhost:8080"))))
                .doesNotThrowAnyException();
    }

    private static ServerTransportSecurityValidator securityValidator(Object transport) {
        Object value = ReflectionTestUtils.getField(transport, "securityValidator");
        assertThat(value).isInstanceOf(ServerTransportSecurityValidator.class);
        return (ServerTransportSecurityValidator) value;
    }

    @SuppressWarnings("unchecked")
    private static McpTransportContextExtractor<ServerRequest> extractor(Object transport) {
        Object value = ReflectionTestUtils.getField(transport, "contextExtractor");
        assertThat(value).isInstanceOf(McpTransportContextExtractor.class);
        return (McpTransportContextExtractor<ServerRequest>) value;
    }

    private static Object callContext(McpTransportContextExtractor<ServerRequest> extractor) {
        ServerRequest request = mock(ServerRequest.class);
        when(request.principal()).thenReturn(Optional.of((Principal) () -> "customizer-test-user"));
        return extractor.extract(request).get(SpringAiMcpTransportContext.CALL_CONTEXT_KEY);
    }
}
