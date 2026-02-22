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
package org.apache.camel.opentelemetry.metrics.springboot.messagehistory;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.PointData;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.opentelemetry.metrics.springboot.AbstractOpenTelemetryTest;
import org.apache.camel.opentelemetry.metrics.springboot.CamelOpenTelemetryExtension;
import org.apache.camel.opentelemetry.metrics.springboot.routepolicy.OpenTelemetryContextOnlyPolicyTest;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.apache.camel.opentelemetry.metrics.OpenTelemetryConstants.DEFAULT_CAMEL_MESSAGE_HISTORY_METER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = { OpenTelemetryContextOnlyPolicyTest.TestConfiguration.class }, properties = {
        "camel.opentelemetry.metrics.enable-message-history=true" })
public class MessageHistoryTest extends AbstractOpenTelemetryTest {

    @Autowired
    private CamelContext context;

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private CamelOpenTelemetryExtension otelExtension;

    @EndpointInject("mock:result")
    private MockEndpoint mockResult;

    @Configuration
    public static class TestConfiguration {

        @Bean
        public CamelOpenTelemetryExtension otelExtension() {
            return CamelOpenTelemetryExtension.create();
        }

        @Bean
        public Meter meter(CamelOpenTelemetryExtension otelExtension) {
            return otelExtension.getOpenTelemetry().getMeter("meterTest");
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:foo")
                            .routeId("route1")
                            .to("mock:result").id("foo");
                }
            };
        }
    }

    @Test
    public void testMessageHistory() throws Exception {
        template.sendBody("direct:foo", "Hello");
        MockEndpoint.assertIsSatisfied(context);
        mockResult.expectedMessageCount(1);

        assertEquals(1, getAllPointData(DEFAULT_CAMEL_MESSAGE_HISTORY_METER_NAME).size());
        assertEquals(1, getPointData("foo").getCount());
    }

    private HistogramPointData getPointData(String routeId) {
        PointData pd = getAllPointDataForRouteId(DEFAULT_CAMEL_MESSAGE_HISTORY_METER_NAME, routeId)
                .stream()
                .findFirst().orElse(null);
        assertNotNull(pd);
        assertInstanceOf(HistogramPointData.class, pd);
        return (HistogramPointData) pd;
    }
}
