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
package org.apache.camel.component.cxf.soap.springboot.converter;




import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.non_wrapper.Person;
import org.apache.camel.non_wrapper.types.GetPerson;
import org.apache.camel.non_wrapper.types.GetPersonResponse;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        PayLoadConvertToPOJOTest.class,
        PayLoadConvertToPOJOTest.TestConfiguration.class,
        CxfAutoConfiguration.class
        
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class PayLoadConvertToPOJOTest {
    
    
    static int port = CXFTestSupport.getPort1();

    @Test
    public void testClient() throws Exception {

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost:" + port + "/services/"
                           + getClass().getSimpleName() + "/CamelContext/RouterPort");
        factory.setServiceClass(Person.class);
        Person person = factory.create(Person.class);
        GetPerson payload = new GetPerson();
        payload.setPersonId("1234");

        GetPersonResponse reply = person.getPerson(payload);
        assertEquals("1234", reply.getPersonId(), "Get the wrong person id.");

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
        public CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress("/PayLoadConvertToPOJOTest/CamelContext/RouterPort");
            cxfEndpoint.setServiceClass(org.apache.camel.non_wrapper.Person.class);
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            return cxfEndpoint;
        }
        
        
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:routerEndpoint").streamCaching().process(new Processor() {

                        @Override
                        public void process(Exchange exchange) throws Exception {
                            // just try to turn the payload to the parameter we want
                            // to use
                            GetPerson request = exchange.getIn().getBody(GetPerson.class);

                            GetPersonResponse reply = new GetPersonResponse();
                            reply.setPersonId(request.getPersonId());
                            exchange.getMessage().setBody(reply);
                        }

                    });
                }
            };
        }
    }
    
    
}
