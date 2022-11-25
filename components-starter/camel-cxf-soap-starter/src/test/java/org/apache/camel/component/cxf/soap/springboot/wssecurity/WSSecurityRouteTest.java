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
package org.apache.camel.component.cxf.soap.springboot.wssecurity;




import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.hello_world_soap_http.Greeter;
import org.apache.camel.hello_world_soap_http.GreeterService;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        WSSecurityRouteTest.class,
        WSSecurityRouteTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class WSSecurityRouteTest {

    static CxfServer cxfServer;
    
    static int port = CXFTestSupport.getPort2();
    static int backendPort = CXFTestSupport.getPort1();

    @BeforeAll
    public static void setup() throws Exception {
        cxfServer = new CxfServer();
    }
    
    
    @Test
    public void testSignature() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSecurityRouteTest.class.getResource("client/wssec.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        GreeterService gs = new GreeterService(null);
        Greeter greeter = gs.getGreeterSignaturePort();

        ((BindingProvider) greeter).getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                "http://localhost:" + port
                     + "/services/WSSecurityRouteTest/GreeterSignaturePort");

        assertEquals("Hello Security", greeter.greetMe("Security"), "Get a wrong response");
    }

    @Test
    public void testUsernameToken() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSecurityRouteTest.class.getResource("client/wssec.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        GreeterService gs = new GreeterService(null);
        Greeter greeter = gs.getGreeterUsernameTokenPort();

        ((BindingProvider) greeter).getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                "http://localhost:" + port
                     + "/services/WSSecurityRouteTest/GreeterUsernameTokenPort");

        assertEquals("Hello Security", greeter.greetMe("Security"), "Get a wrong response");
    }

    @Test
    public void testEncryption() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSecurityRouteTest.class.getResource("client/wssec.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        GreeterService gs = new GreeterService(null);
        Greeter greeter = gs.getGreeterEncryptionPort();

        ((BindingProvider) greeter).getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                "http://localhost:" + port
                     + "/services/WSSecurityRouteTest/GreeterEncryptionPort");

        assertEquals("Hello Security", greeter.greetMe("Security"), "Get a wrong response");
    }

    @Test
    public void testSecurityPolicy() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSecurityRouteTest.class.getResource("client/wssec.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        GreeterService gs = new GreeterService(null);
        Greeter greeter = gs.getGreeterSecurityPolicyPort();

        ((BindingProvider) greeter).getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                "http://localhost:" + port
                     + "/services/WSSecurityRouteTest/GreeterSecurityPolicyPort");

        assertEquals("Hello Security", greeter.greetMe("Security"), "Get a wrong response");
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
        WSS4JInInterceptor wss4jInInterceptorSignature() {
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("action", "Signature Timestamp");
            properties.put("signaturePropFile", "wssecurity/etc/cxfca.properties");
            return new WSS4JInInterceptor(properties);
        }
        
        @Bean
        WSS4JInInterceptor wss4jInInterceptorUsernameToken() {
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("action", "UsernameToken");
            properties.put("passwordCallbackClass", "org.apache.camel.component.cxf.wssecurity.server.UTPasswordCallback");
            return new WSS4JInInterceptor(properties);
        }
        
        @Bean
        CxfEndpoint signatureRoute(WSS4JInInterceptor wss4jInInterceptorSignature) {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("/WSSecurityRouteTest/GreeterSignaturePort");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getInInterceptors().add(wss4jInInterceptorSignature);
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "CXF_MESSAGE");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint signatureService() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("http://localhost:" + backendPort +
                                   "/services/WSSecurityRouteTest/GreeterSignaturePortBackend");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "CXF_MESSAGE");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint usernameTokenRoute(WSS4JInInterceptor wss4jInInterceptorUsernameToken) {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("/WSSecurityRouteTest/GreeterUsernameTokenPort");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getInInterceptors().add(wss4jInInterceptorUsernameToken);
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "CXF_MESSAGE");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint usernameTokenService() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("http://localhost:" + backendPort +
                                   "/services/WSSecurityRouteTest/GreeterUsernameTokenPortBackend");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "CXF_MESSAGE");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }

        @Bean
        CxfEndpoint encryptionRoute() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("/WSSecurityRouteTest/GreeterEncryptionPort");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "RAW");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint encryptionService() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("http://localhost:" + backendPort +
                                   "/services/WSSecurityRouteTest/GreeterEncryptionPortBackend");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "RAW");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint securityPolicyRoute() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("/WSSecurityRouteTest/GreeterSecurityPolicyPort");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "RAW");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint securityPolicyService() {
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.camel.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("http://localhost:" + backendPort +
                                   "/services/WSSecurityRouteTest/GreeterSecurityPolicyPortBackend");
            cxfEndpoint.getInInterceptors().add(new org.apache.cxf.ext.logging.LoggingInInterceptor());
            cxfEndpoint.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("dataFormat", "RAW");
            cxfEndpoint.setProperties(properties);
            return cxfEndpoint;
        }
        
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    errorHandler(noErrorHandler());
                    from("cxf:bean:signatureRoute")
                    .to("cxf:bean:signatureService");
                    
                    from("cxf:bean:usernameTokenRoute")
                    .to("cxf:bean:usernameTokenService");
                    
                    from("cxf:bean:encryptionRoute")
                    .to("cxf:bean:encryptionService?defaultOperationName=greetMe");
                    
                    from("cxf:bean:securityPolicyRoute")
                    .to("cxf:bean:securityPolicyService?defaultOperationName=greetMe");
                }
            };
        }
    }
    
    
}
