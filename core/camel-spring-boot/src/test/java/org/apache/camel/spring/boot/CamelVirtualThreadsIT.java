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

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify that Camel routes use virtual threads when virtual threads are enabled.
 *
 * This is an integration test (IT) rather than a unit test because it requires a clean JVM
 * to properly test the ThreadType static initialization with virtual threads enabled.
 * The maven-failsafe-plugin forks a new JVM with the required system properties set.
 */
@Isolated
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, CamelVirtualThreadsIT.class,
                           CamelVirtualThreadsIT.TestConfiguration.class },
                properties = {
                    "spring.threads.virtual.enabled=true",
                    "camel.threads.virtual.enabled=true"
                })
public class CamelVirtualThreadsIT {

    private static final Logger LOG = LoggerFactory.getLogger(CamelVirtualThreadsIT.class);

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
                            boolean isVirtual = isVirtualThread(currentThread);

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

    private static boolean isVirtualThread(Thread thread) {
        try {
            Method isVirtual = Thread.class.getMethod("isVirtual");
            return (boolean) isVirtual.invoke(thread);
        } catch (Exception e) {
            return false;
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
    @EnabledForJreRange(min = JRE.JAVA_21)
    public void testRouteExecutesOnVirtualThread() throws Exception {
        // Check ThreadType.current()
        try {
            Class<?> threadTypeClass = Class.forName("org.apache.camel.util.concurrent.ThreadType");
            Object currentType = threadTypeClass.getMethod("current").invoke(null);
            LOG.info("ThreadType.current(): {}", currentType);
        } catch (Exception e) {
            LOG.debug("Could not get ThreadType: {}", e.getMessage());
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> isVirtualThread = new AtomicReference<>(false);
        AtomicReference<String> threadName = new AtomicReference<>("");
        AtomicReference<String> threadClassName = new AtomicReference<>("");

        // Create a test route using SEDA which uses ExecutorService
        RouteBuilder testRoute = new RouteBuilder() {
            @Override
            public void configure() {
                from("seda:virtualTest?concurrentConsumers=1")
                    .routeId("virtualTestRoute")
                    .process(exchange -> {
                        Thread currentThread = Thread.currentThread();
                        boolean isVirtual = isVirtualThread(currentThread);
                        isVirtualThread.set(isVirtual);
                        threadName.set(currentThread.getName());
                        threadClassName.set(currentThread.getClass().getName());

                        LOG.info("Thread executing route: {} (class={}, virtual={}, daemon={})",
                                currentThread.getName(), currentThread.getClass().getSimpleName(), isVirtual, currentThread.isDaemon());

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
            LOG.info("Asserting virtual thread: isVirtual={}, name={}, class={}",
                    isVirtualThread.get(), threadName.get(), threadClassName.get());

            assertThat(isVirtualThread.get())
                .as("Route should execute on a virtual thread when virtual threads are enabled.\n" +
                    "  Thread name: " + threadName.get() + "\n" +
                    "  Thread class: " + threadClassName.get() + "\n" +
                    "  System property camel.threads.virtual.enabled: " + System.getProperty("camel.threads.virtual.enabled") + "\n" +
                    "  System property spring.threads.virtual.enabled: " + System.getProperty("spring.threads.virtual.enabled"))
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