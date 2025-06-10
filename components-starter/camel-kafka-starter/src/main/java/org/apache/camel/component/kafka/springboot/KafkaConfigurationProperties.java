package org.apache.camel.component.kafka.springboot;

@ConfigurationProperties(prefix = "camel.component.kafka")
public class KafkaConfigurationProperties {
    private String brokers = "localhost:9092";

}
