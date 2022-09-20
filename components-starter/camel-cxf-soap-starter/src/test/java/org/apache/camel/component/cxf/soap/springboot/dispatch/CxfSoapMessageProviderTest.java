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
package org.apache.camel.component.cxf.soap.springboot.dispatch;




import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.ParameterProcessor;
import org.apache.camel.component.cxf.SoapTargetBean;
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
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.wsdl_first.JaxwsTestHandler;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfSoapMessageProviderTest.class,
        CxfSoapMessageProviderTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfSoapMessageProviderTest {
    
   
    
    static int port = CXFTestSupport.getPort1();


           
    @Test
    public void testSOAPMessageModeDocLit() throws Exception {
        JaxwsTestHandler fromHandler = getMandatoryBean(JaxwsTestHandler.class, "fromEndpointJaxwsHandler");
        fromHandler.reset();

        QName serviceName = new QName("http://apache.org/hello_world_soap_http", "SOAPProviderService");
        QName portName = new QName("http://apache.org/hello_world_soap_http", "SoapProviderPort");

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        String response1 = new String("TestSOAPOutputPMessage");
        String response2 = new String("Bonjour");
        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            ((BindingProvider) greeter).getRequestContext()
                    .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                            "http://localhost:" + port + "/services/CxfSoapMessageProviderTest/SoapContext/SoapProviderPort");
            for (int idx = 0; idx < 2; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull(greeting, "no response received from service");
                assertEquals(response1, greeting);

                String reply = greeter.sayHi();
                assertNotNull(reply, "no response received from service");
                assertEquals(response2, reply);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }

        assertEquals(8, fromHandler.getMessageCount(), "Can't get the right message count");
        assertEquals(0, fromHandler.getFaultCount(), "Can't get the right fault count");
       
    }
    
    @Autowired
    AbstractApplicationContext applicationContext;
    
    private <T> T getMandatoryBean(Class<T> type, String name) {
        Object value = applicationContext.getBean(name);
        assertNotNull(value, "No spring bean found for name <" + name + ">");
        if (type.isInstance(value)) {
            return type.cast(value);
        } else {
            fail("Spring bean <" + name + "> is not an instanceof " + type.getName() + " but is of type "
                 + ObjectHelper.className(value));
            return null;
        }
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        
        @Bean
        public SoapTargetBean targetBean() {
            return new SoapTargetBean();
        }
        
        @Bean
        public ParameterProcessor parameterProcessor() {
            return new ParameterProcessor();
        }
        
        @Bean
        public JaxwsTestHandler fromEndpointJaxwsHandler() {
            return new JaxwsTestHandler();
        }
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            return new UndertowServletWebServerFactory(port);
        }
        
        
        @Bean
        CxfEndpoint soapMessageEndpoint(JaxwsTestHandler fromEndpointJaxwsHandler) {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.component.cxf.SoapMessageProvider.class);
            cxfEndpoint.setAddress("/CxfSoapMessageProviderTest/SoapContext/SoapProviderPort");
            List<Handler> handlers = new ArrayList<Handler>();
            handlers.add(fromEndpointJaxwsHandler);
            cxfEndpoint.setHandlers(handlers);
            return cxfEndpoint;
        }
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:soapMessageEndpoint")
                    .process("parameterProcessor")
                    .to("bean:targetBean?method=invokeSoapMessage");
                }
            };
        }
    }
    
    
}
