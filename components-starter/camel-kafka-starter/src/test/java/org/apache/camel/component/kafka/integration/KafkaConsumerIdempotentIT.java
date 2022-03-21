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
package org.apache.camel.component.kafka.integration;

import java.util.Arrays;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.idempotent.kafka.KafkaIdempotentRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.apache.camel.component.kafka.serde.KafkaSerdeHelper.numericHeader;

@EnabledIfSystemProperty(named = "enable.kafka.consumer.idempotency.tests", matches = "true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                BaseEmbeddedKafkaTestSupport.DefaulKafkaComponent.class,
                KafkaConsumerIdempotentIT.class,
                KafkaConsumerIdempotentIT.TestConfiguration.class,
        }
)
public class KafkaConsumerIdempotentIT extends KafkaConsumerIdempotentTestSupport {

    public static final String TOPIC = "idempt";

    private final String from = "kafka:" + TOPIC
            + "?groupId=group2&autoOffsetReset=earliest"
            + "&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
            + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
            + "&autoCommitIntervalMs=1000&sessionTimeoutMs=30000&autoCommitEnable=true"
            + "&interceptorClasses=org.apache.camel.component.kafka.integration.MockConsumerInterceptor";

    @EndpointInject("mock:result")
    private MockEndpoint to;

    private int size = 200;

    @BeforeEach
    public void before() {
        kafkaAdminClient.deleteTopics(Arrays.asList(TOPIC, "TEST_IDEMPOTENT")).all();
        doSend(size, TOPIC);
    }

    @AfterEach
    public void after() {
        kafkaAdminClient.deleteTopics(Arrays.asList(TOPIC, "TEST_IDEMPOTENT")).all();
    }

    @Test
    @DisplayName("Numeric headers is consumable when using idempotent (CAMEL-16914)")
    public void kafkaIdempotentMessageIsConsumedByCamel() throws InterruptedException {
        doRun(to, size);
    }

    @Configuration
    public class TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(from).routeId("foo")
                            .idempotentConsumer(numericHeader("id"))
                            .idempotentRepository("kafkaIdempotentRepository")
                            .to(to);
                }
            };
        }

        @Bean("kafkaIdempotentRepository")
        public KafkaIdempotentRepository createKafkaIdempotentRepository(){
            return new KafkaIdempotentRepository("TEST_IDEMPOTENT", getBootstrapServers());
        }
    }
}
