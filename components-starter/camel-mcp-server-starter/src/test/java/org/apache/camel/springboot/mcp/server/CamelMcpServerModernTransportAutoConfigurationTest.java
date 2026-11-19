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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Camel's Origin validation and caller-identity extraction are provided to Spring AI's transport of MCP protocol
 * version 2026-07-28 only with {@code spring.ai.mcp.server.protocol=MODERN}, and those of the application take
 * precedence.
 */
class CamelMcpServerModernTransportAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CamelMcpServerModernTransportAutoConfiguration.class))
            .withPropertyValues("spring.ai.mcp.server.protocol=MODERN");

    @Test
    void camelValidatorAndExtractorAreProvidedForCamelRoutes() {
        contextRunner.withPropertyValues("camel.mcp-server.tags=crm").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ServerHttpHeaderValidator.class);
            assertThat(context).hasSingleBean(McpTransportContextExtractor.class);
        });
    }

    @Test
    void applicationValidatorTakesPrecedence() {
        ServerHttpHeaderValidator custom = headers -> {
        };
        contextRunner.withPropertyValues("camel.mcp-server.tags=crm")
                .withBean("applicationValidator", ServerHttpHeaderValidator.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServerHttpHeaderValidator.class);
                    assertThat(context.getBean(ServerHttpHeaderValidator.class)).isSameAs(custom);
                    assertThat(context).hasSingleBean(McpTransportContextExtractor.class);
                });
    }

    @Test
    void nothingIsProvidedWithoutCamelRoutes() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ServerHttpHeaderValidator.class);
            assertThat(context).doesNotHaveBean(McpTransportContextExtractor.class);
        });
    }

    @Test
    void nothingIsProvidedForAnotherProtocol() {
        contextRunner.withPropertyValues("camel.mcp-server.tags=crm", "spring.ai.mcp.server.protocol=STREAMABLE")
                .run(context -> assertThat(context).doesNotHaveBean(ServerHttpHeaderValidator.class));
    }
}
