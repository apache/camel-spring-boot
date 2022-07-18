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



import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.springboot.rule.MllpClientResource;
import org.apache.camel.component.mllp.springboot.rule.MllpJUnitResourceException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MllpMaxConcurrentConsumersTest.class
    }
)
public class MllpMaxConcurrentConsumersTest {
    
    @RegisterExtension
    public static MllpClientResource mllpClient = new MllpClientResource();

    @RegisterExtension
    public static MllpClientResource mllpClient2 = new MllpClientResource();

    @EndpointInject(value = "mock://result")
    MockEndpoint result;

    

    @Autowired
    ProducerTemplate template;
    
    @BeforeEach
    protected void doPreSetup() throws Exception {
        mllpClient.setMllpHost("localhost");
        mllpClient.setMllpPort(AvailablePortFinder.getNextAvailable());

        mllpClient2.setMllpHost("localhost");
        mllpClient2.setMllpPort(AvailablePortFinder.getNextAvailable());

        
    }
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.setUseMDCLogging(true);
                ((DefaultCamelContext)context).setName("MllpMaxConcurrentConsumersTest");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // TODO Auto-generated method stub

            }
        };
    }

    @Autowired
    CamelContext context;
    
    @Test
    public void testConcurrentConsumersLessThanMaxConsumers() throws Exception {
        MockEndpoint.resetMocks(context);
        mllpClient.close();
        addTestRoute(2);
        result.expectedMessageCount(1);

        mllpClient.connect();

        String testMessage = "MSH|^~\\&|ADT|EPIC|JCAPS|CC|20160902123950|RISTECH|ADT^A08|00001|D|2.3|||||||" + '\r' + '\n';
        mllpClient.sendMessageAndWaitForAcknowledgement(testMessage, 10000);

        MockEndpoint.assertIsSatisfied(10, TimeUnit.SECONDS);
        context.removeRoute("mllp-max-concurrent-consumers-route");
        mllpClient.setMllpPort(-1);
        mllpClient2.setMllpPort(-1);
    }

    @Test
    public void testConcurrentConsumersMoreThanMaxConsumers() throws Exception {
        MockEndpoint.resetMocks(context);
        mllpClient.close();
        addTestRoute(1);
        result.expectedMessageCount(1);

        mllpClient.connect();

        String testMessage = "MSH|^~\\&|ADT|EPIC|JCAPS|CC|20160902123950|RISTECH|ADT^A08|00001|D|2.3|||||||" + '\r' + '\n';
        mllpClient.sendMessageAndWaitForAcknowledgement(testMessage, 10000);

        MockEndpoint.assertIsSatisfied(10, TimeUnit.SECONDS);

        // second connection should fail
        assertThrows(MllpJUnitResourceException.class,
                () -> mllpClient2.connect());
        context.removeRoute("mllp-max-concurrent-consumers-route");
        mllpClient.setMllpPort(-1);
        mllpClient2.setMllpPort(-1);
    }

    void addTestRoute(int maxConcurrentConsumers) throws Exception {
        RouteBuilder builder = new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                
                String routeId = "mllp-max-concurrent-consumers-route";

                fromF("mllp://%s:%d?maxConcurrentConsumers=%d&autoAck=true&connectTimeout=100&receiveTimeout=1000",
                        mllpClient.getMllpHost(), mllpClient.getMllpPort(), maxConcurrentConsumers)
                                .routeId(routeId)
                                .log(LoggingLevel.INFO, routeId, "Test route received message")
                                .to(result);

            }
        };
        context.addRoutes(builder);
        context.start();
    }

}
