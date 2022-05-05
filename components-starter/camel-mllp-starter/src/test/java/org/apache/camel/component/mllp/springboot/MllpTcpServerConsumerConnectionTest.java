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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.springboot.rule.MllpClientResource;
import org.apache.camel.component.mllp.springboot.rule.MllpJUnitResourceException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        MllpTcpServerConsumerConnectionTest.class
    }
)
public class MllpTcpServerConsumerConnectionTest {
    
    static final int RECEIVE_TIMEOUT = 1000;
    static final int READ_TIMEOUT = 500;

    
    @RegisterExtension
    public static MllpClientResource mllpClient = new MllpClientResource();

    @EndpointInject("mock://result")
    MockEndpoint result;

    

    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    /**
     * Simulate a Load Balancer Probe
     * <p/>
     * Load Balancers check the status of a port by establishing and closing a TCP connection periodically. The time
     * between these probes can normally be configured, but it is typically set to about 15-sec. Since there could be a
     * large number of port that are being probed, the logging from the connect/disconnect operations can drown-out more
     * useful information.
     * <p/>
     * Watch the logs when running this test to verify that the log output will be acceptable when a load balancer is
     * probing the port.
     * <p/>
     * TODO: Need to add a custom Log4j Appender that can verify the logging is acceptable
     *
     * @throws Exception
     */
    @Test
    public void testConnectThenCloseWithoutData() throws Exception {
        result.reset();
        int connectionCount = 10;
        long connectionMillis = 200;

        result.setExpectedCount(0);
        result.setAssertPeriod(1000);

        addTestRouteWithIdleTimeout(-1);

        for (int i = 1; i <= connectionCount; ++i) {
            mllpClient.connect();
            Thread.sleep(connectionMillis);
            mllpClient.close();
        }

        // Connect one more time and allow a client thread to start
        mllpClient.connect();
        Thread.sleep(1000);
        mllpClient.close();

        result.assertIsSatisfied(15000);
    }

    @Test
    public void testConnectThenResetWithoutData() throws Exception {
        result.reset();
        int connectionCount = 10;
        long connectionMillis = 200;

        result.setExpectedCount(0);
        result.setAssertPeriod(1000);

        addTestRouteWithIdleTimeout(-1);

        for (int i = 1; i <= connectionCount; ++i) {
            mllpClient.connect();
            Thread.sleep(connectionMillis);
            mllpClient.reset();
        }

        // Connect one more time and allow a client thread to start
        mllpClient.connect();
        Thread.sleep(1000);
        mllpClient.reset();

        result.assertIsSatisfied(15000);
    }

    /**
     * Simulate an Idle Client
     *
     * @throws Exception
     */
    @Test
    public void testIdleConnection() throws Exception {
        result.reset();
        final int idleTimeout = RECEIVE_TIMEOUT * 3;
        String testMessage = "MSH|^~\\&|ADT|EPIC|JCAPS|CC|20160902123950|RISTECH|ADT^A08|00001|D|2.3|||||||" + '\r' + '\n';

        result.setExpectedCount(1);
        result.setAssertPeriod(1000);

        addTestRouteWithIdleTimeout(idleTimeout);

        mllpClient.connect();
        mllpClient.sendMessageAndWaitForAcknowledgement(testMessage);
        Thread.sleep(idleTimeout + RECEIVE_TIMEOUT);

        try {
            mllpClient.checkConnection();
            fail("The MllpClientResource should have thrown an exception when writing to the reset socket");
        } catch (MllpJUnitResourceException expectedEx) {
            assertEquals("checkConnection failed - read() returned END_OF_STREAM", expectedEx.getMessage());
            assertNull(expectedEx.getCause());
        }

        
    }

    void addTestRouteWithIdleTimeout(final int idleTimeout) throws Exception {
        RouteBuilder builder = new RouteBuilder() {
            String routeId = "mllp-receiver-with-timeout";

            public void configure() {
                mllpClient.setMllpHost("localhost");
                mllpClient.setMllpPort(AvailablePortFinder.getNextAvailable());
                fromF("mllp://%s:%d?receiveTimeout=%d&readTimeout=%d&idleTimeout=%d", mllpClient.getMllpHost(),
                        mllpClient.getMllpPort(), RECEIVE_TIMEOUT, READ_TIMEOUT, idleTimeout)
                                .routeId(routeId)
                                .log(LoggingLevel.INFO, routeId, "Receiving: ${body}")
                                .to(result);
            }
        };

        context.addRoutes(builder);
        context.start();
    }


}
