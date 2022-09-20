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
package org.apache.camel.component.cxf.soap.springboot.jms;




import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.hello_world_soap_http.Greeter;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfEndpointJMSConsumerTest.class,
        CxfEndpointJMSConsumerTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }
)
public class CxfEndpointJMSConsumerTest {

    private String namespace = "http://apache.org/hello_world_soap_http";
    private QName serviceName = new QName(namespace, "SOAPService");
    private QName endpointName = new QName(namespace, "SoapPort");
    // Here we just the address with JMS URI
    String address = "jms:jndi:dynamicQueues/test.cxf.jmstransport.queue"
                     + "?jndiInitialContextFactory"
                     + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                     + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL="
                     + "vm://localhost";
        
    
    
    @Test
    public void testInvocation() {
        // Here we just the address with JMS URI
        String address = "jms:jndi:dynamicQueues/test.cxf.jmstransport.queue"
                         + "?jndiInitialContextFactory"
                         + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                         + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL="
                         + "vm://localhost";

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Greeter.class);
        factory.setAddress(address);
        Greeter greeter = factory.create(Greeter.class);
        String response = greeter.greetMe("Camel");
        assertEquals("Hello Camel", response, "Get a wrong response");
    }
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        
        @Bean
        CxfEndpoint jmsEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(serviceName);
            cxfEndpoint.setEndpointNameAsQName(endpointName);
            cxfEndpoint.setServiceClass(org.apache.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress(address);
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            return cxfEndpoint;
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:jmsEndpoint").process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            // just set the response for greetme operation here
                            String me = exchange.getIn().getBody(String.class);
                            exchange.getMessage().setBody("Hello " + me);
                        }
                    });
                }
            };
        }
    }
    
    
}
