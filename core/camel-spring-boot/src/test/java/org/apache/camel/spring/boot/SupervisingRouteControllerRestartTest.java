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
package org.apache.camel.spring.boot;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.SupervisingRouteController;
import org.apache.camel.spring.boot.dummy.DummyComponent;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = { CamelAutoConfiguration.class,
        SupervisingRouteControllerRestartTest.TestConfiguration.class }, properties = {
                "camel.main.run-controller = true", "camel.routecontroller.enabled = true",
                "camel.routecontroller.initialDelay = 500", "camel.routecontroller.backoffDelay = 1000",
                "camel.routecontroller.backoffMaxAttempts = 5", })
public class SupervisingRouteControllerRestartTest {

    @Autowired
    private CamelContext context;

    @Test
    public void testRouteRestart() throws Exception {
        Assertions.assertNotNull(context.getRouteController());
        Assertions.assertTrue(context.getRouteController() instanceof SupervisingRouteController);

        SupervisingRouteController controller = context.getRouteController().adapt(SupervisingRouteController.class);

        // Wait for the controller to start the routes
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertEquals(ServiceStatus.Started, context.getRouteController().getRouteStatus("foo"));
            Assertions.assertEquals(ServiceStatus.Started, context.getRouteController().getRouteStatus("bar"));
            Assertions.assertEquals(ServiceStatus.Started, context.getRouteController().getRouteStatus("dummy"));
        });

        // restart the dummy route which should fail on first attempt
        controller.stopRoute("dummy");

        Assertions.assertNull(context.getRoute("dummy").getRouteController());

        try {
            controller.startRoute("dummy");
        } catch (Exception e) {
            Assertions.assertEquals("Forced error on restart", e.getCause().getMessage());
        }

        // Wait for wile to give time to the controller to start the route
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // now its suspended by the policy
            Assertions.assertEquals(ServiceStatus.Started, context.getRouteController().getRouteStatus("dummy"));
            Assertions.assertNotNull(context.getRoute("dummy").getRouteController());
        });
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    getContext().addComponent("dummy", new DummyComponent());

                    from("timer:foo?period=5000").id("foo").startupOrder(2).to("mock:foo");
                    from("timer:bar?period=5000").id("bar").startupOrder(1).to("mock:bar");
                    from("dummy:foo?failOnRestart=true").id("dummy").to("mock:dummy");
                }
            };
        }
    }
}
