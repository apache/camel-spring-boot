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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainListenerSupport;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a {@link MainListener} bean receives each lifecycle callback exactly once when the main run
 * controller is enabled (camel.main.run-controller=true).
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(properties = "camel.main.run-controller=true")
public class CamelMainListenerRunControllerTest {

    @Configuration
    static class Config {

        @Bean
        RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start").to("mock:result");
                }
            };
        }

        @Bean
        CountingMainListener countingMainListener() {
            return new CountingMainListener();
        }

    }

    @Autowired
    CamelContext camelContext;

    @Autowired
    CountingMainListener listener;

    @Test
    public void testMainListenerCallbacksFireExactlyOnce() {
        // the run controller notifies listeners from a background daemon thread,
        // so wait until the start phase has completed
        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> assertTrue(listener.count("afterStart") >= 1,
                        "afterStart should have been invoked"));

        // the counts must remain exactly 1 (hold for a while to catch late duplicate notifications
        // from the run controller background thread)
        await().during(2, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertEquals(1, listener.count("beforeInitialize"), () -> "beforeInitialize: " + listener.counts);
                    assertEquals(1, listener.count("beforeConfigure"), () -> "beforeConfigure: " + listener.counts);
                    assertEquals(1, listener.count("afterConfigure"), () -> "afterConfigure: " + listener.counts);
                    assertEquals(1, listener.count("beforeStart"), () -> "beforeStart: " + listener.counts);
                    assertEquals(1, listener.count("afterStart"), () -> "afterStart: " + listener.counts);
                });

        camelContext.stop();

        await().during(2, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertEquals(1, listener.count("beforeStop"), () -> "beforeStop: " + listener.counts);
                    assertEquals(1, listener.count("afterStop"), () -> "afterStop: " + listener.counts);
                });
    }

    public static class CountingMainListener extends MainListenerSupport {

        private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

        int count(String callback) {
            AtomicInteger counter = counts.get(callback);
            return counter != null ? counter.get() : 0;
        }

        private void record(String callback) {
            counts.computeIfAbsent(callback, k -> new AtomicInteger()).incrementAndGet();
        }

        @Override
        public void beforeInitialize(BaseMainSupport main) {
            record("beforeInitialize");
        }

        @Override
        public void beforeConfigure(BaseMainSupport main) {
            record("beforeConfigure");
        }

        @Override
        public void afterConfigure(BaseMainSupport main) {
            record("afterConfigure");
        }

        @Override
        public void beforeStart(BaseMainSupport main) {
            record("beforeStart");
        }

        @Override
        public void afterStart(BaseMainSupport main) {
            record("afterStart");
        }

        @Override
        public void beforeStop(BaseMainSupport main) {
            record("beforeStop");
        }

        @Override
        public void afterStop(BaseMainSupport main) {
            record("afterStop");
        }
    }

}
