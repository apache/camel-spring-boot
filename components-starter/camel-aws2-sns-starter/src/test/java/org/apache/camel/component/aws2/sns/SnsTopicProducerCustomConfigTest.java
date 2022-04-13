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

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

//Based on SnsTopicProducerCustomConfigIT
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SnsTopicProducerCustomConfigTest.class,
                SnsTopicProducerCustomConfigTest.TestConfiguration.class
        }
)
public class SnsTopicProducerCustomConfigTest extends BaseSns {

    @Test
    public void sendInOnly() {
        Exchange exchange = producerTemplate.send("direct:start", ExchangePattern.InOnly, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(Sns2Constants.SUBJECT, "This is my subject");
                exchange.getIn().setBody("This is my message text.");
            }
        });

        assertNotNull(exchange.getIn().getHeader(Sns2Constants.MESSAGE_ID));
    }

    @Test
    public void sendInOut() {
        Exchange exchange = producerTemplate.send("direct:start", ExchangePattern.InOut, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(Sns2Constants.SUBJECT, "This is my subject");
                exchange.getIn().setBody("This is my message text.");
            }
        });

        assertNotNull(exchange.getMessage().getHeader(Sns2Constants.MESSAGE_ID));
    }

    @Test
    public void testFailure() {
        String uri = String.format("aws2-sns://%s?subject=The+subject+message&configuration=#class:%s&autoCreateTopic=true",
                sharedNameGenerator.getName(), EmptyTestSnsConfiguration.class.getName());
        Assertions.assertThrows(ResolveEndpointFailedException.class, () -> producerTemplate.requestBody(uri, "test", String.class));
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {

            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start")
                            .toF("aws2-sns://%s?subject=The+subject+message&configuration=#class:%s&autoCreateTopic=true",
                                    sharedNameGenerator.getName(), TestSnsConfiguration.class.getName());
                }
            };
        }
    }

}
