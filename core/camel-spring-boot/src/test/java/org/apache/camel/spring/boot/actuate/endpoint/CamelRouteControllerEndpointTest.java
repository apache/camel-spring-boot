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

import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/*
 * Test for the {@link CamelRouteControllerEndpoint} actuator endpoint.
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = { CamelAutoConfiguration.class, CamelRouteControllerEndpointAutoConfiguration.class,
        CamelRoutesEndpointAutoConfiguration.class, ActuatorTestRoute.class }, properties = {
                "management.endpoints.web.exposure.include=*", "camel.routecontroller.enabled=true" })
public class CamelRouteControllerEndpointTest {

    @Autowired
    CamelRouteControllerEndpoint endpoint;

    @Autowired
    CamelContext camelContext;

    @Test
    public void testRouteControllerEndpoint() throws Exception {
        List<String> routesId = endpoint.getControlledRoutes();
        Assertions.assertTrue(routesId.size() > 0);
        Assertions.assertTrue(routesId.contains("foo-route"));
    }

}
