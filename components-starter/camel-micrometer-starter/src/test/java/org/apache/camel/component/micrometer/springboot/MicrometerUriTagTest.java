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
package org.apache.camel.component.micrometer.springboot;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The uri tag must be the static path of the Camel consumer, and requests that are not for a Camel consumer must not
 * add a meter per requested path.
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                classes = { CamelAutoConfiguration.class, MicrometerUriTagTestSupport.TestConfiguration.class },
                properties = { "camel.metrics.uri-tag-enabled=true" })
public class MicrometerUriTagTest extends MicrometerUriTagTestSupport {

    private static final int REQUESTS = 10;

    @Order(1)
    @Test
    void unmatchedRequestsShareASingleMeter() throws Exception {
        for (int i = 0; i < REQUESTS; i++) {
            assertEquals(404, get("/camel/no-such-path-" + i));
        }

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Map<String, Long> tags = uriTags();
            assertEquals(1, tags.size(), "Expected a single uri tag value but got " + tags);
            assertEquals(REQUESTS, tags.values().iterator().next());
            assertFalse(tags.keySet().stream().anyMatch(uri -> uri.contains("no-such-path")),
                    "The requested path must not be used as uri tag but got " + tags);
        });
    }

    @Order(2)
    @Test
    void matchedRequestsUseTheConsumerPath() throws Exception {
        assertEquals(200, get("/camel/users/123"));
        assertEquals(200, get("/camel/users/456"));

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertEquals(2, count("/users/{id}"), "Got uri tags " + uriTags()));
    }
}
