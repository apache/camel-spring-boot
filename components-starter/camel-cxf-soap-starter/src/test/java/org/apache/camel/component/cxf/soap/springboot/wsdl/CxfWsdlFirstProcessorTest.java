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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.handler.Handler;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.soap.springboot.JaxwsTestHandler;
import org.apache.camel.component.cxf.soap.springboot.wsdl.AbstractCxfWsdlFirstTest.ServletConfiguration;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.wsdl_first.PersonImpl;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfWsdlFirstProcessorTest.class,
                           CxfWsdlFirstProcessorTest.TestConfiguration.class,
                           AbstractCxfWsdlFirstTest.ServletConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfWsdlFirstProcessorTest extends AbstractCxfWsdlFirstTest {

    private static final Logger LOG = LoggerFactory.getLogger(CxfWsdlFirstProcessorTest.class);
    
    private QName serviceName = QName.valueOf("{http://camel.apache.org/wsdl-first}PersonService");
    private QName endpointName = QName.valueOf("{http://camel.apache.org/wsdl-first}soap");
    protected Endpoint endpoint;
    
    @BeforeEach
    public void startService() {
        Object implementor = new PersonImpl();
        String address = "/CxfWsdlFirstProcessorTest/PersonService/";
        endpoint = Endpoint.publish(address, implementor);
    }

    @AfterEach
    public void stopService() {
        if (endpoint != null) {
            endpoint.stop();
        }
    }
    
    
    
    @Override
    protected void verifyJaxwsHandlers(JaxwsTestHandler fromHandler, JaxwsTestHandler toHandler) {
        assertEquals(2, fromHandler.getFaultCount());
        assertEquals(4, fromHandler.getMessageCount());
        assertEquals(0, toHandler.getGetHeadersCount());
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
            cxfEndpoint.setServiceClass(org.apache.camel.wsdl_first.Person.class);
            cxfEndpoint.setAddress("/CxfWsdlFirstProcessorTest/RouterService/");
            cxfEndpoint.setWsdlURL("classpath:person.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("schema-validation-enabled", true);
            cxfEndpoint.setProperties(properties);
            List<Handler> handlers = new ArrayList<Handler>();
            handlers.add(fromHandler);
            cxfEndpoint.setHandlers(handlers);
            cxfEndpoint.setDataFormat(DataFormat.POJO);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(serviceName);
            cxfEndpoint.setEndpointNameAsQName(endpointName);
            cxfEndpoint.setServiceClass(org.apache.camel.wsdl_first.Person.class);
            cxfEndpoint.setAddress("http://localhost:" + port 
                                   + "/services/CxfWsdlFirstProcessorTest/PersonService/");
            List<Handler> handlers = new ArrayList<Handler>();
            handlers.add(toHandler);
            cxfEndpoint.setHandlers(handlers);
            cxfEndpoint.setDataFormat(DataFormat.POJO);
            return cxfEndpoint;
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    errorHandler(noErrorHandler());
                    from("cxf:bean:routerEndpoint")
                            .process(new Processor() {
                                public void process(final Exchange exchange) {
                                    LOG.info("processing exchange in camel");

                                    BindingOperationInfo boi = (BindingOperationInfo) exchange.getProperty(BindingOperationInfo.class.getName());
                                    if (boi != null) {
                                        LOG.info("boi.isUnwrapped" + boi.isUnwrapped());
                                    }
                                    // Get the parameters list which element is the holder.
                                    MessageContentsList msgList = (MessageContentsList) exchange.getIn().getBody();
                                    Holder<String> personId = (Holder<String>) msgList.get(0);
                                    Holder<String> ssn = (Holder<String>) msgList.get(1);
                                    Holder<String> name = (Holder<String>) msgList.get(2);

                                    if (personId.value == null || personId.value.length() == 0) {
                                        LOG.info("person id 123, so throwing exception");
                                        // Try to throw out the soap fault message
                                        org.apache.camel.wsdl_first.types.UnknownPersonFault personFault
                                                = new org.apache.camel.wsdl_first.types.UnknownPersonFault();
                                        personFault.setPersonId("");
                                        org.apache.camel.wsdl_first.UnknownPersonFault fault
                                                = new org.apache.camel.wsdl_first.UnknownPersonFault("Get the null value of person name", personFault);
                                        exchange.getMessage().setBody(fault);
                                        return;
                                    }

                                    name.value = "Bonjour";
                                    ssn.value = "123";
                                    LOG.info("setting Bonjour as the response");
                                    // Set the response message, first element is the return value of the operation,
                                    // the others are the holders of method parameters
                                    exchange.getMessage().setBody(new Object[] { null, personId, ssn, name });
                                }
                            });
                    from("direct:producer")
                        .to("cxf:bean:serviceEndpoint");
                    
                }
            };
        }
    }

}
