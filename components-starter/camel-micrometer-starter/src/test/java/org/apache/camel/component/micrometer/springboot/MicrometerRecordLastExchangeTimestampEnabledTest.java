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
package org.apache.camel.component.micrometer.springboot;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = { CamelAutoConfiguration.class, MicrometerRecordLastExchangeTimestampEnabledTest.Routes.class },
                properties = { "camel.metrics.uri-tag-enabled=false" })
public class MicrometerRecordLastExchangeTimestampEnabledTest {

    private static final String LAST = "camel.exchanges.last.timestamp";

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    ProducerTemplate template;

    @Test
    void lastExchangeTimestamp() {
        template.sendBody("direct:start", "x");
        assertNotNull(meterRegistry.find("camel.exchange.event.notifier").timer(), "the exchange timers are recorded");
        Gauge gauge = meterRegistry.find(LAST).gauge();
        assertNotNull(gauge, "the gauge must be registered by default");
        assertTrue(gauge.value() > 0, "the gauge must hold the last exchange time");
    }

    @Configuration
    public static class Routes {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start").routeId("start").to("log:out?level=OFF");
                }
            };
        }
    }
}
