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
package org.apache.camel.component.jolokia.springboot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { CamelAutoConfiguration.class, JolokiaComponentAutoConfiguration.class },
                  properties = "camel.component.jolokia.serverConfig.port=0")
class JolokiaComponentOriginTest extends JolokiaComponentTestBase {

    @Test
    void allowsRequestFromTheJolokiaServersOwnOrigin() throws Exception {
        String serverOrigin = "http://127.0.0.1:" + agent.getAddress().getPort();
        URI endpoint = URI.create(serverOrigin + "/jolokia");
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> commandLineResponse = client.send(
                HttpRequest.newBuilder(endpoint)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"type\":\"version\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(commandLineResponse.statusCode()).isEqualTo(200);
        assertThat(commandLineResponse.body()).contains("\"status\":200");

        HttpResponse<String> browserResponse = client.send(
                HttpRequest.newBuilder(endpoint)
                        .header("Content-Type", "application/json")
                        .header("Origin", serverOrigin)
                        .POST(HttpRequest.BodyPublishers.ofString("{\"type\":\"version\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(browserResponse.body())
                .as("a same-origin browser request should not be rejected")
                .contains("\"status\":200");
    }
}
