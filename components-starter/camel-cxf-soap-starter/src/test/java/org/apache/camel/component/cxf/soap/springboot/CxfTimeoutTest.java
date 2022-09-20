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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.xml.ws.Endpoint;


import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.GreeterImplWithSleep;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxws.CxfConfigurer;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hello_world_soap_http.Greeter;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
                           CamelAutoConfiguration.class, CxfTimeoutTest.class,
                           CxfTimeoutTest.TestConfiguration.class,
                           CxfAutoConfiguration.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxfTimeoutTest {

      
    static int port = CXFTestSupport.getPort1();;
    
    protected static final String GREET_ME_OPERATION = "greetMe";
    protected static final String TEST_MESSAGE = "Hello World!";
    protected static final String SERVER_ADDRESS = "/CxfTimeoutTest/SoapContext/SoapPort";
    protected static final String JAXWS_SERVER_ADDRESS
            = "http://localhost:" + port + "/services/CxfTimeoutTest/SoapContext/SoapPort";

    private Endpoint endpoint;
    @BeforeEach
    public void startService() {
        Greeter implementor = new GreeterImplWithSleep();
        endpoint = Endpoint.publish(SERVER_ADDRESS, implementor);
    }
    
    @AfterEach
    public void stopService() {
        endpoint.stop();
    }
    
    

    @Test
    public void testInvokingJaxWsServerWithBusUriParams() throws Exception {
        sendTimeOutMessage("cxf://" + JAXWS_SERVER_ADDRESS + "?serviceClass=org.apache.hello_world_soap_http.Greeter&bus=#cxf&cxfConfigurer=#origConfigurer");
    }

    @Test
    public void testInvokingJaxWsServerWithoutBusUriParams() throws Exception {
        sendTimeOutMessage("cxf://" + JAXWS_SERVER_ADDRESS + "?serviceClass=org.apache.hello_world_soap_http.Greeter&cxfConfigurer=#origConfigurer");
    }

    @Test
    public void testInvokingJaxWsServerWithCxfEndpoint() throws Exception {
        sendTimeOutMessage("cxf://bean:springEndpoint");
    }

    @Test
    public void testInvokingFromCamelRoute() throws Exception {
        sendTimeOutMessage("direct:start");
    }

    @Test
    public void testDoCatchWithTimeOutException() throws Exception {
        sendTimeOutMessage("direct:doCatch");
    }

    protected void sendTimeOutMessage(String endpointUri) throws Exception {
        Exchange reply = sendJaxWsMessage(endpointUri);
        Exception e = reply.getException();
        assertNotNull(e, "We should get the exception cause here");
        assertTrue(e instanceof SocketTimeoutException, "We should get the socket time out exception here");
    }

    @Autowired
    ProducerTemplate template;
    protected Exchange sendJaxWsMessage(String endpointUri) throws InterruptedException {
        Exchange exchange = template.send(endpointUri, new Processor() {
            public void process(final Exchange exchange) {
                final List<String> params = new ArrayList<>();
                params.add(TEST_MESSAGE);
                exchange.getIn().setBody(params);
                exchange.getIn().setHeader(CxfConstants.OPERATION_NAME, GREET_ME_OPERATION);
            }
        });
        return exchange;
    }

    public static class MyCxfConfigurer implements CxfConfigurer {

        @Override
        public void configure(AbstractWSDLBasedEndpointFactory factoryBean) {
            // Do nothing here
        }

        @Override
        public void configureClient(Client client) {
            // reset the timeout option to override the spring configuration one
            HTTPConduit conduit = (HTTPConduit) client.getConduit();
            HTTPClientPolicy policy = new HTTPClientPolicy();
            policy.setReceiveTimeout(60000);
            conduit.setClient(policy);

        }

        @Override
        public void configureServer(Server server) {
            // Do nothing here

        }

    }
    
    public static class OrigCxfConfigurer implements CxfConfigurer {

        @Override
        public void configure(AbstractWSDLBasedEndpointFactory factoryBean) {
            // Do nothing here
        }

        @Override
        public void configureClient(Client client) {
            // reset the timeout option to override the spring configuration one
            HTTPConduit conduit = (HTTPConduit) client.getConduit();
            HTTPClientPolicy policy = new HTTPClientPolicy();
            policy.setReceiveTimeout(100);
            conduit.setClient(policy);

        }

        @Override
        public void configureServer(Server server) {
            // Do nothing here

        }

    }
    
    
    
    

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        HostnameVerifier defaultHostnameVerifier() {
            return new org.apache.cxf.transport.https.httpclient.DefaultHostnameVerifier();
        }
        
        @Bean
        MyCxfConfigurer myConfigurer() {
            return new MyCxfConfigurer();
        }
        
        @Bean
        OrigCxfConfigurer origConfigurer() {
            return new OrigCxfConfigurer();
        }
        
        @Bean
        SSLContextParameters mySslContext() {
            SSLContextParameters sslContext = new SSLContextParameters();
            KeyManagersParameters keyManager = new KeyManagersParameters();
            keyManager.setKeyPassword("changeit");
            KeyStoreParameters keyStore = new KeyStoreParameters();
            keyStore.setPassword("changeit");
            keyStore.setResource("/localhost.p12");
            keyManager.setKeyStore(keyStore);
            sslContext.setKeyManagers(keyManager);
            return sslContext;
        }
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() throws InterruptedException {
            ServletWebServerFactory webServerFactory = new UndertowServletWebServerFactory(port);
            return webServerFactory;
        }

        
        @Bean
        CxfEndpoint springEndpoint() {
            
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress(JAXWS_SERVER_ADDRESS);
            cxfEndpoint.setCxfConfigurer(new OrigCxfConfigurer());
            return cxfEndpoint;
        }
        
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    errorHandler(noErrorHandler());
                    from("direct:start").
                        to("cxf:bean:springEndpoint?sslContextParameters=#mySslContext&hostnameVerifier=#defaultHostnameVerifier");
                    from("direct:doCatch").
                        to("cxf:bean:springEndpoint");
                   
                   
                }
            };
        }
    }

}
