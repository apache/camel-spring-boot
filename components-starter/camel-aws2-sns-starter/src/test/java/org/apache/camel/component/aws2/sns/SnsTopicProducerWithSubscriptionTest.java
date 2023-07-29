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
package org.apache.camel.component.aws2.sns;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.infra.common.SharedNameGenerator;
import org.apache.camel.test.infra.common.TestEntityNameGenerator;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//Based on SnsTopicProducerWithSubscriptionIT
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SnsTopicProducerWithSubscriptionTest.class,
                SnsTopicProducerWithSubscriptionTest.TestConfiguration.class
        }
)
@Disabled("Need to review this later for localstack 2.x upgrade")
public class SnsTopicProducerWithSubscriptionTest extends BaseSns {

    private static final Logger LOG = LoggerFactory.getLogger(SnsTopicProducerWithSubscriptionTest.class);

    @Bean
    public SqsClient sqsClient(CamelContext context) {
        return AWSSDKClientUtils.newSQSClient();
    }

    public List<Message> receive() {
        SqsClient client = context.getRegistry().findSingleByType(SqsClient.class);
        Supplier<String> sqsQueueSupplier = context.getRegistry().lookupByNameAndType("sqsQueue", Supplier.class);

        LOG.debug("Consuming messages from {}", sqsQueueSupplier.get());

        final int maxNumberOfMessages = 1;

        int maxWaitTime = 10;
        final ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(sqsQueueSupplier.get())
                .waitTimeSeconds(maxWaitTime)
                .maxNumberOfMessages(maxNumberOfMessages)
                .build();

        while (true) {
            ReceiveMessageResponse response = client.receiveMessage(request);

            if (!response.sdkHttpResponse().isSuccessful()) {
                LOG.warn("Did not receive a success response from SQS: status code {}",
                        response.sdkHttpResponse().statusCode());
            }

            List<Message> messages = response.messages();
            for (Message message : messages) {
                LOG.info("Received message: {}", message.body());
            }

            if (!messages.isEmpty()) {
                return messages;
            }
        }
    }



    @Test
    public void sendInOnly() {
        Exchange exchange = producerTemplate.send("direct:start", ExchangePattern.InOnly, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(Sns2Constants.SUBJECT, "This is my subject");
                exchange.getIn().setBody("This is my message text.");
            }
        });

        assertNotNull(exchange.getIn().getHeader(Sns2Constants.MESSAGE_ID));

        List<Message> messages = receive();

        assertEquals(1, messages.size(), "Did not receive as many messages as expected");
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  BaseSns.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder(Supplier<String> queueSupplier) {

            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start")
                            .toF("aws2-sns://%s?subject=The+subject+message&autoCreateTopic=true&subscribeSNStoSQS=true&queueUrl=%s",
                                    sharedNameGenerator.getName(), queueSupplier.get());
                }
            };
        }

        @Bean(value = "sqsQueue")
        public Supplier<String> sqsQueue() {
            SqsClient client = AWSSDKClientUtils.newSQSClient();;

            String queue = sharedNameGenerator.getName() + "sqs";

            GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(queue)
                    .build();

            String sqsQueueUrl;
            try {
                GetQueueUrlResponse getQueueUrlResult = client.getQueueUrl(getQueueUrlRequest);

                sqsQueueUrl = getQueueUrlResult.queueUrl();
            } catch (QueueDoesNotExistException e) {
                CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                        .queueName(queue)
                        .build();

                sqsQueueUrl = client.createQueue(createQueueRequest).queueUrl();
            }

            String finalSqsQueueUrl = sqsQueueUrl;
            return () -> finalSqsQueueUrl;
        }
    }
}
