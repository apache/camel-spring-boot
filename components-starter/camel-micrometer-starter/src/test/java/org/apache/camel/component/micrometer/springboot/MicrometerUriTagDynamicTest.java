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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * With dynamic uri tags, the requested path is used as uri tag, but only for requests that are resolved to a Camel
 * consumer, and the tag value is kept bounded in length.
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                classes = { CamelAutoConfiguration.class, MicrometerUriTagTestSupport.TestConfiguration.class },
                // the legacy camelCase spelling of the properties must keep working as well
                properties = { "camel.metrics.uriTagEnabled=true", "camel.metrics.uriTagDynamic=true" })
public class MicrometerUriTagDynamicTest extends MicrometerUriTagTestSupport {

    private static final int REQUESTS = 10;
    private static final int MAX_URI_LENGTH = 200;

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
    void matchedRequestsUseTheRequestedPath() throws Exception {
        assertEquals(200, get("/camel/users/123"));

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertEquals(1, count("/camel/users/123"), "Got uri tags " + uriTags()));
    }

    @Order(3)
    @Test
    void longRequestedPathIsCapped() throws Exception {
        assertEquals(200, get("/camel/users/" + "a".repeat(300)));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Map<String, Long> tags = uriTags();
            assertTrue(tags.keySet().stream().anyMatch(uri -> uri.length() == MAX_URI_LENGTH
                    && uri.startsWith("/camel/users/aaa")), "Expected a capped uri tag value but got " + tags);
            assertFalse(tags.keySet().stream().anyMatch(uri -> uri.length() > MAX_URI_LENGTH),
                    "No uri tag value must be longer than " + MAX_URI_LENGTH + " but got " + tags);
        });
    }
}
