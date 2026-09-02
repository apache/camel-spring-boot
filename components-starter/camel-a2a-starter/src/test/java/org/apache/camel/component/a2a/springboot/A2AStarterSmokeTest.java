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
package org.apache.camel.component.a2a.springboot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                classes = { A2AStarterSmokeTest.TestRoutes.class })
class A2AStarterSmokeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SEND_BODY
            = "{\"message\":{\"messageId\":\"msg-1\",\"role\":\"user\",\"parts\":[{\"text\":\"Hello\"}]}}";

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Configuration
    static class TestRoutes {
        @Bean
        RouteBuilder a2aRoutes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("a2a:classpath:cards/streaming-agent-card.json?validateAuth=false&sseHeartbeatInterval=200")
                            .delay(1500)
                            .setBody(constant("Echo from Spring Boot"));
                    from("a2a:rpc-agent?name=JsonRpcAgent&version=1.0.0&protocolBinding=jsonrpc&basePath=/rpc&validateAuth=false")
                            .setBody(constant("JSON-RPC echo"));
                }
            };
        }
    }

    @Test
    void agentCard() throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/.well-known/agent-card.json")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        JsonNode card = MAPPER.readTree(response.body());
        assertEquals("SpringBootAgent", card.get("name").asText());
    }

    @Test
    void sendMessageViaRestCustomMethodPath() throws Exception {
        HttpResponse<String> response = post("/message:send", SEND_BODY, "application/a2a+json");
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = MAPPER.readTree(response.body());
        assertTrue(body.has("task"), response.body());
        assertFalse(body.get("task").get("id").asText().isEmpty());
    }

    @Test
    void sendMessageViaJsonRpc() throws Exception {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"SendMessage\",\"params\":{\"message\":{\"messageId\":\"msg-jrpc\","
                         + "\"role\":\"user\",\"parts\":[{\"text\":\"Hello JSON-RPC\"}]}},\"id\":\"req-1\"}";
        HttpResponse<String> response = post("/rpc/", request, "application/json");
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = MAPPER.readTree(response.body());
        assertEquals("2.0", body.get("jsonrpc").asText());
        assertEquals("req-1", body.get("id").asText());
        assertNotNull(body.get("result").get("task").get("id"));
    }

    @Test
    void streamMessageDeliversSseIncrementally() throws Exception {
        long start = System.nanoTime();
        HttpResponse<InputStream> response = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/message:stream"))
                        .header("Content-Type", "application/a2a+json")
                        .POST(HttpRequest.BodyPublishers.ofString(SEND_BODY))
                        .build(),
                HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/event-stream"),
                response.headers().toString());

        StringBuilder all = new StringBuilder();
        long firstLineMillis = -1;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (firstLineMillis < 0 && !line.isBlank()) {
                    firstLineMillis = (System.nanoTime() - start) / 1_000_000;
                }
                all.append(line).append('\n');
            }
        }
        String events = all.toString();
        assertTrue(firstLineMillis >= 0 && firstLineMillis < 1500,
                "first SSE bytes should arrive before the 1500ms route delay completes, took " + firstLineMillis + "ms");
        assertTrue(events.contains("TASK_STATE_COMPLETED"), events);
        assertTrue(events.contains("Echo from Spring Boot"), events);
    }

    private HttpResponse<String> post(String path, String body, String contentType) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + path))
                        .header("Content-Type", contentType)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
