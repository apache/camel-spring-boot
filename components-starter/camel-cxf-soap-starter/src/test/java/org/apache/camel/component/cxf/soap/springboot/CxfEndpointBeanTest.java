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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.jaxws.CxfProducer;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPConduit;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfEndpointBeanTest.class,
                           CxfEndpointBeanTest.TestConfiguration.class
})
public class CxfEndpointBeanTest {
    
    private QName serviceName = QName.valueOf("{http://camel.apache.org/wsdl-first}PersonService");
    private QName endpointName = QName.valueOf("{http://camel.apache.org/wsdl-first}soap");
    static int port = CXFTestSupport.getPort1();
    @Autowired
    ApplicationContext ctx;
    
    @Test
    public void testCxfEndpointsWithCamelContext() {
        CamelContext context = ctx.getBean("camelContext", CamelContext.class);
        // try to create a new CxfEndpoint which could override the old bean's setting
        CxfEndpoint myLocalCxfEndpoint = (CxfEndpoint)context
            .getEndpoint("cxf:bean:routerEndpoint?address=http://localhost:" + port + "/services"
                         + "/CxfEndpointBeanTest/myCamelContext/");
        assertEquals("http://localhost:" + port + "/services" + "/CxfEndpointBeanTest/myCamelContext/",
                     myLocalCxfEndpoint.getAddress(), "Got the wrong endpoint address");

        CxfEndpoint routerEndpoint = ctx.getBean("routerEndpoint", CxfEndpoint.class);
        assertEquals("http://localhost:" + port + "/services" + "/CxfEndpointBeanTest/myCamelContext/",
                     routerEndpoint.getAddress(), "Got the wrong endpoint address");
    }

    @Test
    public void testPropertiesSettingOnCxfClient() throws Exception {
        CxfEndpoint clientEndpoint = ctx.getBean("clientEndpoint", CxfEndpoint.class);
        CxfProducer producer = (CxfProducer) clientEndpoint.createProducer();
        // need to start the producer to get the client
        producer.start();
        Client client = producer.getClient();
        HTTPConduit conduit = (HTTPConduit) client.getConduit();
        assertEquals("test", conduit.getAuthorization().getUserName(), "Got the wrong user name");
    }
    
    @Configuration
    public class TestConfiguration {
        
        @Bean
        CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.HelloService.class);
            cxfEndpoint.setAddress("/CxfEndpointBeanTest/router");
            cxfEndpoint.setContinuationTimeout(60000);
            List<String> schemaLocations = new ArrayList<String>();
            schemaLocations.add("classpath:wsdl/Message.xsd");
            cxfEndpoint.setSchemaLocations(schemaLocations);
            List<Handler> handlers = new ArrayList<Handler>();
            handlers.add(new JaxwsTestHandler());
            cxfEndpoint.setHandlers(handlers);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint clientEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.HelloService.class);
            
            cxfEndpoint.setAddress("http://localhost:" + port + "/services/CxfEndpointBeanTest/helloworld");
            
            Map<String, Object> properties = new HashMap<String, Object>();
            AuthorizationPolicy policy = new AuthorizationPolicy();
            policy.setUserName("test");
            properties.put("org.apache.cxf.configuration.security.AuthorizationPolicy", policy);
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint myEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.wsdl_first.Person.class);
            cxfEndpoint.setAddress("/CxfEndpointBeanTest/test");
            cxfEndpoint.setWsdlURL("person.wsdl");
            cxfEndpoint.setServiceNameAsQName(serviceName);
            cxfEndpoint.setEndpointNameAsQName(endpointName);
            cxfEndpoint.setLoggingFeatureEnabled(true);
            cxfEndpoint.setLoggingSizeLimit(200);
            
            SoapBindingConfiguration bindingCfg = new SoapBindingConfiguration();
            bindingCfg.setVersion(Soap12.getInstance());
            cxfEndpoint.setBindingConfig(bindingCfg);
            return cxfEndpoint;
        }
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start")
                            .to("cxf:bean:myEndpoint");
                    from("cxf:bean:myEndpoint").to("mock:result");
                }
            };
        }
    }
    
}
