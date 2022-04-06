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

import org.apache.camel.Configuration;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SqsOperationsTest.class,
                SqsOperationsTest.TestConfiguration.class
        }
)
class SqsOperationsTest extends  BaseSqs {

    private static final String queueName = "Aws2SqsTest_queue_" + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);

    private static String queueUrl;

    @BeforeAll
    protected static void setupResources()  {
        final SqsClient sqsClient = AWSSDKClientUtils.newSQSClient();
        {
            queueUrl = sqsClient.createQueue(
                            CreateQueueRequest.builder()
                                    .queueName(queueName)
                                    .build())
                    .queueUrl();
        }
    }

    @AfterAll
    protected static void cleanupResources() {
        final SqsClient sqsClient = AWSSDKClientUtils.newSQSClient();

        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }

    @AfterEach
    void purgeQueueAndWait() {
        purgeQueue(queueName);
        Assertions.assertNull(receiveMessageFromQueue(queueName, false));
    }


    @Test
    void simpleInOut() {
        Assertions.assertTrue(clientListQueues().stream().distinct().anyMatch(u -> u.contains(queueName)));

        final String msg = sendSingleMessageToQueue(queueName);
        awaitMessageWithExpectedContentFromQueue(msg, queueName);
    }

    @Test
    void listQueues() {
        Assertions.assertTrue(clientListQueues().stream().distinct().anyMatch(u -> u.contains(queueName)));
    }

    @Test
    void sqsDeleteMessage() {
        sendSingleMessageToQueue(queueName);
        final String receipt = receiveReceipt(queueName);
        final String msg = sendSingleMessageToQueue(queueName);
        deleteMessageFromQueue(queueName, receipt);
        // assertion is here twice because in case delete wouldn't work in our queue would be two messages
        // it's possible that the first retrieval would retrieve the correct message and therefore the test
        // would incorrectly pass. By receiving message twice we check if the message was really deleted.
        Assertions.assertEquals(receiveMessageFromQueue(queueName, false), msg);
        Assertions.assertEquals(receiveMessageFromQueue(queueName, false), msg);
    }

    @Test
    void sqsSendBatchMessage() {
        final List<String> messages = new ArrayList<>(Arrays.asList(
                "Hello from camel-quarkus",
                "This is a batch message test",
                "Let's add few more messages",
                "Next message will be last",
                "Goodbye from camel-quarkus"));
        Assertions.assertEquals(messages.size(), sendMessageBatchAndRetrieveSuccessCount(queueName, messages));
    }

    // helper methods

    private void purgeQueue(String queueName) {
        producerTemplate.sendBodyAndHeader("aws2-sqs://" + queueName + "?operation=purgeQueue",
                null,
                Sqs2Constants.SQS_QUEUE_PREFIX,
                queueName);
    }

    private int sendMessageBatchAndRetrieveSuccessCount(String queueName, List<String> messages) {
        return producerTemplate.requestBody(
                "aws2-sqs://" + queueName + "?operation=sendBatchMessage",
                messages,
                SendMessageBatchResponse.class).successful().size();
    }

    private List<String> clientListQueues() {
        return producerTemplate.requestBody("aws2-sqs://" + queueName + "?operation=listQueues", null, ListQueuesResponse.class)
                .queueUrls();
    }



    private String receiveReceipt(String queueName) {
        Exchange exchange = consumerTemplate.receive("aws2-sqs://" + queueName, 5000);
        return exchange.getIn().getHeader(Sqs2Constants.RECEIPT_HANDLE, String.class);
    }

    private void awaitMessageWithExpectedContentFromQueue(String expectedContent, String queueName) {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(
                () -> expectedContent.equals(receiveMessageFromQueue(queueName, true)));
    }

    private void deleteMessageFromQueue(String queueName, String receipt) {
        producerTemplate.sendBodyAndHeader("aws2-sqs://" + queueName + "?operation=deleteMessage",
                null,
                Sqs2Constants.RECEIPT_HANDLE,
                URLDecoder.decode(receipt, StandardCharsets.UTF_8));
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  BaseSqs.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            final String sqsEndpointUri = String
                    .format("aws2-sqs://%s?messageRetentionPeriod=%s&maximumMessageSize=%s&visibilityTimeout=%s&policy=%s&autoCreateQueue=true",
                            sharedNameGenerator.getName(),
                            "1209600", "65536", "60",
                            "file:src/test/resources/org/apache/camel/component/aws2/sqs/policy.txt");
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start").to(sqsEndpointUri);

                    from(sqsEndpointUri).to("mock:result");
                }
            };
        }
    }

}
