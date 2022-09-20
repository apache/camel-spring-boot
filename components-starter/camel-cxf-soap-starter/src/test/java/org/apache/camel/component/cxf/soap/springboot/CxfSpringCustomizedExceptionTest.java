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


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.message.CxfConstants;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfSpringCustomizedExceptionTest.class,
                           CxfSpringCustomizedExceptionTest.TestConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfSpringCustomizedExceptionTest {

    private static final String EXCEPTION_MESSAGE = "This is an exception test message";
    private static final String DETAIL_TEXT = "This is a detail text node";
    private static final SoapFault SOAP_FAULT;
    static int port = CXFTestSupport.getPort1();

    static {
        // START SNIPPET: FaultDefine
        SOAP_FAULT = new SoapFault(EXCEPTION_MESSAGE, Fault.FAULT_CODE_CLIENT);
        Element detail = SOAP_FAULT.getOrCreateDetail();
        Document doc = detail.getOwnerDocument();
        Text tn = doc.createTextNode(DETAIL_TEXT);
        detail.appendChild(tn);
        // END SNIPPET: FaultDefine
    }
    
    
    
    @Bean
    private CxfEndpoint serviceEndpoint() {
        CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
        cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.HelloService.class);
        cxfEndpoint.setAddress("http://localhost:" + port 
                               + "/services/CxfSpringCustomizedExceptionTest/router");
        return cxfEndpoint;
    }

    @Autowired
    ProducerTemplate template;
    
    @Test
    public void testInvokingServiceFromCamel() throws Exception {
        try {
            template.sendBodyAndHeader("direct:start", ExchangePattern.InOut, "hello world", CxfConstants.OPERATION_NAME,
                    "echo");
            fail("Should have thrown an exception");
        } catch (Exception ex) {
            Throwable result = ex.getCause();
            assertTrue(result instanceof SoapFault, "Exception is not instance of SoapFault");
            assertEquals(DETAIL_TEXT, ((SoapFault) result).getDetail().getTextContent(), "Expect to get right detail message");
            assertEquals("{http://schemas.xmlsoap.org/soap/envelope/}Client", ((SoapFault) result).getFaultCode().toString(),
                    "Expect to get right fault-code");
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
        CxfEndpoint routerEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.HelloService.class);
            cxfEndpoint.setAddress("/CxfSpringCustomizedExceptionTest/router");
            return cxfEndpoint;
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start")
                            .to("cxf:bean:serviceEndpoint");
                    from("cxf:bean:routerEndpoint").process(new Processor() {
                        public void process(final Exchange exchange) {
                            exchange.getMessage().setBody(SOAP_FAULT);
                        }
                    }).to("log:mylog");

                }
            };
        }
    }

}
