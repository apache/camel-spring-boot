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

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

/*
 * Test for the {@link CamelRoutesEndpoint} actuator endpoint.
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootApplication
@SpringBootTest(
    classes = {CamelAutoConfiguration.class, CamelRoutesEndpointAutoConfiguration.class,
               CamelRouteControllerEndpointAutoConfiguration.class, ActuatorTestRoute.class},
    properties = {"management.endpoints.web.exposure.include=*",
                  "management.endpoint.camelroute.enabled=true",
                  "management.endpoint.camelroutecontroller.enabled=false"}
)
public class CamelRouteControllerEndpointGloballyDisabledAndReenabledTest {

    @Autowired(required = false)
    CamelRouteControllerEndpoint routeControllerEndpoint;

    @Autowired(required = false)
    CamelRoutesEndpoint routesEndpoint;

    @Autowired
    CamelContext camelContext;

    @Test
    public void testRoutesEndpointPresent() throws Exception {
        // routes is enabled
        Assertions.assertNotNull(routesEndpoint);
        // controller is disabled
        Assertions.assertNull(routeControllerEndpoint);
    }

}
