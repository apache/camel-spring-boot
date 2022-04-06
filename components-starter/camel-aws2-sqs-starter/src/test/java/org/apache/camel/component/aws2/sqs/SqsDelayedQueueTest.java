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
package org.apache.camel.component.aws2.sqs;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SqsDelayedQueueTest.class,
                BaseSqs.TestConfiguration.class
        }
)
/**
 * Based on camel-quarkus Aws2SqsTest#sqsAutoCreateDelayedQueue
 */
public class SqsDelayedQueueTest extends BaseSqs {

    @EndpointInject("direct:start")
    private ProducerTemplate template;

    @EndpointInject("mock:result")
    private MockEndpoint result;

    @Test
    public void delayedQueue() throws Exception {
        int delay = 20;
        String delayedQueueuName = sharedNameGenerator.getName() + "_delayed";
        Instant start = Instant.now(); 
        //create delayed queue
        List<String> queues = producerTemplate
                .requestBody(
                        String.format("aws2-sqs://%s?autoCreateQueue=true&delayQueue=true&delaySeconds=%d&operation=listQueues", delayedQueueuName, delay),
                        null,
                        ListQueuesResponse.class)
                .queueUrls();
        
        String msg = sendSingleMessageToQueue(delayedQueueuName);
        awaitMessageWithExpectedContentFromQueue(msg, delayedQueueuName);

        Assertions.assertTrue(Duration.between(start, Instant.now()).getSeconds() >= delay);
    }

    private void awaitMessageWithExpectedContentFromQueue(String expectedContent, String queueName) {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> expectedContent.equals(receiveMessageFromQueue(queueName, false)));

    }
}
