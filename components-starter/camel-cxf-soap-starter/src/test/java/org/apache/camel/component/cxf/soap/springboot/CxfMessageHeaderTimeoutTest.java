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
package org.apache.camel.component.cxf.soap.springboot;




import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Endpoint;


import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.GreeterImplWithSleep;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hello_world_soap_http.Greeter;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfMessageHeaderTimeoutTest.class,
        CxfMessageHeaderTimeoutTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfMessageHeaderTimeoutTest {
    
    
    protected static final String GREET_ME_OPERATION = "greetMe";
    protected static final String TEST_MESSAGE = "Hello World!";
    protected static final String SERVER_ADDRESS
            = "/CxfMessageHeaderTimeoutTest/SoapContext/SoapPort";

    
    
    static int port = CXFTestSupport.getPort1();

    private Endpoint endpoint;
    
    @BeforeEach
    public void startService() {
        Greeter implementor = new GreeterImplWithSleep();
        endpoint = Endpoint.publish(SERVER_ADDRESS, implementor);
    }
    
    @AfterEach
    public void stopService() {
        endpoint.stop();
    }
    
    
    @Autowired
    ProducerTemplate template;
    
    @Test
    public void testInvokingJaxWsServerWithCxfEndpoint() throws Exception {
        sendTimeOutMessage("cxf://bean:springEndpoint");
    }

    protected void sendTimeOutMessage(String endpointUri) throws Exception {
        Exchange reply = sendJaxWsMessage(endpointUri);
        Exception e = reply.getException();
        assertNotNull(e, "We should get the exception cause here");
        assertTrue(e instanceof SocketTimeoutException, "We should get the socket time out exception here");
    }

    protected Exchange sendJaxWsMessage(String endpointUri) throws InterruptedException {
        Exchange exchange = template.send(endpointUri, new Processor() {
            public void process(final Exchange exchange) {
                final List<String> params = new ArrayList<>();
                params.add(TEST_MESSAGE);
                exchange.getIn().setBody(params);
                exchange.getIn().setHeader(CxfConstants.OPERATION_NAME, GREET_ME_OPERATION);
                // setup the receive timeout dynamically
                Map<String, Object> requestContext = new HashMap<>();
                HTTPClientPolicy clientPolicy = new HTTPClientPolicy();
                clientPolicy.setReceiveTimeout(100);
                requestContext.put(HTTPClientPolicy.class.getName(), clientPolicy);
                exchange.getIn().setBody(params);
                exchange.getIn().setHeader(Client.REQUEST_CONTEXT, requestContext);

            }
        });
        return exchange;
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
                public void configure() {
                    errorHandler(noErrorHandler());
                    from("direct:start").to("cxf:bean:springEndpoint");
                }
            };
        }
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            return new UndertowServletWebServerFactory(port);
        }
        
        @Bean
        public CxfEndpoint springEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress("http://localhost:" + port 
                                   + "/services" + SERVER_ADDRESS);
            cxfEndpoint.setServiceClass(org.apache.hello_world_soap_http.Greeter.class);
            return cxfEndpoint;
        }
    }
    
    
}
