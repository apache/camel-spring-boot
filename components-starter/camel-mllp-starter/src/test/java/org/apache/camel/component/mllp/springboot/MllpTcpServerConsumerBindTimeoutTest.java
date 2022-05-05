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



import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
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


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MllpTcpServerConsumerBindTimeoutTest.class,
        MllpTcpServerConsumerBindTimeoutTest.TestConfiguration.class
    }
)
public class MllpTcpServerConsumerBindTimeoutTest {
    
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
                ((DefaultCamelContext)context).setName("MllpTcpServerConsumerBindTimeoutTest");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // TODO Auto-generated method stub

            }
        };
    }

    
    @Test
    public void testReceiveSingleMessage() throws Exception {
        result.expectedMessageCount(1);

        Thread tmpThread = new Thread() {
            public void run() {
                try {
                    ServerSocket tmpSocket = new ServerSocket(mllpClient.getMllpPort());
                    Thread.sleep(15000);
                    tmpSocket.close();
                } catch (Exception ex) {
                    throw new RuntimeCamelException("Exception caught in dummy listener", ex);
                }
            }

        };

        tmpThread.start();

        

        mllpClient.connect();

        mllpClient.sendMessageAndWaitForAcknowledgement(Hl7TestMessageGenerator.generateMessage(), 10000);

        MockEndpoint.assertIsSatisfied(10, TimeUnit.SECONDS);
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
                    mllpClient.setMllpHost("localhost");
                    mllpClient.setMllpPort(AvailablePortFinder.getNextAvailable());
                    int connectTimeout = 500;
                    int responseTimeout = 5000;
                    String routeId = "mllp-test-receiver-route";

                    onCompletion()
                            .toF("log:%s?level=INFO&showAll=true", routeId)
                            .log(LoggingLevel.INFO, routeId, "Test route complete");

                    fromF("mllp://%s:%d?autoAck=true&connectTimeout=%d&receiveTimeout=%d",
                            mllpClient.getMllpHost(), mllpClient.getMllpPort(), connectTimeout, responseTimeout)
                                    .routeId(routeId)
                                    .log(LoggingLevel.INFO, routeId, "Test route received message")
                                    .to(result);

                }
            };
        }
    }
    
   

}
