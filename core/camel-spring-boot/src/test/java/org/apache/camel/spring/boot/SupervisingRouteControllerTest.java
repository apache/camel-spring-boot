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

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.SupervisingRouteController;
import org.apache.camel.test.AvailablePortFinder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.awaitility.Awaitility.await;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SupervisingRouteControllerTest.TestConfiguration.class
    },
    properties = {
        "camel.springboot.xml-routes = false",
        "camel.springboot.xml-rests = false",
        "camel.springboot.main-run-controller = true",
        "camel.springboot.routeControllerSuperviseEnabled = true",
        "camel.springboot.routeControllerInitialDelay = 500",
        "camel.springboot.routeControllerBackoffDelay = 1000",
        "camel.springboot.routeControllerBackoffMaxAttempts = 5",
    }
)
public class SupervisingRouteControllerTest {
    @Autowired
    private CamelContext context;

    @Test
    public void test() throws Exception {
        Assert.assertNotNull(context.getRouteController());
        Assert.assertTrue(context.getRouteController() instanceof SupervisingRouteController);

        SupervisingRouteController controller = context.adapt(ExtendedCamelContext.class).getSupervisingRouteController();

        // Wait for the controller to start the routes
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertEquals(3, controller.getControlledRoutes().size());
            Assert.assertEquals(500, controller.getInitialDelay());

            Assert.assertEquals(ServiceStatus.Started, context.getRouteController().getRouteStatus("foo"));
            Assert.assertEquals(ServiceStatus.Started, context.getRouteController().getRouteStatus("bar"));
            Assert.assertEquals(ServiceStatus.Started, context.getRouteController().getRouteStatus("jetty"));
        });
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {
        private static final int PORT = AvailablePortFinder.getNextAvailable();

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("timer:foo?period=5000")
                        .id("foo")
                        .startupOrder(2)
                        .to("mock:foo");
                    from("timer:bar?period=5000")
                        .id("bar")
                        .startupOrder(1)
                        .to("mock:bar");
                    fromF("jetty:http://localhost:%d", PORT)
                        .id("jetty")
                        .to("mock:jetty");
                }
            };
        }
    }
}
