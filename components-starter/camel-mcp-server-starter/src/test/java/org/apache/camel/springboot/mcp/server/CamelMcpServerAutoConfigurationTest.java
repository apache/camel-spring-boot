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

import io.modelcontextprotocol.server.McpSyncServer;
import org.apache.camel.component.mcp.server.McpServerBridge;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CamelMcpServerAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CamelAutoConfiguration.class, CamelMcpServerAutoConfiguration.class))
            .withPropertyValues("spring.main.banner-mode=off");

    @Configuration
    static class McpSyncServerConfiguration {
        @Bean
        McpSyncServer mcpSyncServer() {
            return Mockito.mock(McpSyncServer.class);
        }
    }

    @Test
    void testBridgeAndEngineConfiguredByDefault() {
        runner.withUserConfiguration(McpSyncServerConfiguration.class)
                .withPropertyValues("camel.mcp-server.tags=crm")
                .run(context -> {
                    assertThat(context).hasSingleBean(McpServerEngine.class);
                    assertThat(context).hasSingleBean(McpServerBridge.class);
                    assertThat(context.getBean(McpServerBridge.class).getConfiguration().getTags()).isEqualTo("crm");
                });
    }

    @Test
    void testDisabledProperty() {
        runner.withUserConfiguration(McpSyncServerConfiguration.class)
                .withPropertyValues("camel.mcp-server.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(McpServerEngine.class);
                    assertThat(context).doesNotHaveBean(McpServerBridge.class);
                });
    }

    @Test
    void testBacksOffWithoutMcpSyncServer() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(McpServerEngine.class);
            assertThat(context).doesNotHaveBean(McpServerBridge.class);
        });
    }
}
