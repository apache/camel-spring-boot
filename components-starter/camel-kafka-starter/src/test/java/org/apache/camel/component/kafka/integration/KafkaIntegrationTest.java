package org.apache.camel.component.kafka.integration;
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = { "test-topic" })
public class KafkaIntegrationTest extends CamelSpringBootTestSupport{
    @Autowired
    private CamelContext camelContext;

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Test
    void testKafkaRoute() throws Exception {
        resultEndpoint.expectedMessageCount(1);

        template.sendBody("kafka:test-topic", "hello");

        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("kafka:test-topic")
                        .to("mock:result");
            }
        };
    }


}
