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
package org.apache.camel.component.cxf.soap.springboot.namespace;




import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.DataFormat;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfPayloadProducerNamespaceOnEnvelopeTest.class,
        CxfPayloadProducerNamespaceOnEnvelopeTest.TestConfiguration.class,
        CxfPayloadProducerNamespaceOnEnvelopeTest.EndpointConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfPayloadProducerNamespaceOnEnvelopeTest {
    
    
    protected static final String RESPONSE_MESSAGE
        = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><soap:Body>"
            + "<ns2:getTokenResponse xmlns:ns2=\"http://camel.apache.org/cxf/namespace\"><return xsi:type=\"xs:string\">Return Value</return></ns2:getTokenResponse></soap:Body></soap:Envelope>";
    protected static final String REQUEST_PAYLOAD = "<ns2:getToken xmlns:ns2=\"http://camel.apache.org/cxf/namespace\"/>";
    
    private static QName SERVICENAME = QName.valueOf("{http://camel.apache.org/cxf/namespace}GetTokenService");
    
    static int port = CXFTestSupport.getPort1();


    
    
    
    
    @Autowired
    ProducerTemplate template;
    
    @Test
    public void testInvokeRouter() {
        Object returnValue = template.requestBody("direct:router", REQUEST_PAYLOAD);
        assertNotNull(returnValue);
        assertTrue(returnValue instanceof String);
        assertTrue(((String) returnValue).contains("Return Value"));
        assertTrue(((String) returnValue).contains("http://www.w3.org/2001/XMLSchema-instance"));
    }
    

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:router") //
                        // call an external Web service in payload mode
                        .to("cxf:bean:serviceEndpoint")
                        // Convert the CxfPayload to a String to trigger the
                        // issue
                        .convertBodyTo(String.class)
                        // Parse to DOM to make sure it's still valid XML
                        .convertBodyTo(Document.class)
                        // Convert back to String to make testing the result
                        // easier
                        .convertBodyTo(String.class);
                    // This route just returns the test message
                    from("cxf:bean:routerEndpoint").setBody().constant(RESPONSE_MESSAGE);
                }
            };
        }
    }
    
    @Configuration
    class EndpointConfiguration {
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            return new UndertowServletWebServerFactory(port);
        }
        
        @Bean
        CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            cxfEndpoint.setAddress("/GetToken/SoapContext/SoapPort");
            cxfEndpoint.setDataFormat(DataFormat.RAW);
            return cxfEndpoint;
        }
        
        
        @Bean
        CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceNameAsQName(SERVICENAME);
            cxfEndpoint.setAddress("http://localhost:" + port 
                + "/services/GetToken/SoapContext/SoapPort");
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            return cxfEndpoint;
        }
    }
    
}
