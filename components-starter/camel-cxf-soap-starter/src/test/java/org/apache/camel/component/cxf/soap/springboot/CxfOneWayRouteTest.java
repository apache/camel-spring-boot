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




import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.jaxws.DefaultCxfBinding;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.hello_world_soap_http.Greeter;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CxfOneWayRouteTest.class,
        CxfOneWayRouteTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CxfOneWayRouteTest {

    private static final QName SERVICE_NAME = new QName("http://apache.org/hello_world_soap_http", "SOAPService");
    private static final QName PORT_NAME = new QName("http://apache.org/hello_world_soap_http", "SoapPort");
    private static final String ROUTER_ADDRESS = "/CxfOneWayRouteTest/router";

    private static Exception bindingException;
    private static boolean bindingDone;
    private static boolean onCompeletedCalled;
    
    static int port = CXFTestSupport.getPort1();

    @BeforeEach
    public void setup() {
        bindingException = null;
        bindingDone = false;
        onCompeletedCalled = false;
    }
    
    
    protected Greeter getCXFClient() throws Exception {
        Service service = Service.create(SERVICE_NAME);
        service.addPort(PORT_NAME, "http://schemas.xmlsoap.org/soap/", 
                        "http://localhost:" + port 
                        + "/services" + ROUTER_ADDRESS);
        Greeter greeter = service.getPort(PORT_NAME, Greeter.class);
        return greeter;
    }

 

    @EndpointInject("mock:result")
    MockEndpoint mock;
    
       
    @Bean
    TestProcessor testProcessor() {
        return new TestProcessor();
    }
    
    @Test
    public void testInvokingOneWayServiceFromCXFClient() throws Exception {
        mock.expectedMessageCount(1);
        mock.expectedFileExists("target/camel-file/cxf-oneway-route");

        Greeter client = getCXFClient();
        client.greetMeOneWay("lemac");

        // may need to wait until the oneway call completes 
        long waitUntil = System.currentTimeMillis() + 10000;
        while (!bindingDone && System.currentTimeMillis() < waitUntil) {
            Thread.sleep(1000);
        }

        mock.assertIsSatisfied();
        assertTrue(onCompeletedCalled, "UnitOfWork done should be called");
        assertNull(bindingException, "exception occured: " + bindingException);
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
            cxfEndpoint.setServiceNameAsQName(SERVICE_NAME);
            cxfEndpoint.setEndpointNameAsQName(PORT_NAME);
            cxfEndpoint.setServiceClass(org.apache.hello_world_soap_http.GreeterImpl.class);
            cxfEndpoint.setAddress(ROUTER_ADDRESS);
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("org.apache.cxf.oneway.robust", true);
            cxfEndpoint.setProperties(properties);
            cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
            cxfEndpoint.setCxfBinding(new TestCxfBinding());
            return cxfEndpoint;
        }
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("cxf:bean:routerEndpoint")
                    .to("log:org.apache.camel?level=DEBUG")
                    .to("bean:testProcessor")
                    .to("file://target/camel-file/cxf-oneway-route")
                    .to("mock:result");
                }
            };
        }
    }
    
    public static class TestProcessor implements Processor {
        static final byte[] MAGIC = { (byte) 0xca, 0x3e, 0x1e };

        @Override
        public void process(Exchange exchange) throws Exception {
            // just check the MEP here
            assertEquals(ExchangePattern.InOnly, exchange.getPattern(), "Don't get the right MEP");
            // adding some binary segment
            String msg = exchange.getIn().getBody(String.class);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(MAGIC);
            bos.write(msg.getBytes());
            exchange.getIn().setBody(bos.toByteArray());
            // add compliation
            exchange.getUnitOfWork().addSynchronization(new Synchronization() {
                @Override
                public void onComplete(Exchange exchange) {
                    onCompeletedCalled = true;
                }

                @Override
                public void onFailure(Exchange exchange) {
                    // do nothing here
                }
            });
        }
    }

    public static class TestCxfBinding extends DefaultCxfBinding {

        @Override
        public void populateCxfResponseFromExchange(Exchange camelExchange, org.apache.cxf.message.Exchange cxfExchange) {
            try {
                super.populateCxfResponseFromExchange(camelExchange, cxfExchange);
            } catch (RuntimeException e) {
                bindingException = e;
                throw e;
            } finally {
                bindingDone = true;
            }
        }

    }
}
