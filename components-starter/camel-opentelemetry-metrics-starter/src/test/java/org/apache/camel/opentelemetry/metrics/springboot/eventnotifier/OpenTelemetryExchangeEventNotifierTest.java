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
package org.apache.camel.opentelemetry.metrics.springboot.eventnotifier;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.data.PointData;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.opentelemetry.metrics.springboot.AbstractOpenTelemetryTest;
import org.apache.camel.opentelemetry.metrics.springboot.CamelOpenTelemetryExtension;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.apache.camel.opentelemetry.metrics.OpenTelemetryConstants.DEFAULT_CAMEL_EXCHANGE_ELAPSED_TIMER;
import static org.apache.camel.opentelemetry.metrics.OpenTelemetryConstants.DEFAULT_CAMEL_EXCHANGE_SENT_TIMER;
import static org.apache.camel.opentelemetry.metrics.OpenTelemetryConstants.ROUTE_ID_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest
public class OpenTelemetryExchangeEventNotifierTest extends AbstractOpenTelemetryTest {

    @Autowired
    private CamelContext context;

    @Autowired
    private ProducerTemplate template;

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
                    from("direct://foo").routeId("foo").to("mock://result");
                }
            };
        }
    }

    @Test
    public void testElapsedTimerEvents() throws Exception {
        template.sendBody("direct:foo", "Hello");
        mockResult.expectedMessageCount(1);

        PointData pd = getPointDataForRouteId(DEFAULT_CAMEL_EXCHANGE_ELAPSED_TIMER, "foo");
        assertEquals("foo", pd.getAttributes().get(stringKey(ROUTE_ID_ATTRIBUTE)));
    }

    @Test
    public void testSentTimerEvents() throws Exception {
        template.sendBody("direct:foo", "Hello");
        mockResult.expectedMessageCount(1);

        List<PointData> ls = getAllPointDataForRouteId(DEFAULT_CAMEL_EXCHANGE_SENT_TIMER, "foo");
        assertEquals(2, ls.size());
    }
}
