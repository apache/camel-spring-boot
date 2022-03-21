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

import java.util.Collections;
import java.util.Properties;
import java.util.stream.StreamSupport;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                BaseEmbeddedKafkaTestSupport.DefaulKafkaComponent.class,
                KafkaConsumerTopicIsPatternIT.class,
                KafkaConsumerTopicIsPatternIT.TestConfiguration.class,
        }
)
public class KafkaConsumerTopicIsPatternIT extends BaseEmbeddedKafkaTestSupport {

    public static final String TOPIC = "vess123d";
    public static final String TOPIC_PATTERN = "v.*d";

    private final String from = "kafka:" + TOPIC_PATTERN + "?topicIsPattern=true&groupId=group1&autoOffsetReset=earliest"
            + "&autoCommitIntervalMs=1000&sessionTimeoutMs=30000&autoCommitEnable=true&interceptorClasses=org.apache.camel.component.kafka.integration.MockConsumerInterceptor";

    @EndpointInject("mock:result")
    private MockEndpoint to;

    private org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;

    @BeforeEach
    public void before() {
        Properties props = getDefaultProperties();
        producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);
        MockConsumerInterceptor.recordsCaptured.clear();

    }

    @AfterEach
    public void after() {
        if (producer != null) {
            producer.close();
        }
        // clean all test topics
        kafkaAdminClient.deleteTopics(Collections.singletonList(TOPIC));
    }

    @Test
    public void kafkaTopicIsPattern() throws Exception {
        to.expectedMessageCount(5);
        to.expectedBodiesReceivedInAnyOrder("message-0", "message-1", "message-2", "message-3", "message-4");
        to.allMessages().header(KafkaConstants.TOPIC).isEqualTo(TOPIC);
        // The LAST_RECORD_BEFORE_COMMIT header should not be configured on any
        // exchange because autoCommitEnable=true
        to.expectedHeaderValuesReceivedInAnyOrder(KafkaConstants.LAST_RECORD_BEFORE_COMMIT, null, null, null, null, null);

        for (int k = 0; k < 5; k++) {
            String msg = "message-" + k;
            ProducerRecord<String, String> data = new ProducerRecord<>(TOPIC, "1", msg);
            producer.send(data);
        }

        to.assertIsSatisfied(3000);

        assertEquals(5, StreamSupport.stream(MockConsumerInterceptor.recordsCaptured.get(0).records(TOPIC).spliterator(), false)
                .count());
    }

    @Configuration
    public class TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(from).to(to);
                }
            };
        }
    }

}
