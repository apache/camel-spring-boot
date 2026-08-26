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
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

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

    @Autowired
    CamelRoutesEndpoint endpoint;

    @Autowired
    CamelContext camelContext;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void infoViewDoesNotExposeStartException() throws Exception {
        Object info = endpoint.doReadAction(FAILING_ROUTE_ID, CamelRoutesEndpoint.ReadAction.INFO);
        Assertions.assertNotNull(info);
        String json = mapper.writeValueAsString(info);
        Assertions.assertFalse(json.contains("route.start.exception"),
                "route.start.exception must not be serialized in the info view, but was: " + json);
    }

    @Test
    public void detailViewDoesNotExposeStartException() throws Exception {
        Object details = endpoint.doReadAction(FAILING_ROUTE_ID, CamelRoutesEndpoint.ReadAction.DETAIL);
        Assertions.assertNotNull(details);
        String json = mapper.writeValueAsString(details);
        Assertions.assertFalse(json.contains("route.start.exception"),
                "route.start.exception must not be serialized in the detail view, but was: " + json);
    }
}
