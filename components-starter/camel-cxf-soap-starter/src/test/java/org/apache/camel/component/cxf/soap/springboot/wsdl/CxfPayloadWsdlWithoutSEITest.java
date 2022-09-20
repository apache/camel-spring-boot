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
package org.apache.camel.component.cxf.soap.springboot.wsdl;

import static org.apache.camel.test.junit5.TestSupport.assertStringContains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.handler.Handler;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.wsdl_first.PersonImpl;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfPayloadWsdlWithoutSEITest.class,
                           CxfPayloadWsdlWithoutSEITest.TestConfiguration.class,
                           AbstractCxfWsdlFirstTest.ServletConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfPayloadWsdlWithoutSEITest extends AbstractCxfWsdlFirstTest {

    private QName serviceName = QName.valueOf("{http://camel.apache.org/wsdl-first}PersonService");
    private QName endpointName = QName.valueOf("{http://camel.apache.org/wsdl-first}soap");
    protected Endpoint endpoint;
    
    @BeforeEach
    public void startService() {
        Object implementor = new PersonImpl();
        String address = "/CxfPayloadWsdlWithoutSEITest/PersonService/";
        endpoint = Endpoint.publish(address, implementor);
    }

    @AfterEach
    public void stopService() {
        if (endpoint != null) {
            endpoint.stop();
        }
    }
    
    

    @Test
    @Override
    public void testInvokingServiceWithCamelProducer() {
        Exchange exchange = sendJaxWsMessage("hello");
        assertEquals(false, exchange.isFailed(), "The request should be handled sucessfully");
        org.apache.camel.Message out = exchange.getMessage();
        String result = out.getBody(String.class);
        assertStringContains(result, "Bonjour");

        exchange = sendJaxWsMessage("");
        assertEquals(true, exchange.isFailed(), "We should get a fault here");
        Throwable ex = exchange.getException();
        assertTrue(ex instanceof SoapFault, "We should get a SoapFault here");
    }

    private Exchange sendJaxWsMessage(final String personIdString) {
        Exchange exchange = template.send("direct:producer", new Processor() {
            public void process(final Exchange exchange) {
                String body = "<GetPerson xmlns=\"http://camel.apache.org/wsdl-first/types\"><personId>" + personIdString
                              + "</personId></GetPerson>\n";
                exchange.getIn().setBody(body);
                exchange.getIn().setHeader(CxfConstants.OPERATION_NAME, "GetPerson");
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
        CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(serviceName);
            cxfEndpoint.setEndpointNameAsQName(endpointName);
            cxfEndpoint.setAddress("/CxfPayloadWsdlWithoutSEITest/RouterService/");
            cxfEndpoint.setWsdlURL("classpath:person.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("schema-validation-enabled", true);
            cxfEndpoint.setProperties(properties);
            List<Handler> handlers = new ArrayList<Handler>();
            handlers.add(fromHandler);
            cxfEndpoint.setHandlers(handlers);
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(serviceName);
            cxfEndpoint.setEndpointNameAsQName(endpointName);
            cxfEndpoint.setAddress("http://localhost:"+ port 
                                   + "/services/CxfPayloadWsdlWithoutSEITest/PersonService/");
            cxfEndpoint.setWsdlURL("classpath:person.wsdl");
            List<Handler> handlers = new ArrayList<Handler>();
            handlers.add(toHandler);
            cxfEndpoint.setHandlers(handlers);
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            return cxfEndpoint;
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    errorHandler(noErrorHandler());
                    from("cxf:bean:routerEndpoint")
                            .to("cxf:bean:serviceEndpoint");
                    from("direct:producer")
                        .to("cxf:bean:serviceEndpoint");
                    
                }
            };
        }
    }

}
