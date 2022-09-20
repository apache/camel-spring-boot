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

import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CxfPayload;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfConsumerPayloadTest.class,
                           CxfConsumerPayloadTest.TestConfiguration.class,
                           CxfConsumerMessageTest.ServletConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfConsumerPayloadTest extends CxfConsumerMessageTest {

    protected static final String ECHO_RESPONSE = "<ns1:echoResponse xmlns:ns1=\"http://cxf.component.camel.apache.org/\">"
                                                  + "<return xmlns=\"http://cxf.component.camel.apache.org/\">echo Hello World!</return>"
                                                  + "</ns1:echoResponse>";
    protected static final String ECHO_BOOLEAN_RESPONSE = "<ns1:echoBooleanResponse xmlns:ns1=\"http://cxf.component.camel.apache.org/\">"
                                                          + "<return xmlns=\"http://cxf.component.camel.apache.org/\">true</return>"
                                                          + "</ns1:echoBooleanResponse>";
    protected static final String ECHO_REQUEST = "<ns1:echo xmlns:ns1=\"http://cxf.component.camel.apache.org/\">"
                                                 + "<arg0 xmlns=\"http://cxf.component.camel.apache.org/\">Hello World!</arg0></ns1:echo>";
    protected static final String ECHO_BOOLEAN_REQUEST = "<ns1:echoBoolean xmlns:ns1=\"http://cxf.component.camel.apache.org/\">"
                                                         + "<arg0 xmlns=\"http://cxf.component.camel.apache.org/\">true</arg0></ns1:echoBoolean>";

    protected static final String ELEMENT_NAMESPACE = "http://cxf.component.camel.apache.org/";
    
   
    protected void checkRequest(String expect, String request) {

        if (expect.equals("ECHO_REQUEST")) {
            assertTrue(request.startsWith(ECHO_REQUEST.substring(0, 60))
                       && request.endsWith(ECHO_REQUEST.substring(61)), "Get a wrong request");
        } else {
            assertTrue(request.startsWith(ECHO_BOOLEAN_REQUEST.substring(0, 67))
                       && request.endsWith(ECHO_BOOLEAN_REQUEST.substring(68)), "Get a wrong request");
        }
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
                    from(simpleEndpointURI + "&dataFormat=PAYLOAD").to("log:info").process(new Processor() {
                        @SuppressWarnings("unchecked")
                        public void process(final Exchange exchange) throws Exception {
                            CxfPayload<SoapHeader> requestPayload = exchange.getIn()
                                .getBody(CxfPayload.class);
                            List<Source> inElements = requestPayload.getBodySources();
                            List<Source> outElements = new ArrayList<>();
                            // You can use a customer toStringConverter to turn a CxfPayLoad message into
                            // String as you want
                            String request = exchange.getIn().getBody(String.class);
                            XmlConverter converter = new XmlConverter();
                            String documentString = ECHO_RESPONSE;

                            Element in = new XmlConverter().toDOMElement(inElements.get(0));
                            // Just check the element namespace
                            if (!in.getNamespaceURI().equals(ELEMENT_NAMESPACE)) {
                                throw new IllegalArgumentException("Wrong element namespace");
                            }
                            if (in.getLocalName().equals("echoBoolean")) {
                                documentString = ECHO_BOOLEAN_RESPONSE;
                                checkRequest("ECHO_BOOLEAN_REQUEST", request);
                            } else {
                                documentString = ECHO_RESPONSE;
                                checkRequest("ECHO_REQUEST", request);
                            }
                            Document outDocument = converter.toDOMDocument(documentString, exchange);
                            outElements.add(new DOMSource(outDocument.getDocumentElement()));
                            // set the payload header with null
                            CxfPayload<SoapHeader> responsePayload = new CxfPayload<>(null, outElements,
                                                                                      null);
                            exchange.getMessage().setBody(responsePayload);
                        }
                    });
                }
            };
        }
    }

}
