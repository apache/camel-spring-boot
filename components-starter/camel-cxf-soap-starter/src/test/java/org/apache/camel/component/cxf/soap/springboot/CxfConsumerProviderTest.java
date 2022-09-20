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


import org.w3c.dom.Node;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfConsumerProviderTest.class,
        CxfConsumerProviderTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfConsumerProviderTest {

    protected static final String REQUEST_MESSAGE
        = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"test/service\">"
            + "<soapenv:Header/><soapenv:Body><ser:ping/></soapenv:Body></soapenv:Envelope>";

    protected static final String RESPONSE_MESSAGE_BEGINE
        = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><pong xmlns=\"test/service\"";
    protected static final String RESPONSE_MESSAGE_END = "/></soap:Body></soap:Envelope>";

    protected static final String RESPONSE = "<pong xmlns=\"test/service\"/>";

    protected final String relativeAddress = "/" + getClass().getSimpleName() + "/test";
    
    protected final String simpleEndpointAddress = "http://localhost:" + port + "/services"
                                            + relativeAddress;
    protected final String simpleEndpointURI = "cxf://" + relativeAddress
                                       + "?serviceClass=org.apache.camel.component.cxf.ServiceProvider";

    static int port = CXFTestSupport.getPort1();

    @Autowired
    ProducerTemplate template;
    
    
    
    @Test
    public void testInvokingServiceFromHttpCompnent() throws Exception {
        // call the service with right post message

        String response = template.requestBody(simpleEndpointAddress, REQUEST_MESSAGE, String.class);
        assertTrue(response.startsWith(RESPONSE_MESSAGE_BEGINE), "Get a wrong response");
        assertTrue(response.endsWith(RESPONSE_MESSAGE_END), "Get a wrong response");
        try {
            template.requestBody(simpleEndpointAddress, null, String.class);
            fail("Excpetion to get exception here");
        } catch (Exception ex) {
            // do nothing here
        }

        response = template.requestBody(simpleEndpointAddress, REQUEST_MESSAGE, String.class);
        assertTrue(response.startsWith(RESPONSE_MESSAGE_BEGINE), "Get a wrong response");
        assertTrue(response.endsWith(RESPONSE_MESSAGE_END), "Get a wrong response");
    }

    
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            return new UndertowServletWebServerFactory(port);
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    errorHandler(noErrorHandler());
                    from(getFromEndpointUri()).process(new Processor() {
                        public void process(final Exchange exchange) {
                            Message in = exchange.getIn();
                            Node node = in.getBody(Node.class);
                            assertNotNull(node);
                            XmlConverter xmlConverter = new XmlConverter();
                            // Put the result back
                            exchange.getMessage().setBody(xmlConverter.toSource(RESPONSE));
                        }
                    });
                }
            };
        }
    }
    
    protected String getFromEndpointUri() {
        return simpleEndpointURI;
    }

}
