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
package org.apache.camel.spring.boot.actuate.health;

import java.io.IOException;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootApplication
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        CamelHealthCheckAutoConfiguration.class, CamelAvailabilityCheckAutoConfiguration.class,
        ProbesRoute.class }, properties = { "camel.main.java-routes-include-pattern=**/ProbesRoute*",
                "management.endpoint.health.probes.enabled=true",
                "management.endpoint.health.group.readiness.include=readinessState,camelReadinessState",
                "management.endpoint.health.group.liveness.include=livenessState,camelLivenessState" })
public class CamelProbesTest {

    @Autowired
    RestTemplateBuilder restTemplateBuilder;

    @LocalManagementPort
    int managementPort;

    @Test
    public void testMetrics() {
        ResponseEntity<String> livenessResponse = restTemplateBuilder
                .rootUri("http://localhost:" + managementPort + "/actuator").build()
                .exchange("/health/liveness", HttpMethod.GET, new HttpEntity<>(null), String.class);

        ResponseEntity<String> readinessResponse = restTemplateBuilder.errorHandler(new NoOpErrorHandler())
                .rootUri("http://localhost:" + managementPort + "/actuator").build()
                .exchange("/health/readiness", HttpMethod.GET, new HttpEntity<>(null), String.class);

        ResponseEntity<String> healthResponse = restTemplateBuilder.errorHandler(new NoOpErrorHandler())
                .rootUri("http://localhost:" + managementPort + "/actuator").build()
                .exchange("/health", HttpMethod.GET, new HttpEntity<>(null), String.class);

        Assertions.assertThat(livenessResponse.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        Assertions.assertThat(livenessResponse.getBody()).isEqualTo("{\"status\":\"UP\"}");

        Assertions.assertThat(readinessResponse.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(503));
        Assertions.assertThat(readinessResponse.getBody()).isEqualTo("{\"status\":\"OUT_OF_SERVICE\"}");

        Assertions.assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(503));
    }
}

class NoOpErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return false;
    }
}
