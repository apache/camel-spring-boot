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





import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import javax.xml.ws.Endpoint;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.component.cxf.wsdl.OrderEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;



import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        OrderTest.class,
        OrderTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class OrderTest {
    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:end")
    MockEndpoint mock;
    
    static int port = CXFTestSupport.getPort1();

        
    
    @Test
    public void testCamelWsdl() throws Exception {
        Object body = template.sendBody("http://localhost:" + port 
                                        + "/services" + "/camel-order/?wsdl",
                ExchangePattern.InOut, null);
        InputStream is = context.getTypeConverter().convertTo(InputStream.class, body);
        checkWsdl(is);
    }

    @Test
    public void testCxfWsdl() throws Exception {
        Object implementor = new OrderEndpoint();
        Endpoint.publish("/cxf-order/", implementor);
        Object body = template.sendBody("http://localhost:" + port 
                                        + "/services" + "/cxf-order/?wsdl",
                ExchangePattern.InOut, null);
        InputStream is = context.getTypeConverter().convertTo(InputStream.class, body);
        checkWsdl(is);
    }

    public void checkWsdl(InputStream in) throws Exception {

        boolean containsOrderComplexType = false;
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("complexType name=\"order\"")) {
                containsOrderComplexType = true;
                // break;
            }

        }

        if (!containsOrderComplexType) {
            throw new RuntimeException("WSDL does not contain complex type defintion for class Order");
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
        CxfEndpoint orderEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(OrderEndpoint.class);
            cxfEndpoint.setAddress("/camel-order/");
            return cxfEndpoint;
        }
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:orderEndpoint")
                    .transform().constant("OK")
                    .to("mock:end");
                }
            };
        }
    }
    
    
}
