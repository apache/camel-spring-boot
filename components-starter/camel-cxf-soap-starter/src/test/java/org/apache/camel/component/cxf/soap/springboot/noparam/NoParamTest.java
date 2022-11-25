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
package org.apache.camel.component.cxf.soap.springboot.noparam;




import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        NoParamTest.class,
        NoParamTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class NoParamTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NoParamTest.class);
    
    static int port = CXFTestSupport.getPort1();


     
    
    
    @Autowired
    ProducerTemplate template;
        
    @Test
    public void testNullBody() throws Exception {
        Object body = template.sendBody("direct:noParam", ExchangePattern.InOut, null);
        assertNotNull(body);
        LOGGER.error(body.toString());
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
        CxfEndpoint noParamEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.noparam.NoParamEndpoint.class);
            cxfEndpoint.setAddress("/camel-noparam/");
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint noParamServiceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.noparam.NoParamEndpoint.class);
            cxfEndpoint.setAddress("http://localhost:" + port 
                                   + "/services/camel-noparam/");
            return cxfEndpoint;
        }
        
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:noParamEndpoint")
                    .transform(constant("OK"))
                    .to("mock:end");
                    
                    from("direct:noParam")
                    .to("cxf:bean:noParamServiceEndpoint");
                }
            };
        }
    }
    
    
}
