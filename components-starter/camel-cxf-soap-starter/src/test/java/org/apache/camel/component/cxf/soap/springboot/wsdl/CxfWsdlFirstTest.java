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
import javax.xml.ws.handler.Handler;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.soap.springboot.wsdl.AbstractCxfWsdlFirstTest.ServletConfiguration;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.wsdl_first.PersonImpl;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfWsdlFirstTest.class,
                           CxfWsdlFirstTest.TestConfiguration.class,
                           AbstractCxfWsdlFirstTest.ServletConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfWsdlFirstTest extends AbstractCxfWsdlFirstTest {

    private QName serviceName = QName.valueOf("{http://camel.apache.org/wsdl-first}PersonService");
    private QName endpointName = QName.valueOf("{http://camel.apache.org/wsdl-first}soap");
    protected Endpoint endpoint;
    
    @BeforeEach
    public void startService() {
        Object implementor = new PersonImpl();
        String address = "/CxfWsdlFirstTest/PersonService/";
        endpoint = Endpoint.publish(address, implementor);
    }

    @AfterEach
    public void stopService() {
        if (endpoint != null) {
            endpoint.stop();
        }
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
            cxfEndpoint.setAddress("/CxfWsdlFirstTest/RouterService/");
            cxfEndpoint.setWsdlURL("classpath:person.wsdl");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("schema-validation-enabled", true);
            cxfEndpoint.setProperties(properties);
            List<Handler> handlers = new ArrayList<Handler>();
            handlers.add(fromHandler);
            cxfEndpoint.setHandlers(handlers);
            return cxfEndpoint;
        }
        
        @Bean
        @Scope("prototype")
        CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(serviceName);
            cxfEndpoint.setEndpointNameAsQName(endpointName);
            cxfEndpoint.setServiceClass(org.apache.camel.wsdl_first.Person.class);
            cxfEndpoint.setAddress("http://localhost:" + port 
                                   + "/services/CxfWsdlFirstTest/PersonService/");
            List<Handler> handlers = new ArrayList<Handler>();
            handlers.add(toHandler);
            cxfEndpoint.setHandlers(handlers);
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
                        .to("cxf:bean:serviceEndpoint?dataFormat=POJO");
                    
                }
            };
        }
    }

}
