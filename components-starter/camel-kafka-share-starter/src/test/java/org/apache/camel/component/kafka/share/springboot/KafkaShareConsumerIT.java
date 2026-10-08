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
package org.apache.camel.component.kafka.share.springboot;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.share.KafkaShareComponent;
import org.apache.camel.component.kafka.share.KafkaShareConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.infra.kafka.services.KafkaService;
import org.apache.camel.test.infra.kafka.services.KafkaServiceFactory;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.apache.camel.builder.Builder.body;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(classes = KafkaShareConsumerIT.TestConfiguration.class,
        properties = {
                "camel.component.kafka-share.group-id=spring-boot-share-it",
                "camel.component.kafka-share.consumers-count=3",
                "camel.component.kafka-share.max-poll-records=5",
                "camel.component.kafka-share.acquire-mode=record_limit" })
@DisabledIfSystemProperty(named = "ci.env.name", matches = "github.com",
        disabledReason = "Disabled on GH Action due to Docker limit")
class KafkaShareConsumerIT {

    private static final String WORK_TOPIC = "spring-boot-share-work";
    private static final String RETRY_TOPIC = "spring-boot-share-retry";
    private static final String GROUP = "spring-boot-share-it";

    @RegisterExtension
    static KafkaService service = KafkaServiceFactory.createSingletonService();

    @Autowired
    CamelContext context;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("camel.component.kafka-share.brokers", service::getBootstrapServers);
    }

    @BeforeAll
    static void createTopicsAndGroup() throws Exception {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, service.getBootstrapServers());
        try (Admin admin = Admin.create(properties)) {
            admin.createTopics(List.of(new NewTopic(WORK_TOPIC, 1, (short) 1), new NewTopic(RETRY_TOPIC, 1, (short) 1)))
                    .all().get(30, TimeUnit.SECONDS);
            // Read records produced before the share group's first poll as well.
            admin.incrementalAlterConfigs(Map.of(new ConfigResource(ConfigResource.Type.GROUP, GROUP),
                    List.of(new AlterConfigOp(new ConfigEntry("share.auto.offset.reset", "earliest"),
                            AlterConfigOp.OpType.SET))))
                    .all().get(30, TimeUnit.SECONDS);
        }
    }

    @Test
    void consumesRecordsUsingAutoConfiguredComponent() throws Exception {
        KafkaShareComponent component = assertInstanceOf(KafkaShareComponent.class, context.getComponent("kafka-share"));
        assertEquals(service.getBootstrapServers(), component.getConfiguration().getBrokers());
        assertEquals(GROUP, component.getConfiguration().getGroupId());
        assertEquals(3, component.getConfiguration().getConsumersCount());

        MockEndpoint work = context.getEndpoint("mock:work", MockEndpoint.class);
        work.expectedBodiesReceivedInAnyOrder("message-0", "message-1", "message-2", "message-3", "message-4");
        work.expectsNoDuplicates(body());
        work.expectedHeaderReceived(KafkaShareConstants.TOPIC, WORK_TOPIC);
        work.expectedHeaderReceived(KafkaShareConstants.PARTITION, 0);
        work.expectedHeaderReceived(KafkaShareConstants.DELIVERY_COUNT, (short) 1);

        produce(WORK_TOPIC, 5);

        work.assertIsSatisfied(60000);
    }

    @Test
    void failedRecordIsReleasedAndDeliveredAgain() throws Exception {
        MockEndpoint deliveries = context.getEndpoint("mock:deliveries", MockEndpoint.class);
        deliveries.expectedMessageCount(2);
        MockEndpoint retried = context.getEndpoint("mock:retried", MockEndpoint.class);
        retried.expectedBodiesReceived("message-0");
        retried.expectedHeaderReceived(KafkaShareConstants.DELIVERY_COUNT, (short) 2);

        produce(RETRY_TOPIC, 1);

        MockEndpoint.assertIsSatisfied(60, TimeUnit.SECONDS, deliveries, retried);
    }

    private static void produce(String topic, int count) throws Exception {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, service.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            for (int i = 0; i < count; i++) {
                producer.send(new ProducerRecord<>(topic, "key-" + i, "message-" + i)).get(30, TimeUnit.SECONDS);
            }
        }
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {
        @Bean
        RouteBuilder routes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    // All component settings come from Spring Boot, not the endpoint URI or a manual bean.
                    from("kafka-share:" + WORK_TOPIC).routeId("work").to("mock:work");
                    from("kafka-share:" + RETRY_TOPIC).routeId("retry")
                            .errorHandler(noErrorHandler())
                            .to("mock:deliveries")
                            .filter(header(KafkaShareConstants.DELIVERY_COUNT).isEqualTo(1))
                                .throwException(new IllegalStateException("First delivery fails"))
                            .end()
                            .to("mock:retried");
                }
            };
        }
    }
}
