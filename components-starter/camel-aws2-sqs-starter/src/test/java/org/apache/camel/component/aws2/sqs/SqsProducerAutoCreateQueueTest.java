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
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.util.ArrayList;
import java.util.Collection;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SqsProducerAutoCreateQueueTest.class,
                SqsProducerAutoCreateQueueTest.TestConfiguration.class
        }
)
public class SqsProducerAutoCreateQueueTest extends BaseSqs {

    @EndpointInject("direct:start")
    private ProducerTemplate template;

    @EndpointInject("mock:result")
    private MockEndpoint result;

    @BeforeEach
    public void beforeEach() throws Exception {

        SqsClient client = AWSSDKClientUtils.newSQSClient();
        for (int i = 0; i < 2000; i++) {
            client.createQueue(CreateQueueRequest.builder().queueName("queue-" + String.valueOf(i)).build());
        }
    }

    @Test
    public void sendInOnly() throws Exception {
        result.expectedMessageCount(5);

        template.send("direct:start", ExchangePattern.InOnly, new Processor() {
            public void process(Exchange exchange) {
                Collection c = new ArrayList<Integer>();
                c.add("1");
                c.add("2");
                c.add("3");
                c.add("4");
                c.add("5");
                exchange.getIn().setBody(c);
            }
        });

        assertMockEndpointsSatisfied();
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
                    from("direct:start").startupOrder(2).setHeader(Sqs2Constants.SQS_OPERATION, constant("sendBatchMessage"))
                            .toF("aws2-sqs://%s?autoCreateQueue=true", "queue-2001");

                    fromF("aws2-sqs://%s?deleteAfterRead=true&autoCreateQueue=true", "queue-2001")
                            .startupOrder(1).log("${body}").to("mock:result");
                }
            };
        }
    }
}
