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

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
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
        MllpProducerConsumerLoopbackInOnlyTest.class,
        MllpProducerConsumerLoopbackInOnlyTest.TestConfiguration.class
    }
)
public class MllpProducerConsumerLoopbackInOnlyTest {
    
    @RegisterExtension
    public static MllpClientResource mllpClient = new MllpClientResource();
    
    @EndpointInject("direct://source")
    ProducerTemplate source;

    @EndpointInject("mock://received-and-processed")
    MockEndpoint receivedAndProcessed;

    

    @Autowired
    ProducerTemplate template;
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.setUseMDCLogging(true);
                ((DefaultCamelContext)context).setName("MllpProducerConsumerLoopbackInOnlyTest");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // TODO Auto-generated method stub

            }
        };
    }

    
    @Test
    public void testLoopbackWithOneMessage() throws Exception {
        String testMessage = Hl7TestMessageGenerator.generateMessage();
        receivedAndProcessed.expectedBodiesReceived(testMessage);

        String acknowledgement = source.requestBody((Object) testMessage, String.class);
        assertThat("Should receive no acknowledgment for message 1", acknowledgement, CoreMatchers.nullValue());

        MockEndpoint.assertIsSatisfied(60, TimeUnit.SECONDS);
    }
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        String mllpHost = "localhost";
        int mllpPort = AvailablePortFinder.getNextAvailable();

        @Bean
        public RouteBuilder routeBuilder1() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    fromF("mllp://%s:%d?autoAck=false&exchangePattern=InOnly", mllpHost, mllpPort)
                            .convertBodyTo(String.class)
                            .to(receivedAndProcessed);
                }
            };
        }
        
        @Bean
        public RouteBuilder routeBuilder2() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(source.getDefaultEndpoint())
                            .toF("mllp://%s:%d?exchangePattern=InOnly", mllpHost, mllpPort)
                            .setBody(header(MllpConstants.MLLP_ACKNOWLEDGEMENT));
                }
            };
        }
    }
    
   

}
