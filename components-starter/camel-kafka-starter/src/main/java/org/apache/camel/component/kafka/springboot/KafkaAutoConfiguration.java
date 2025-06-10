package org.apache.camel.component.kafka.springboot;

@Configuration
@ConditionalOnClass(KafkaComponent.class)
@EnableConfigurationProperties(KafkaConfigurationProperties.class)
public class KafkaAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public KafkaComponent kafkaComponent(CamelContext context) {
        KafkaComponent component = new KafkaComponent();
        component.setCamelContext(context);
        return component;
    }
}
