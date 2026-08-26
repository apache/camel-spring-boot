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
package org.apache.camel.spring.boot.actuate.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code route.start.exception} route property is filtered out of the actuator route views. The base
 * {@link CamelRoutesEndpoint.RouteEndpointInfo} carries the {@code @JsonIgnoreProperties} filter; the detail view must
 * not bypass it.
 */
@EnableAutoConfiguration
@SpringBootTest(classes = { CamelAutoConfiguration.class, CamelRoutesEndpointAutoConfiguration.class,
        ActuatorTestControlledRoutes.class }, properties = {
                "management.endpoints.web.exposure.include=*",
                "camel.routecontroller.enabled=true",
                "camel.routecontroller.initial-delay=100",
                "camel.routecontroller.back-off-delay=100",
                "camel.routecontroller.back-off-max-attempts=3",
                "camel.main.routes-exclude-pattern=*",
                "camel.main.routes-collector-enabled=true" })
public class CamelRoutesEndpointStartExceptionTest {

    private static final String FAILING_ROUTE_ID = "controlled-bar";
    private static final String START_EXCEPTION = "route.start.exception";

    @Autowired
    CamelRoutesEndpoint endpoint;

    @Autowired
    CamelContext camelContext;

    private final ObjectMapper mapper = new ObjectMapper();

    private Route failingRoute() {
        return camelContext.getRouteController().getControlledRoutes().stream()
                .filter(route -> FAILING_ROUTE_ID.equals(route.getId()))
                .findAny()
                .orElse(null);
    }

    /**
     * The supervising route controller starts routes asynchronously, so the start failure is not recorded when the
     * context finishes refreshing. Without waiting for it the assertions below would pass vacuously - there would be
     * no property to leak in the first place.
     */
    @BeforeEach
    public void waitUntilTheStartFailureIsRecorded() {
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Route route = failingRoute();
            assertNotNull(route, "the supervised route should be known to the route controller");
            assertInstanceOf(Throwable.class, route.getProperties().get(START_EXCEPTION),
                    "the route start failure should be recorded before the endpoint views are checked");
        });
    }

    @Test
    public void infoViewDoesNotExposeStartException() throws Exception {
        Object info = endpoint.doReadAction(FAILING_ROUTE_ID, CamelRoutesEndpoint.ReadAction.INFO);
        assertNotNull(info);

        String json = mapper.writeValueAsString(info);
        assertFalse(json.contains(START_EXCEPTION),
                START_EXCEPTION + " must not be serialized in the info view, but was: " + json);
        assertTrue(json.contains("customId"),
                "the remaining route properties should still be serialized, but were not: " + json);
    }

    @Test
    public void detailViewDoesNotExposeStartException() throws Exception {
        Object details = endpoint.doReadAction(FAILING_ROUTE_ID, CamelRoutesEndpoint.ReadAction.DETAIL);
        assertNotNull(details);

        String json = mapper.writeValueAsString(details);
        assertFalse(json.contains(START_EXCEPTION),
                START_EXCEPTION + " must not be serialized in the detail view, but was: " + json);
        assertTrue(json.contains("customId"),
                "the remaining route properties should still be serialized, but were not: " + json);
    }
}
