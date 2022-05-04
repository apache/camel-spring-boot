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
import org.apache.camel.component.mllp.springboot.rule.MllpClientResource;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.hamcrest.CoreMatchers;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MllpTcpServerConsumerMulitpleTcpPacketTest.class,
        MllpTcpServerConsumerMulitpleTcpPacketTest.TestConfiguration.class
    }
)
public class MllpTcpServerConsumerMulitpleTcpPacketTest {
    
    @RegisterExtension
    public static MllpClientResource mllpClient = new MllpClientResource();

    @EndpointInject("mock://result")
    MockEndpoint result;

    

    @Autowired
    ProducerTemplate template;
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.setUseMDCLogging(true);
                ((DefaultCamelContext)context).setName("MllpTcpServerConsumerMulitpleTcpPacketTest");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // TODO Auto-generated method stub

            }
        };
    }

    
    @Test
    public void testReceiveSingleMessage() throws Exception {
        result.reset();
        mllpClient.connect();

        String message = Hl7TestMessageGenerator.generateMessage();
        result.expectedBodiesReceived(message);

        mllpClient.sendFramedDataInMultiplePackets(message, (byte) '\r');
        String acknowledgement = mllpClient.receiveFramedData();

        result.assertIsSatisfied(10000);

        assertThat("Should be acknowledgment for message 1", acknowledgement,
                CoreMatchers.containsString(String.format("MSA|AA|00001")));
    }

    @Test
    public void testReceiveMultipleMessages() throws Exception {
        result.reset();
        int sendMessageCount = 100;
        result.expectedMessageCount(sendMessageCount);

        mllpClient.setSoTimeout(10000);
        mllpClient.connect();

        for (int i = 1; i <= sendMessageCount; ++i) {
            String testMessage = Hl7TestMessageGenerator.generateMessage(i);
            result.message(i - 1).body().isEqualTo(testMessage);
            mllpClient.sendFramedDataInMultiplePackets(testMessage, (byte) '\r');
            String acknowledgement = mllpClient.receiveFramedData();
            assertThat("Should be acknowledgment for message " + i, acknowledgement,
                    CoreMatchers.containsString(String.format("MSA|AA|%05d", i)));
        }

        result.assertIsSatisfied(10000);
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
                public void configure() throws Exception {
                    final int groupInterval = 1000;
                    final boolean groupActiveOnly = false;
                    String routeId = "mllp-receiver";
                    mllpClient.setMllpHost("localhost");
                    mllpClient.setMllpPort(AvailablePortFinder.getNextAvailable());

                    onCompletion()
                            .log(LoggingLevel.INFO, routeId, "Test route complete");

                    fromF("mllp://%s:%d",
                            mllpClient.getMllpHost(), mllpClient.getMllpPort())
                                    .routeId(routeId)
                                    .process(new PassthroughProcessor("Before send to result"))
                                    .to(result)
                                    .toF("log://%s?level=INFO&groupInterval=%d&groupActiveOnly=%b", routeId, groupInterval,
                                            groupActiveOnly)
                                    .log(LoggingLevel.DEBUG, routeId, "Test route received message");

                }
            };
        }
    }
    
   

}
