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

import org.apache.camel.CamelContext;
import org.apache.camel.component.kafka.share.KafkaShareAcknowledgeType;
import org.apache.camel.component.kafka.share.KafkaShareCommitMode;
import org.apache.camel.component.kafka.share.KafkaShareComponent;
import org.apache.camel.component.kafka.share.KafkaShareEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, KafkaShareComponentAutoConfiguration.class },
        properties = {
                "camel.component.kafka-share.brokers=localhost:9092",
                "camel.component.kafka-share.group-id=spring-boot-share",
                "camel.component.kafka-share.consumers-count=2",
                "camel.component.kafka-share.commit-mode=ASYNC",
                "camel.component.kafka-share.on-failure=REJECT",
                "camel.component.kafka-share.poll-timeout-ms=250" })
class KafkaShareComponentAutoConfigurationTest {

    @Autowired
    CamelContext context;

    @Test
    void componentAndEndpointUseSpringBootProperties() {
        KafkaShareComponent component = assertInstanceOf(KafkaShareComponent.class, context.getComponent("kafka-share"));
        assertEquals("localhost:9092", component.getConfiguration().getBrokers());
        assertEquals("spring-boot-share", component.getConfiguration().getGroupId());
        assertEquals(2, component.getConfiguration().getConsumersCount());
        assertEquals(KafkaShareCommitMode.ASYNC, component.getConfiguration().getCommitMode());
        assertEquals(KafkaShareAcknowledgeType.REJECT, component.getConfiguration().getOnFailure());
        assertEquals(250L, component.getConfiguration().getPollTimeoutMs());

        KafkaShareEndpoint endpoint = context.getEndpoint("kafka-share:work", KafkaShareEndpoint.class);
        assertEquals("spring-boot-share", endpoint.getConfiguration().getGroupId());
        assertEquals("localhost:9092", endpoint.getConfiguration().getBrokers());
        assertEquals(2, endpoint.getConfiguration().getConsumersCount());
        assertEquals(KafkaShareCommitMode.ASYNC, endpoint.getConfiguration().getCommitMode());
        assertEquals(KafkaShareAcknowledgeType.REJECT, endpoint.getConfiguration().getOnFailure());
    }
}
