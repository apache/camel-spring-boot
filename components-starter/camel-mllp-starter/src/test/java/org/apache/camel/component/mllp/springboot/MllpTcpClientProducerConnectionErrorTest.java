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



import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.MllpAcknowledgementException;
import org.apache.camel.component.mllp.MllpWriteException;
import org.apache.camel.component.mllp.springboot.rule.MllpServerResource;
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
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MllpTcpClientProducerConnectionErrorTest.class,
        MllpTcpClientProducerConnectionErrorTest.TestConfiguration.class
    }
)
public class MllpTcpClientProducerConnectionErrorTest {
    
    @RegisterExtension
    public static MllpServerResource mllpServer = new MllpServerResource("localhost", AvailablePortFinder.getNextAvailable());

    @EndpointInject("direct://source")
    ProducerTemplate source;

    @EndpointInject("mock://target")
    MockEndpoint target;

    @EndpointInject("mock://complete")
    MockEndpoint complete;

    @EndpointInject("mock://write-ex")
    MockEndpoint writeEx;

    @EndpointInject("mock://connect-ex")
    MockEndpoint connectEx;

    @EndpointInject("mock://acknowledgement-ex")
    MockEndpoint acknowledgementEx;

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
                ((DefaultCamelContext)context).setName("MllpTcpClientProducerConnectionErrorTest");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // TODO Auto-generated method stub

            }
        };
    }

    
    /**
     * The component should reconnect, so the route shouldn't see any errors.
     *
     * @throws Exception
     */
    @Test
    public void testConnectionClosedBeforeSendingHL7Message() throws Exception {
        MockEndpoint.resetMocks(context);
        target.expectedMessageCount(2);
        complete.expectedMessageCount(2);
        connectEx.expectedMessageCount(0);
        writeEx.expectedMessageCount(0);
        acknowledgementEx.expectedMessageCount(0);

        NotifyBuilder oneDone = new NotifyBuilder(context).whenCompleted(1).create();
        NotifyBuilder twoDone = new NotifyBuilder(context).whenCompleted(2).create();

        // Need to send one message to get the connection established
        source.sendBody(Hl7TestMessageGenerator.generateMessage());
        assertTrue(oneDone.matches(5, TimeUnit.SECONDS), "Should have completed an exchange");

        mllpServer.closeClientConnections();

        source.sendBody(Hl7TestMessageGenerator.generateMessage());

        assertTrue(twoDone.matches(5, TimeUnit.SECONDS), "Should have completed two exchanges");

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS);
    }

    /**
     * The component should reconnect, so the route shouldn't see any errors.
     *
     * @throws Exception
     */
    @Test()
    public void testConnectionResetBeforeSendingHL7Message() throws Exception {
        MockEndpoint.resetMocks(context);
        target.expectedMessageCount(2);
        complete.expectedMessageCount(2);
        connectEx.expectedMessageCount(0);
        writeEx.expectedMessageCount(0);
        acknowledgementEx.expectedMessageCount(0);

        NotifyBuilder oneDone = new NotifyBuilder(context).whenCompleted(1).create();
        NotifyBuilder twoDone = new NotifyBuilder(context).whenCompleted(2).create();

        // Need to send one message to get the connection established
        source.sendBody(Hl7TestMessageGenerator.generateMessage());
        assertTrue(oneDone.matches(5, TimeUnit.SECONDS), "Should have completed an exchange");

        mllpServer.resetClientConnections();

        source.sendBody(Hl7TestMessageGenerator.generateMessage());
        assertTrue(twoDone.matches(5, TimeUnit.SECONDS), "Should have completed two exchanges");

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS);
    }

    @Test()
    public void testConnectionClosedBeforeReadingAcknowledgement() throws Exception {
        MockEndpoint.resetMocks(context);
        target.expectedMessageCount(0);
        complete.expectedMessageCount(1);
        connectEx.expectedMessageCount(0);
        writeEx.expectedMessageCount(0);
        acknowledgementEx.expectedMessageCount(1);

        mllpServer.setCloseSocketBeforeAcknowledgementModulus(1);

        NotifyBuilder done = new NotifyBuilder(context).whenCompleted(1).create();

        source.sendBody(Hl7TestMessageGenerator.generateMessage());

        assertTrue(done.matches(5, TimeUnit.SECONDS), "Should have completed an exchange");

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS);
    }

    @Test()
    public void testConnectionResetBeforeReadingAcknowledgement() throws Exception {
        MockEndpoint.resetMocks(context);
        target.expectedMessageCount(0);
        complete.expectedMessageCount(1);
        connectEx.expectedMessageCount(0);
        writeEx.expectedMessageCount(0);
        acknowledgementEx.expectedMessageCount(1);

        mllpServer.setResetSocketBeforeAcknowledgementModulus(1);

        NotifyBuilder done = new NotifyBuilder(context).whenCompleted(1).create();

        source.sendBody(Hl7TestMessageGenerator.generateMessage());

        assertTrue(done.matches(5, TimeUnit.SECONDS), "Should have completed an exchange");

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS);
    }

    @Test
    public void testServerShutdownBeforeSendingHL7Message() throws Exception {
        MockEndpoint.resetMocks(context);
        target.expectedMessageCount(1);
        complete.expectedMessageCount(2);
        connectEx.expectedMessageCount(0);

        NotifyBuilder done = new NotifyBuilder(context).whenCompleted(2).create();

        // Need to send one message to get the connection established
        source.sendBody(Hl7TestMessageGenerator.generateMessage());

        mllpServer.shutdown();

        source.sendBody(Hl7TestMessageGenerator.generateMessage());

        assertTrue(done.matches(5, TimeUnit.SECONDS), "Should have completed an exchange");

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS);

        // Depending on the timing, either a write or a receive exception will be thrown
        assertEquals(1, writeEx.getExchanges().size() + acknowledgementEx.getExchanges().size(),
                "Either a write or a receive exception should have been be thrown");
    }

    @Test()
    public void testConnectionCloseAndServerShutdownBeforeSendingHL7Message() throws Exception {
        MockEndpoint.resetMocks(context);
        target.expectedMessageCount(1);
        complete.expectedMessageCount(2);
        connectEx.expectedMessageCount(0);

        NotifyBuilder done = new NotifyBuilder(context).whenCompleted(2).create();

        // Need to send one message to get the connection established
        source.sendBody(Hl7TestMessageGenerator.generateMessage());

        mllpServer.closeClientConnections();
        mllpServer.shutdown();

        source.sendBody(Hl7TestMessageGenerator.generateMessage());

        assertTrue(done.matches(5, TimeUnit.SECONDS), "Should have completed an exchange");

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS);

        // Depending on the timing, either a write or a receive exception will be thrown
        assertEquals(1, writeEx.getExchanges().size() + acknowledgementEx.getExchanges().size(),
                "Either a write or a receive exception should have been be thrown");
    }

    @Test()
    public void testConnectionResetAndServerShutdownBeforeSendingHL7Message() throws Exception {
        MockEndpoint.resetMocks(context);
        target.expectedMessageCount(1);
        complete.expectedMessageCount(2);
        connectEx.expectedMessageCount(0);
        writeEx.expectedMessageCount(1);
        acknowledgementEx.expectedMessageCount(0);

        NotifyBuilder done = new NotifyBuilder(context).whenCompleted(2).create();

        // Need to send one message to get the connection established
        source.sendBody(Hl7TestMessageGenerator.generateMessage());

        mllpServer.resetClientConnections();
        mllpServer.shutdown();

        source.sendBody(Hl7TestMessageGenerator.generateMessage());

        assertTrue(done.matches(5, TimeUnit.SECONDS), "Should have completed an exchange");

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS);
    }

    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                String routeId = "mllp-sender";
                @Override
                public void configure() {
                    onCompletion()
                            .to(complete);

                    onException(ConnectException.class)
                            .handled(true)
                            .to(connectEx)
                            .log(LoggingLevel.ERROR, routeId, "Connect Error")
                            .stop();

                    onException(MllpWriteException.class)
                            .handled(true)
                            .to(writeEx)
                            .log(LoggingLevel.ERROR, routeId, "Write Error")
                            .stop();

                    onException(MllpAcknowledgementException.class)
                            .handled(true)
                            .to(acknowledgementEx)
                            .log(LoggingLevel.ERROR, routeId, "Acknowledgement Error")
                            .stop();

                    from(source.getDefaultEndpoint()).routeId(routeId)
                            .log(LoggingLevel.INFO, routeId, "Sending Message")
                            .toF("mllp://%s:%d", mllpServer.getListenHost(), mllpServer.getListenPort())
                            .log(LoggingLevel.INFO, routeId, "Received Acknowledgement")
                            .to(target);
                }
            };
        }
    }
    
   

}
