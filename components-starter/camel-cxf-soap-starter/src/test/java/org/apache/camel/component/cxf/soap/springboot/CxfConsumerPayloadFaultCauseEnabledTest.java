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




import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.wsdl_first.Person;
import org.apache.camel.wsdl_first.PersonService;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfConsumerPayloadFaultCauseEnabledTest.class,
        CxfConsumerPayloadFaultCauseEnabledTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfConsumerPayloadFaultCauseEnabledTest {

    protected static final QName SERVICE_QNAME = new QName("http://camel.apache.org/wsdl-first", "PersonService");
    protected static final QName PORT_QNAME = new QName("http://camel.apache.org/wsdl-first", "soap");

    static int port = CXFTestSupport.getPort1();
    
    
    @Test
    public void testInvokingFromCxfClient() throws Exception {
        
        URL wsdlURL = getClass().getClassLoader().getResource("person.wsdl");
        PersonService ss = new PersonService(wsdlURL, SERVICE_QNAME);

        Person client = ss.getSoap();

        Client c = ClientProxy.getClient(client);
        c.getInInterceptors().add(new LoggingInInterceptor());
        c.getOutInterceptors().add(new LoggingOutInterceptor());
        ((BindingProvider) client).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                     "http://localhost:" + port 
                     + "/services/CxfConsumerPayloadFaultCauseEnabledTest/PersonService");

        Holder<String> personId = new Holder<>();
        personId.value = "";
        Holder<String> ssn = new Holder<>();
        Holder<String> name = new Holder<>();
        try {
            client.getPerson(personId, ssn, name);
            fail("SOAPFault expected!");
        } catch (Exception e) {
            assertTrue(e instanceof SOAPFaultException);
            SOAPFault fault = ((SOAPFaultException) e).getFault();
            assertEquals("Someone messed up the service. Caused by: Homer", fault.getFaultString());
        }
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
        CxfEndpoint consumerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_QNAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_QNAME);
            cxfEndpoint.setAddress("/CxfConsumerPayloadFaultCauseEnabledTest/PersonService");
            cxfEndpoint.setWsdlURL("classpath:person.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "PAYLOAD");
            properties.put("exceptionMessageCauseEnabled", "true");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:consumerEndpoint").process(new Processor() {
                        public void process(final Exchange exchange) throws Exception {
                            Throwable cause = new IllegalArgumentException("Homer");
                            Fault fault = new Fault("Someone messed up the service.", (ResourceBundle) null, cause);
                            exchange.setException(fault);
                        }
                    });
                }
            };
        }
    }
    
    
}
