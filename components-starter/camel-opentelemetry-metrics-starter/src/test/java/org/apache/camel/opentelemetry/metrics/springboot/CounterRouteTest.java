package org.apache.camel.opentelemetry.metrics.springboot;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.PointData;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest
public class CounterRouteTest {

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
                    from("direct:in1")
                            .to("opentelemetry-metrics:counter:A?increment=5")
                            .to("mock:result");
                }
            };
        }
    }

    @Test
    public void testIterativeIncrement() throws Exception {
        for (int i = 0; i < 3; i++) {
            template.sendBody("direct:in1", new Object());
        }
        mockResult.expectedMessageCount(3);
        assertEquals(15L, getMetric("A").getValue());
    }

    protected LongPointData getMetric(String metricName) {
        PointData pd = otelExtension.getMetrics().stream()
                .filter(d -> d.getName().equals(metricName))
                .map(metricData -> metricData.getData().getPoints())
                .flatMap(Collection::stream)
                .findFirst().orElse(null);

        assertInstanceOf(LongPointData.class, pd, "Expected LongPointData");
        return (LongPointData) pd;
    }
}
