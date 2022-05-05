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



import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.MllpConstants;
import org.apache.camel.component.mllp.springboot.rule.MllpClientResource;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MllpTcpServerConsumerMessageHeadersTest.class,
        MllpTcpServerConsumerMessageHeadersTest.TestConfiguration.class
    }
)
public class MllpTcpServerConsumerMessageHeadersTest {
    
    @RegisterExtension
    public static MllpClientResource mllpClient1 = new MllpClientResource();
    @RegisterExtension
    public static MllpClientResource mllpClient2 = new MllpClientResource();

    @EndpointInject("mock://result")
    MockEndpoint result;
    
    @EndpointInject("mock://on-completion-result")
    MockEndpoint onCompletionResult;

    

    @Autowired
    ProducerTemplate template;
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.setUseMDCLogging(true);
                ((DefaultCamelContext)context).setName("MllpTcpServerConsumerMessageHeadersTest");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // TODO Auto-generated method stub

            }
        };
    }

    
    @Test
    public void testHl7HeadersEnabled() throws Exception {
        String testMessage = "MSH|^~\\&|ADT|EPIC|JCAPS|CC|20160902123950|RISTECH|ADT^A08|00001|D|2.3|||||||" + '\r' + '\n';

        result.reset();

        result.expectedMessageCount(1);

        result.expectedHeaderReceived(MllpConstants.MLLP_SENDING_APPLICATION, "ADT");
        result.expectedHeaderReceived(MllpConstants.MLLP_SENDING_FACILITY, "EPIC");
        result.expectedHeaderReceived(MllpConstants.MLLP_RECEIVING_APPLICATION, "JCAPS");
        result.expectedHeaderReceived(MllpConstants.MLLP_TIMESTAMP, "20160902123950");
        result.expectedHeaderReceived(MllpConstants.MLLP_SECURITY, "RISTECH");
        result.expectedHeaderReceived(MllpConstants.MLLP_MESSAGE_TYPE, "ADT^A08");
        result.expectedHeaderReceived(MllpConstants.MLLP_EVENT_TYPE, "ADT");
        result.expectedHeaderReceived(MllpConstants.MLLP_TRIGGER_EVENT, "A08");
        result.expectedHeaderReceived(MllpConstants.MLLP_MESSAGE_CONTROL, "00001");
        result.expectedHeaderReceived(MllpConstants.MLLP_PROCESSING_ID, "D");
        result.expectedHeaderReceived(MllpConstants.MLLP_VERSION_ID, "2.3");

        mllpClient1.connect();

        mllpClient1.sendMessageAndWaitForAcknowledgement(testMessage, 10000);

        result.assertIsSatisfied(10000);

        Message message = result.getExchanges().get(0).getIn();

        assertNotNull(message.getHeader(MllpConstants.MLLP_LOCAL_ADDRESS),
                "Should have header" + MllpConstants.MLLP_LOCAL_ADDRESS);
        assertNotNull(message.getHeader(MllpConstants.MLLP_REMOTE_ADDRESS),
                "Should have header" + MllpConstants.MLLP_REMOTE_ADDRESS);
    }

    @Test
    public void testHl7HeadersDisabled() throws Exception {
        String testMessage = "MSH|^~\\&|ADT|EPIC|JCAPS|CC|20160902123950|RISTECH|ADT^A08|00001|D|2.3|||||||" + '\r' + '\n';
        result.reset();
        result.expectedMessageCount(1);

        mllpClient2.connect();

        mllpClient2.sendMessageAndWaitForAcknowledgement(testMessage, 10000);

        result.assertIsSatisfied(10000);

        Message message = result.getExchanges().get(0).getIn();

        assertNotNull(message.getHeader(MllpConstants.MLLP_LOCAL_ADDRESS),
                "Should have header" + MllpConstants.MLLP_LOCAL_ADDRESS);
        assertNotNull(message.getHeader(MllpConstants.MLLP_REMOTE_ADDRESS),
                "Should have header" + MllpConstants.MLLP_REMOTE_ADDRESS);

        assertNull(message.getHeader(MllpConstants.MLLP_SENDING_APPLICATION),
                "Should NOT have header" + MllpConstants.MLLP_SENDING_APPLICATION);
        assertNull(message.getHeader(MllpConstants.MLLP_SENDING_FACILITY),
                "Should NOT have header" + MllpConstants.MLLP_SENDING_FACILITY);
        assertNull(message.getHeader(MllpConstants.MLLP_RECEIVING_APPLICATION),
                "Should NOT have header" + MllpConstants.MLLP_RECEIVING_APPLICATION);
        assertNull(message.getHeader(MllpConstants.MLLP_TIMESTAMP), "Should NOT have header" + MllpConstants.MLLP_TIMESTAMP);
        assertNull(message.getHeader(MllpConstants.MLLP_SECURITY), "Should NOT have header" + MllpConstants.MLLP_SECURITY);
        assertNull(message.getHeader(MllpConstants.MLLP_MESSAGE_TYPE),
                "Should NOT have header" + MllpConstants.MLLP_MESSAGE_TYPE);
        assertNull(message.getHeader(MllpConstants.MLLP_EVENT_TYPE), "Should NOT have header" + MllpConstants.MLLP_EVENT_TYPE);
        assertNull(message.getHeader(MllpConstants.MLLP_MESSAGE_CONTROL),
                "Should NOT have header" + MllpConstants.MLLP_MESSAGE_CONTROL);
        assertNull(message.getHeader(MllpConstants.MLLP_PROCESSING_ID),
                "Should NOT have header" + MllpConstants.MLLP_PROCESSING_ID);
        assertNull(message.getHeader(MllpConstants.MLLP_VERSION_ID), "Should NOT have header" + MllpConstants.MLLP_VERSION_ID);
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
                    int connectTimeout = 500;
                    int responseTimeout = 5000;

                    
                    
                    mllpClient1.setMllpHost("localhost");
                    mllpClient1.setMllpPort(AvailablePortFinder.getNextAvailable());

                    
                    mllpClient2.setMllpHost("localhost");
                    mllpClient2.setMllpPort(AvailablePortFinder.getNextAvailable());

                    String routeId = "mllp-test-receiver-route";

                    onCompletion()
                            .to("mock://on-completion-result")
                            .toF("log:%s?level=INFO&showAll=true", routeId)
                            .log(LoggingLevel.INFO, routeId, "Test route complete");

                    fromF("mllp://%s:%d?autoAck=true&connectTimeout=%d&receiveTimeout=%d&hl7Headers=%b",
                            mllpClient1.getMllpHost(), mllpClient1.getMllpPort(), connectTimeout, responseTimeout, true)
                                    .routeId(routeId + 1)
                                    .log(LoggingLevel.INFO, routeId, "Test route received message")
                                    .to(result);
                    fromF("mllp://%s:%d?autoAck=true&connectTimeout=%d&receiveTimeout=%d&hl7Headers=%b",
                          mllpClient2.getMllpHost(), mllpClient2.getMllpPort(), connectTimeout, responseTimeout, false)
                                  .routeId(routeId + 2)
                                  .log(LoggingLevel.INFO, routeId, "Test route received message")
                                  .to(result);
                }
            };
        }
    }
    
   

}
