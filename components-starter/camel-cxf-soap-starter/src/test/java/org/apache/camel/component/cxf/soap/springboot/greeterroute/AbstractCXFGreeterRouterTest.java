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
package org.apache.camel.component.cxf.soap.springboot.greeterroute;


import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           AbstractCXFGreeterRouterTest.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractCXFGreeterRouterTest {

    
    private final String testDocLitFaultBody = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                               + "<soap:Body><testDocLitFault xmlns=\"http://apache.org/hello_world_soap_http/types\">"
                                               + "<faultType>NoSuchCodeLitFault</faultType></testDocLitFault>"
                                               + "</soap:Body></soap:Envelope>";
    
    protected String routerAddress = "http://localhost:" + port + "/services/" 
        + getClass().getSimpleName() + "/CamelContext/RouterPort";
    
    protected final QName serviceName = new QName("http://apache.org/hello_world_soap_http", "SOAPService");
    protected final QName routerPortName = new QName("http://apache.org/hello_world_soap_http", "RouterPort");
    protected final QName endpointName = new QName("http://apache.org/hello_world_soap_http", "SoapPort");

    static int port = CXFTestSupport.getPort1();
    
    @Autowired
    ProducerTemplate template;

    

    @Test
    public void testInvokingServiceFromCXFClient() throws Exception {
        Service service = Service.create(serviceName);
        service.addPort(routerPortName, "http://schemas.xmlsoap.org/soap/",
                routerAddress);
        Greeter greeter = service.getPort(routerPortName, Greeter.class);

        String reply = greeter.greetMe("test");
        assertNotNull(reply, "No response received from service");
        assertEquals("Hello test", reply, "Got the wrong reply");
        reply = greeter.sayHi();
        assertNotNull(reply, "No response received from service");
        assertEquals("Bonjour", reply, "Got the wrong reply");

        greeter.greetMeOneWay("call greetMe OneWay !");

        // test throw the exception
        try {
            greeter.testDocLitFault("NoSuchCodeLitFault");
            // should get the exception here
            fail("Should get the NoSuchCodeLitFault here.");
        } catch (NoSuchCodeLitFault fault) {
            // expect the fault here
            assertNotNull(fault.getFaultInfo(), "The fault info should not be null");
        }

    }

    @Test
    public void testRoutingSOAPFault() {
        Exception ex = assertThrows(RuntimeCamelException.class,
                () -> template.sendBody(routerAddress, testDocLitFaultBody));

        assertTrue(ex.getCause() instanceof HttpOperationFailedException, "It should get the response error");
        assertEquals(500, ((HttpOperationFailedException) ex.getCause()).getStatusCode(),
                "Get a wrong response code");
    }

    @Test
    public void testPublishEndpointUrl() throws Exception {
        String response = template.requestBody(routerAddress
                                                + "?wsdl",
                null, String.class);
        assertTrue(response.indexOf("http://www.simple.com/services/test") > 0, "Can't find the right service location.");
    }

    @Configuration
    public class TestConfiguration {
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            return new UndertowServletWebServerFactory(port);
        }
    }
}
