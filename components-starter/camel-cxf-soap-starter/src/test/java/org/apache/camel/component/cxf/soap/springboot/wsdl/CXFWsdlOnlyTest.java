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




import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.wsdl_first.Person;
import org.apache.camel.wsdl_first.PersonImpl;
import org.apache.camel.wsdl_first.PersonService;
import org.apache.camel.wsdl_first.UnknownPersonFault;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CXFWsdlOnlyTest.class,
        CXFWsdlOnlyTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CXFWsdlOnlyTest {
    
    private static final QName SERVICE_NAME = new QName("http://camel.apache.org/wsdl-first", "PersonService");
    private static final QName PORT_NAME = new QName("http://camel.apache.org/wsdl-first", "soap");


    private static Endpoint endpoint1;
    private static Endpoint endpoint2;
    
    static int port = CXFTestSupport.getPort1();

    @BeforeEach
    public void setup() {
        Object implementor = new PersonImpl();
        String address = "/CXFWsdlOnlyTest/PersonService/endpoint1/backend";
        endpoint1 = Endpoint.publish(address, implementor);

        address = "/CXFWsdlOnlyTest/PersonService/endpoint2/backend";
        endpoint2 = Endpoint.publish(address, implementor);
    }
    
    @AfterEach
    public void tearDown() {
        if (endpoint1 != null) {
            endpoint1.stop();
        }

        if (endpoint2 != null) {
            endpoint2.stop();
        }
    }
    
    
    @Test
    public void testRoutesWithFault() throws Exception {
        URL wsdlURL = getClass().getClassLoader().getResource("person.wsdl");
        PersonService ss = new PersonService(
                wsdlURL, new QName(
                        "http://camel.apache.org/wsdl-first",
                        "PersonService"));
        Person client = ss.getSoap();

        ((BindingProvider) client).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CXFWsdlOnlyTest/PersonService/endpoint1");
        Holder<String> personId = new Holder<>();
        personId.value = "hello";
        Holder<String> ssn = new Holder<>();
        Holder<String> name = new Holder<>();
        client.getPerson(personId, ssn, name);
        assertEquals("Bonjour", name.value);

        personId.value = "";
        ssn = new Holder<>();
        name = new Holder<>();
        Throwable t = null;
        try {
            client.getPerson(personId, ssn, name);
            fail("Expect exception");
        } catch (UnknownPersonFault e) {
            t = e;
        }
        assertTrue(t instanceof UnknownPersonFault);

        Person client2 = ss.getSoap2();
        ((BindingProvider) client2).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://localhost:" + port + "/services/CXFWsdlOnlyTest/PersonService/endpoint2");
        Holder<String> personId2 = new Holder<>();
        personId2.value = "hello";
        Holder<String> ssn2 = new Holder<>();
        Holder<String> name2 = new Holder<>();
        client2.getPerson(personId2, ssn2, name2);
        assertEquals("Bonjour", name2.value);

        personId2.value = "";
        ssn2 = new Holder<>();
        name2 = new Holder<>();
        try {
            client2.getPerson(personId2, ssn2, name2);
            fail("Expect exception");
        } catch (UnknownPersonFault e) {
            t = e;
        }
        assertTrue(t instanceof UnknownPersonFault);
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
        CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_NAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_NAME);
            cxfEndpoint.setWsdlURL("person.wsdl");
            cxfEndpoint.setAddress("/CXFWsdlOnlyTest/PersonService/endpoint1");
            cxfEndpoint.setDataFormat(DataFormat.RAW);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_NAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_NAME);
            cxfEndpoint.setWsdlURL("person.wsdl");
            cxfEndpoint.setAddress("http://localhost:" + port 
                                   + "/services/CXFWsdlOnlyTest/PersonService/endpoint1/backend");
            cxfEndpoint.setDataFormat(DataFormat.RAW);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint routerEndpoint2() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_NAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_NAME);
            cxfEndpoint.setWsdlURL("person.wsdl");
            cxfEndpoint.setAddress("/CXFWsdlOnlyTest/PersonService/endpoint2");
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceEndpoint2() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICE_NAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_NAME);
            cxfEndpoint.setWsdlURL("person.wsdl");
            cxfEndpoint.setAddress("http://localhost:" + port 
                                   + "/services/CXFWsdlOnlyTest/PersonService/endpoint2/backend");
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            return cxfEndpoint;
        }
            

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:routerEndpoint")
                    .to("cxf:bean:serviceEndpoint")
                    .to("log:camelLogger");
                    
                    from("cxf:bean:routerEndpoint2")
                    .to("cxf:bean:serviceEndpoint2")
                    .to("log:camelLogger");
                    
                }
            };
        }
    }
    
    
}
