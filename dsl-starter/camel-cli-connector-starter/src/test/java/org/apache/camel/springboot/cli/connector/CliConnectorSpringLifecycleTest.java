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
package org.apache.camel.springboot.cli.connector;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.util.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * How the connector stops a Spring Boot application, and refuses to run where it should not.
 */
class CliConnectorSpringLifecycleTest {

    private final ToolServer tool = new ToolServer();
    private ConfigurableApplicationContext context;

    @AfterEach
    void stopAll() throws Exception {
        if (context != null) {
            context.close();
        }
        tool.close();
    }

    @Test
    void stopActionClosesTheApplicationContext() throws Exception {
        tool.start();
        context = new SpringApplicationBuilder(
                CamelAutoConfiguration.class, CliConnectorAutoConfiguration.class,
                CliConnectorWebSocketTestSupport.Routes.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "camel.cli.transport=websocket",
                        "camel.cli.websocket.url=" + tool.url())
                .run();
        tool.awaitFrame(f -> "hello".equals(f.getString("type")));

        JsonObject action = new JsonObject(Map.of("action", "stop"));
        tool.send(new JsonObject(Map.of("v", 1, "type", "action", "requestId", "r1", "action", action)));

        assertThat(tool.awaitResult("r1").getBoolean("ok")).isTrue();
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> assertThat(context.isActive()).isFalse());
    }

    @Test
    void refusesTheWebSocketTransportWithTheSpringProdProfile() throws Exception {
        tool.start();
        SpringApplicationBuilder app = new SpringApplicationBuilder(
                CamelAutoConfiguration.class, CliConnectorAutoConfiguration.class,
                CliConnectorWebSocketTestSupport.Routes.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .properties(
                        "camel.cli.transport=websocket",
                        "camel.cli.websocket.url=" + tool.url());

        assertThatThrownBy(app::run).hasRootCauseMessage(
                "The Camel CLI connector websocket transport gives the connected tool full control of this application"
                                                         + " and cannot be used with the Spring prod profile."
                                                         + " Remove camel.cli.transport=websocket, or use another profile.");
        assertThat(tool.sessions).isEmpty();
    }
}
