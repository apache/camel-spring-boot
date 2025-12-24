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
package org.apache.camel.component.platform.http.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AutoConfigureRestTestClient
abstract class PlatformHttpBase {

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    CamelContext camelContext;

    @Test
    public void testGet() throws Exception {
        waitUntilRouteIsStarted(1, getGetRouteId());

        EntityExchangeResult<String> result = restTestClient.get().uri("/myget")
                .exchange()
                .expectBody(String.class)
                .returnResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(HttpStatusCode.valueOf(200));
    }

    @Test
    public void testPost() throws Exception {
        waitUntilRouteIsStarted(1, getPostRouteId());

        EntityExchangeResult<String> result = restTestClient.post().uri("/mypost")
                .body("test")
                .exchange()
                .expectBody(String.class)
                .returnResult();
        Assertions.assertThat(result.getResponseBody()).isEqualTo("TEST");
    }

    protected void waitUntilRouteIsStarted(int atMostSeconds, String routeId) {
        Awaitility.await().atMost(atMostSeconds, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(ServiceStatus.Started, camelContext.getRouteController().getRouteStatus(routeId))
        );
    }

    protected abstract String getPostRouteId();

    protected abstract String getGetRouteId();
}
