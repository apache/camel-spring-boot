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

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Spring Boot application with camel.cli.transport=websocket connects to the tool, says hello with the name of its
 * WebSocket client, and executes an action.
 */
abstract class CliConnectorWebSocketTestSupport {

    static ToolServer tool;

    @Autowired
    CamelContext context;

    @DynamicPropertySource
    static void tool(DynamicPropertyRegistry registry) throws Exception {
        tool = new ToolServer().start();
        tool.requiredToken = "t0k3n-4-t3st";
        // read by Camel from the Spring environment
        registry.add("camel.cli.websocket.url", tool::url);
        registry.add("camel.cli.websocket.token", () -> tool.requiredToken);
    }

    @AfterAll
    static void stopTool() throws Exception {
        tool.close();
    }

    abstract String expectedClient();

    @Test
    void connectsAndExecutesActions() throws Exception {
        JsonObject hello = tool.awaitFrame(f -> "hello".equals(f.getString("type")));
        assertThat(hello.getString("transport")).isEqualTo(expectedClient());
        assertThat(hello.getString("name")).isEqualTo(context.getName());

        JsonObject action = new JsonObject(Map.of("action", "route", "command", "stop", "id", "hello"));
        tool.send(new JsonObject(Map.of("v", 1, "type", "action", "requestId", "r1", "action", action)));

        assertThat(tool.awaitResult("r1").getBoolean("ok")).isTrue();
        assertThat(context.getRouteController().getRouteStatus("hello")).isEqualTo(ServiceStatus.Stopped);
        JsonObject status = tool.awaitFrame(f -> "snapshot".equals(f.getString("type"))
                && "status".equals(f.getString("kind"))).getMap("data");
        assertThat(status.toJson()).contains("\"routeId\":\"hello\"");
    }

    @Configuration
    static class Routes {

        @Bean
        RouteBuilder routes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:hello").routeId("hello").setBody(simple("Hello ${body}"));
                }
            };
        }
    }
}
