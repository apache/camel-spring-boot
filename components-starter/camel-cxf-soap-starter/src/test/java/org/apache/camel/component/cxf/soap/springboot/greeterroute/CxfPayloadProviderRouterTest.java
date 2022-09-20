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

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, 
                           CxfPayloadProviderRouterTest.class,
                           CxfPayloadProviderRouterTest.TestConfiguration.class,
                           AbstractCXFGreeterRouterTest.TestConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfPayloadProviderRouterTest extends AbstractCXFGreeterRouterTest {

    private static String backServiceAddress = "/CxfPayloadProviderRouterTest/SoapContext/SoapPort";
    protected static Endpoint endpoint;
    protected static GreeterImpl implementor;
    

    @AfterEach
    public void stopService() {
        if (endpoint != null) {
            endpoint.stop();
        }
    }

    @BeforeEach
    public void startService() {
        implementor = new GreeterImpl();
        endpoint = Endpoint.publish(backServiceAddress, implementor);
    }

    @Override
    @Test
    public void testPublishEndpointUrl() throws Exception {
        final String path = getClass().getSimpleName() + "/CamelContext/RouterPort/" + getClass().getSimpleName();
        String response = template.requestBody("http://localhost:" + port + "/services/" + path
                                               + "?wsdl",
                null, String.class);
        assertTrue(response.indexOf(path) > 0, "Can't find the right service location.");
    }

    @Test
    public void testInvokeGreetMeOverProvider() throws Exception {
        Service service = Service.create(serviceName);
        service.addPort(routerPortName, "http://schemas.xmlsoap.org/soap/",
                "http://localhost:" + port + "/services/"
                    + getClass().getSimpleName()
                    + "/CamelContext/RouterPort");
        Greeter greeter = service.getPort(routerPortName, Greeter.class);
        org.apache.cxf.endpoint.Client client = org.apache.cxf.frontend.ClientProxy.getClient(greeter);
        VerifyInboundInterceptor icp = new VerifyInboundInterceptor();
        client.getInInterceptors().add(icp);

        int ic = implementor.getInvocationCount();

        icp.setCalled(false);
        String reply = greeter.greetMe("test");
        assertEquals("Hello test", reply, "Got the wrong reply");
        assertTrue(icp.isCalled(), "No Inbound message received");
        assertEquals(++ic, implementor.getInvocationCount(), "The target service not invoked");

        icp.setCalled(false);
        greeter.greetMeOneWay("call greetMe OneWay !");
        assertFalse(icp.isCalled(), "An unnecessary inbound message");
        // wait a few seconds for the async oneway service to be invoked
        Thread.sleep(3000);
        assertEquals(++ic, implementor.getInvocationCount(), "The target service not invoked");
    }

    static class VerifyInboundInterceptor extends AbstractPhaseInterceptor<Message> {
        private boolean called;

        VerifyInboundInterceptor() {
            super(Phase.USER_PROTOCOL);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            called = true;
        }

        public boolean isCalled() {
            return called;
        }

        public void setCalled(boolean b) {
            called = b;
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
            cxfEndpoint.setAddress("/CxfPayloadProviderRouterTest/CamelContext/RouterPort");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("jaxws.provider.interpretNullAsOneway", true);
            cxfEndpoint.setProperties(properties);
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            cxfEndpoint.setSynchronous(true);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceEndpoint() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setAddress("http://localhost:" + port + "/services" + backServiceAddress);
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            cxfEndpoint.setSynchronous(true);
            return cxfEndpoint;
        }


        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:routerEndpoint")
                            .setHeader("operationNamespace", constant("http://camel.apache.org/cxf/jaxws/dispatch"))
                            .setHeader("operationName", constant("Invoke"))
                            .to("cxf:bean:serviceEndpoint");
                }
            };
        }
    }
    
    

}
