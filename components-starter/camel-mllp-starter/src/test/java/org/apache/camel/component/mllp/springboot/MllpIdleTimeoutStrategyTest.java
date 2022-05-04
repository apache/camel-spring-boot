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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.MllpComponent;
import org.apache.camel.component.mllp.MllpEndpoint;
import org.apache.camel.component.mllp.MllpTcpClientProducer;
import org.apache.camel.component.mllp.internal.MllpSocketBuffer;
import org.apache.camel.component.mllp.springboot.rule.MllpServerResource;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.mockito.Mockito;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, MllpIdleTimeoutStrategyTest.class,
                           MllpIdleTimeoutStrategyTest.TestConfiguration.class
})
public class MllpIdleTimeoutStrategyTest {

    static final int IDLE_TIMEOUT = 500;

    @RegisterExtension
    public static MllpServerResource mllpServer = new MllpServerResource("localhost", AvailablePortFinder
        .getNextAvailable());

    @EndpointInject("direct://sourcedefault")
    ProducerTemplate defaultStrategySource;

    @EndpointInject("direct://sourcereset")
    ProducerTemplate resetStrategySource;

    @EndpointInject("direct://sourceclose")
    ProducerTemplate closeStrategySource;

    @EndpointInject("mock://target")
    MockEndpoint target;

    private static MllpEndpoint defaultStrategyEndpoint;

    private static MllpEndpoint resetStrategyEndpoint;

    private static MllpEndpoint closeStrategyEndpoint;

    private static MllpSocketBuffer defaultStrategyMllpSocketBuffer;

    private static MllpSocketBuffer resetStrategyMllpSocketBuffer;

    private static MllpSocketBuffer closeStrategyMllpSocketBuffer;

    private static MllpTcpClientProducer defaultStrategyProducer;

    private static MllpTcpClientProducer resetStrategyProducer;

    private static MllpTcpClientProducer closeStrategyProducer;

    @Autowired
    ProducerTemplate template;

    @Autowired
    CamelContext context;

    @BeforeEach
    public void setupMock() {
        MockEndpoint.resetMocks(context);
    }

    private void sendHl7Message(ProducerTemplate template) throws Exception {
        target.expectedMessageCount(1);
        // Need to send one message to get the connection established
        template.sendBody(Hl7TestMessageGenerator.generateMessage());
        Thread.sleep(IDLE_TIMEOUT * 3);
        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS);
    }

    @Test
    public void defaultStrategyTest() throws Exception {
        sendHl7Message(defaultStrategySource);
        verify(defaultStrategyMllpSocketBuffer, times(1)).resetSocket(any());
        verify(defaultStrategyMllpSocketBuffer, never()).closeSocket(any());
    }

    @Test
    public void resetStrategyTest() throws Exception {
        sendHl7Message(resetStrategySource);
        verify(resetStrategyMllpSocketBuffer, times(1)).resetSocket(any());
        verify(resetStrategyMllpSocketBuffer, never()).closeSocket(any());
    }

    @Test
    public void closeStrategyTest() throws Exception {
        sendHl7Message(closeStrategySource);
        verify(closeStrategyMllpSocketBuffer, times(1)).closeSocket(any());
        verify(closeStrategyMllpSocketBuffer, never()).resetSocket(any());
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
                    MllpComponent mllpComponent = context.getComponent("mllp", MllpComponent.class);
                    defaultStrategyEndpoint = Mockito.spy((MllpEndpoint)mllpComponent.createEndpoint(String
                        .format("mllp://%s:%d?idleTimeout=%d", mllpServer.getListenHost(),
                                mllpServer.getListenPort(), IDLE_TIMEOUT)));
                    resetStrategyEndpoint = Mockito.spy((MllpEndpoint)mllpComponent.createEndpoint(String
                        .format("mllp://%s:%d?idleTimeout=%d&idleTimeoutStrategy=reset",
                                mllpServer.getListenHost(), mllpServer.getListenPort(), IDLE_TIMEOUT)));

                    closeStrategyEndpoint = Mockito.spy((MllpEndpoint)mllpComponent.createEndpoint(String
                        .format("mllp://%s:%d?idleTimeout=%d&idleTimeoutStrategy=close",
                                mllpServer.getListenHost(), mllpServer.getListenPort(), IDLE_TIMEOUT)));

                    defaultStrategyProducer = Mockito.spy(new MllpTcpClientProducer(defaultStrategyEndpoint));
                    defaultStrategyProducer.start();
                    Field defaultStrategyMllpBufferField = MllpTcpClientProducer.class
                        .getDeclaredField("mllpBuffer");
                    defaultStrategyMllpBufferField.setAccessible(true);
                    defaultStrategyMllpSocketBuffer = Mockito
                        .spy(new MllpSocketBuffer(defaultStrategyEndpoint));
                    defaultStrategyMllpBufferField.set(defaultStrategyProducer,
                                                       defaultStrategyMllpSocketBuffer);
                    when(defaultStrategyEndpoint.createProducer()).thenReturn(defaultStrategyProducer);

                    resetStrategyProducer = Mockito.spy(new MllpTcpClientProducer(resetStrategyEndpoint));

                    Field resetStrategyMllpBufferField = MllpTcpClientProducer.class
                        .getDeclaredField("mllpBuffer");
                    resetStrategyMllpBufferField.setAccessible(true);
                    resetStrategyMllpSocketBuffer = Mockito.spy(new MllpSocketBuffer(resetStrategyEndpoint));
                    resetStrategyMllpBufferField.set(resetStrategyProducer, resetStrategyMllpSocketBuffer);
                    when(resetStrategyEndpoint.createProducer()).thenReturn(resetStrategyProducer);

                    closeStrategyProducer = Mockito.spy(new MllpTcpClientProducer(closeStrategyEndpoint));
                    Field closeStrategyMllpBufferField = MllpTcpClientProducer.class
                        .getDeclaredField("mllpBuffer");
                    closeStrategyMllpBufferField.setAccessible(true);
                    closeStrategyMllpSocketBuffer = Mockito.spy(new MllpSocketBuffer(closeStrategyEndpoint));
                    closeStrategyMllpBufferField.set(closeStrategyProducer, closeStrategyMllpSocketBuffer);
                    when(closeStrategyEndpoint.createProducer()).thenReturn(closeStrategyProducer);

                    defaultStrategyProducer.start();
                    resetStrategyProducer.start();
                    closeStrategyProducer.start();
                    route("mllp-sender-default", defaultStrategySource.getDefaultEndpoint(),
                          defaultStrategyEndpoint);
                    route("mllp-sender-reset", resetStrategySource.getDefaultEndpoint(),
                          resetStrategyEndpoint);
                    route("mllp-sender-close", closeStrategySource.getDefaultEndpoint(),
                          closeStrategyEndpoint);
                }

                private void route(String routeId, Endpoint sourceEndpoint, Endpoint destinationEndpoint) {
                    from(sourceEndpoint).routeId(routeId).log(LoggingLevel.INFO, routeId, "Sending Message")
                        .to(destinationEndpoint).log(LoggingLevel.INFO, routeId, "Received Acknowledgement")
                        .to(target);
                }
            };
        }
    }

}
