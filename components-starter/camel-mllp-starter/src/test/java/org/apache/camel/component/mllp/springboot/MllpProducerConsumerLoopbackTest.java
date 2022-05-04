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
package org.apache.camel.component.mllp.springboot;


import static org.hamcrest.MatcherAssert.assertThat;


import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.MllpConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.hamcrest.CoreMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MllpProducerConsumerLoopbackTest.class,
        MllpProducerConsumerLoopbackTest.TestConfiguration.class
    }
)
public class MllpProducerConsumerLoopbackTest {
    
    Logger log = LoggerFactory.getLogger(MllpProducerConsumerLoopbackTest.class);
    int mllpPort = AvailablePortFinder.getNextAvailable();
    String mllpHost = "localhost";

    @EndpointInject("direct://source")
    ProducerTemplate source;

    @EndpointInject("mock://acknowledged")
    MockEndpoint acknowledged;

    @BeforeAll
    public static void setUpClass() throws Exception {
        assumeTrue(System.getenv("BUILD_ID") == null,
                "Skipping test running in CI server - Fails sometimes on CI server with address already in use");
    }

    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.setUseMDCLogging(true);
                ((DefaultCamelContext)context).setName("MllpProducerConsumerLoopbackTest");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // TODO Auto-generated method stub

            }
        };
    }

    
    @Test
    public void testLoopbackWithOneMessage() throws Exception {
        acknowledged.reset();
        String testMessage = Hl7TestMessageGenerator.generateMessage();
        acknowledged.expectedBodiesReceived(testMessage);

        String acknowledgement = source.requestBody((Object) testMessage, String.class);
        assertThat("Should be acknowledgment for message 1", acknowledgement,
                CoreMatchers.containsString(String.format("MSA|AA|00001")));

        acknowledged.assertIsSatisfied(60000);
    }

    @Test
    public void testLoopbackWithMultipleMessages() throws Exception {
        acknowledged.reset();
        int messageCount = 1000;
        acknowledged.expectedMessageCount(messageCount);

        for (int i = 1; i <= messageCount; ++i) {
            log.debug("Processing message {}", i);
            String testMessage = Hl7TestMessageGenerator.generateMessage(i);
            acknowledged.message(i - 1).body().isEqualTo(testMessage);
            String acknowledgement = source.requestBody((Object) testMessage, String.class);
            assertThat("Should be acknowledgment for message " + i, acknowledgement,
                    CoreMatchers.containsString(String.format("MSA|AA|%05d", i)));
        }

        acknowledged.assertIsSatisfied(60000);
    }
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder1() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    String routeId = "mllp-receiver";
                    fromF("mllp://%s:%d?autoAck=true&readTimeout=5000", mllpHost, mllpPort).id(routeId)
                            .convertBodyTo(String.class)
                            .to(acknowledged)
                            .process(new PassthroughProcessor("after send to result"))
                            .log(LoggingLevel.DEBUG, routeId, "Receiving: ${body}");
                }
            };
        }
        
        @Bean
        public RouteBuilder routeBuilder2() {
            return new RouteBuilder() {
                String routeId = "mllp-sender";
                @Override
                public void configure() {
                    from(source.getDefaultEndpoint()).routeId(routeId)
                            .log(LoggingLevel.DEBUG, routeId, "Sending: ${body}")
                            .toF("mllp://%s:%d?readTimeout=5000", mllpHost, mllpPort)
                            .setBody(header(MllpConstants.MLLP_ACKNOWLEDGEMENT));
                }
            };
        }
    }
    
   

}
