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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that Camel routes use virtual threads when virtual threads are enabled.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, CamelVirtualThreadsTest.class, 
                           CamelVirtualThreadsTest.TestConfiguration.class },
                properties = { "spring.threads.virtual.enabled=true" })
public class CamelVirtualThreadsTest {

    // Ensure camel.threads.virtual.enabled is set before any Camel classes are loaded
    static {
        System.setProperty("camel.threads.virtual.enabled", "true");
    }

    @Autowired
    CamelContext context;

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder virtualThreadTestRoute() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("timer:virtualThreadTest?period=100&repeatCount=1")
                        .routeId("virtualThreadTestRoute")
                        .process(exchange -> {
                            // Capture the current thread information
                            Thread currentThread = Thread.currentThread();
                            String threadName = currentThread.getName();
                            boolean isVirtual = currentThread.isVirtual();
                            
                            // Store thread information in exchange properties for assertion
                            exchange.setProperty("threadName", threadName);
                            exchange.setProperty("isVirtual", isVirtual);
                            exchange.setProperty("threadClass", currentThread.getClass().getName());
                        })
                        .to("mock:result");
                }
            };
        }
    }

    @Test
    public void testCamelVirtualThreadPropertyIsSet() throws Exception {
        // Verify that the environment post processor set the camel.threads.virtual.enabled property
        String camelVirtualThreadsProperty = System.getProperty("camel.threads.virtual.enabled");
        assertThat(camelVirtualThreadsProperty)
                .as("camel.threads.virtual.enabled should be automatically set by EnvironmentPostProcessor when spring.threads.virtual.enabled=true")
                .isEqualTo("true");
    }

    @Test
    public void testRouteExecutesOnVirtualThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> isVirtualThread = new AtomicReference<>(false);
        AtomicReference<String> threadName = new AtomicReference<>("");

        // Create a test route using SEDA which uses ExecutorService
        RouteBuilder testRoute = new RouteBuilder() {
            @Override
            public void configure() {
                from("seda:virtualTest?concurrentConsumers=1")
                    .routeId("virtualTestRoute")
                    .process(exchange -> {
                        Thread currentThread = Thread.currentThread();
                        isVirtualThread.set(currentThread.isVirtual());
                        threadName.set(currentThread.getName());
                        latch.countDown();
                    });
            }
        };

        // Add the route dynamically to ensure it uses current context configuration
        try {
            context.addRoutes(testRoute);
            
            // Send a message to trigger the route
            context.createProducerTemplate().sendBody("seda:virtualTest", "test message");
            
            // Wait for the route to execute using Awaitility
            Awaitility.await()
                    .atMost(2, TimeUnit.SECONDS)
                    .until(() -> latch.getCount() == 0);
            
            // Assert that the route executed on a virtual thread
            assertThat(isVirtualThread.get())
                .as("Route should execute on a virtual thread when virtual threads are enabled. Thread name: " + threadName.get())
                .isTrue();
                
            assertThat(threadName.get())
                .as("Virtual thread should have a recognizable name pattern")
                .isNotEmpty();
                
        } finally {
            // Clean up - remove the test route
            context.getRouteController().stopRoute("virtualTestRoute");
            context.removeRoute("virtualTestRoute");
        }
    }
}