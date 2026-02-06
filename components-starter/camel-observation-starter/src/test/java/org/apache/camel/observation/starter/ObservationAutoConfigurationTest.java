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
package org.apache.camel.observation.starter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.observation.MicrometerObservationTracer;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                CamelAutoConfiguration.class,
                ObservationAutoConfiguration.class,
                ObservationAutoConfigurationTest.TestConfiguration.class
        },
        properties = {
                "spring.main.banner-mode=off",
                "management.endpoints.web.exposure.include=health,metrics",
                "management.endpoint.health.show-details=always"
        }
)
public class ObservationAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @LocalManagementPort
    private int managementPort;

    /**
     * Test that the MicrometerObservationTracer bean is created when Spring Actuator
     * classes are present. This verifies the @ConditionalOnClass annotation is correct.
     */
    @Test
    public void testObservationTracerBeanCreated() {
        assertTrue(applicationContext.containsBean("micrometerObservationTracer"),
                "MicrometerObservationTracer bean should be created when actuator is present");

        MicrometerObservationTracer tracer = applicationContext.getBean(MicrometerObservationTracer.class);
        assertNotNull(tracer, "MicrometerObservationTracer should not be null");
    }

    /**
     * Test that a message can flow through a traced route and the camel.component metric is recorded.
     * The metric name "camel.component" is defined in MicrometerObservationTracer.
     */
    @Test
    public void testTracedRouteExecutionWithCamelMetric() throws Exception {
        // Send a message through the route - this should be traced and create metrics
        producerTemplate.sendBody("direct:test", "Hello Traced World");

        // Verify the metrics list contains the camel.component metric
        ResponseEntity<String> metricsResponse = restTemplateBuilder
                .rootUri("http://localhost:" + managementPort + "/actuator")
                .build()
                .exchange("/metrics", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(metricsResponse.getBody()).contains("camel.component");

        // Verify the camel.component metric details are accessible
        ResponseEntity<String> camelMetricResponse = restTemplateBuilder
                .rootUri("http://localhost:" + managementPort + "/actuator")
                .build()
                .exchange("/metrics/camel.component", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(camelMetricResponse.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(camelMetricResponse.getBody()).contains("camel.component", "camel-direct");
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder testRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:test")
                        .routeId("test-route")
                        .log("Processing: ${body}")
                        .to("mock:result");
                }
            };
        }

        /**
         * Provide a Tracer bean for testing purposes using OpenTelemetry.
         */
        @Bean
        public Tracer tracer() {
            SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build();
            io.opentelemetry.api.trace.Tracer otelTracer = sdkTracerProvider.get("test");
            ContextPropagators contextPropagators = ContextPropagators.noop();
            OtelCurrentTraceContext otelCurrentTraceContext = new OtelCurrentTraceContext();
            return new OtelTracer(otelTracer, otelCurrentTraceContext, null);
        }

        /**
         * Customizer to register observation handlers for both tracing and metrics.
         * This replaces the no-op handler from ObservationAutoConfiguration for testing purposes.
         */
        @Bean
        public ObservationRegistryCustomizer<ObservationRegistry> metricsObservationRegistryCustomizer(
                MeterRegistry meterRegistry, Tracer tracer) {
            return registry -> {
                // Register TracingObservationHandler first to set up tracing context
                registry.observationConfig()
                    .observationHandler(new DefaultTracingObservationHandler(tracer));

                // Then register TracingAwareMeterObservationHandler for metrics
                TracingAwareMeterObservationHandler<Observation.Context> meterHandler = new TracingAwareMeterObservationHandler<>(
                    new DefaultMeterObservationHandler(meterRegistry),
                    tracer
                );
                registry.observationConfig().observationHandler(meterHandler);
            };
        }
    }
}
