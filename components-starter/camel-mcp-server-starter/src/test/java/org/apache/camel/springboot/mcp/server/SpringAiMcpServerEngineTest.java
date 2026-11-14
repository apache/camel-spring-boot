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
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiMcpServerEngineTest {

    @Test
    void reportsResourcesAndPromptsFromActualServerCapabilities() {
        McpSyncServer server = mock(McpSyncServer.class);
        SpringAiMcpServerEngine engine = new SpringAiMcpServerEngine(server);

        when(server.getServerCapabilities()).thenReturn(
                new McpSchema.ServerCapabilities(null, null, null, null, null, null));

        assertThat(engine.supportsResources()).isFalse();
        assertThat(engine.supportsResourceTemplates()).isFalse();
        assertThat(engine.supportsPrompts()).isFalse();

        when(server.getServerCapabilities()).thenReturn(
                new McpSchema.ServerCapabilities(
                        null,
                        null,
                        null,
                        new McpSchema.ServerCapabilities.PromptCapabilities(true),
                        new McpSchema.ServerCapabilities.ResourceCapabilities(false, true),
                        null));

        assertThat(engine.supportsResources()).isTrue();
        assertThat(engine.supportsResourceTemplates()).isTrue();
        assertThat(engine.supportsPrompts()).isTrue();
    }
}
