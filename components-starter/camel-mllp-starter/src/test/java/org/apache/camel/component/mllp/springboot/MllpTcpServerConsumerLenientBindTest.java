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


import static org.apache.camel.test.junit5.TestSupport.assertIsInstanceOf;
import static org.apache.camel.test.junit5.TestSupport.assertStringContains;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.springboot.rule.MllpClientResource;
import org.apache.camel.component.mllp.springboot.rule.MllpJUnitResourceTimeoutException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        MllpTcpServerConsumerLenientBindTest.class,
        MllpTcpServerConsumerLenientBindTest.TestConfiguration.class
    }
)
public class MllpTcpServerConsumerLenientBindTest {
    
    static final int RECEIVE_TIMEOUT = 1000;
    static final int READ_TIMEOUT = 500;

    @RegisterExtension
    public static MllpClientResource mllpClient = new MllpClientResource();

    @EndpointInject("mock://result")
    MockEndpoint result;

    static ServerSocket portBlocker;
    

    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.setUseMDCLogging(true);
                ((DefaultCamelContext)context).setName("MllpTcpServerConsumerLenientBindTest");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // TODO Auto-generated method stub

            }
        };
    }

    
    @Test
    public void testLenientBind() throws Exception {
        assertEquals(ServiceStatus.Started, context.getStatus());

        mllpClient.connect();
        try {
            mllpClient.sendMessageAndWaitForAcknowledgement(Hl7TestMessageGenerator.generateMessage(10001));
        } catch (MllpJUnitResourceTimeoutException expectedEx) {
            assertIsInstanceOf(SocketTimeoutException.class, expectedEx.getCause());
        }
        
        mllpClient.reset();

        portBlocker.close();
        Thread.sleep(2000);
        assertEquals(ServiceStatus.Started, context.getStatus());

        mllpClient.connect();
        String acknowledgement
                = mllpClient.sendMessageAndWaitForAcknowledgement(Hl7TestMessageGenerator.generateMessage(10002));
        assertStringContains(acknowledgement, "10002");
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                
                String routeId = "mllp-receiver-with-lenient-bind";
                @Override
                public void configure() throws IOException {
                    mllpClient.setMllpHost("localhost");
                    mllpClient.setMllpPort(AvailablePortFinder.getNextAvailable());

                    portBlocker = new ServerSocket(mllpClient.getMllpPort());

                    assertTrue(portBlocker.isBound());
                    fromF("mllp://%s:%d?bindTimeout=15000&bindRetryInterval=500&receiveTimeout=%d&readTimeout=%d&reuseAddress=false&lenientBind=true",
                            mllpClient.getMllpHost(), mllpClient.getMllpPort(), RECEIVE_TIMEOUT, READ_TIMEOUT)
                                    .routeId(routeId)
                                    .log(LoggingLevel.INFO, routeId, "Receiving: ${body}")
                                    .to(result);
                }
            };
        }
    }
    
   

}
