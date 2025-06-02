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
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.actuate.endpoint.CamelRoutesEndpoint.RouteEndpointInfo;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.stream.Collectors;

/*
 * Test for the {@link CamelRoutesEndpoint} actuator endpoint.
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
        "camel.main.routes-collector-enabled=true"}
)
public class CamelRoutesEndpointControllerRouteTest {

    @Autowired
    CamelRoutesEndpoint endpoint;

    @Autowired
    CamelContext camelContext;


   @Test
    public void testFailedRouteVisible() throws Exception {
        List<RouteEndpointInfo> routes = endpoint.readRoutes();
        Assertions.assertTrue(contains(routes,"controlled-bar"));
        List<RouteEndpointInfo> filtered = filterById( routes,"controlled-bar");
        Assertions.assertEquals(1, filtered.size());
       Assertions.assertEquals(ServiceStatus.Stopped.name(),filtered.get(0).getStatus());


    }


    private boolean contains(List<RouteEndpointInfo> routes, String routeId){
        List<RouteEndpointInfo> list = filterById( routes,  routeId);
        return list.size() > 0;
    }

    private  List<RouteEndpointInfo> filterById(List<RouteEndpointInfo> routes, String routeId){
        return routes.stream().filter(x->(routeId.equals(x.getId()))).collect(Collectors.toUnmodifiableList());
    }


}
