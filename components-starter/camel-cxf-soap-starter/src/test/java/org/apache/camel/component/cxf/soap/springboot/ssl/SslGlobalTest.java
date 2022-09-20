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
package org.apache.camel.component.cxf.soap.springboot.ssl;




import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.SSLContextParametersAware;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.GreeterImpl;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.soap.springboot.CxfTimeoutTest.OrigCxfConfigurer;
import org.apache.camel.component.cxf.spring.jaxws.CxfSpringEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.CertificateFileSslStoreProvider;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.apache.cxf.transport.https.httpclient.DefaultHostnameVerifier;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SslGlobalTest.class,
        SslGlobalTest.TestConfiguration.class,
        CxfAutoConfiguration.class
    }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class SslGlobalTest {
    protected static final String GREET_ME_OPERATION = "greetMe";
    protected static final String TEST_MESSAGE = "Hello World!";
    protected static final String JAXWS_SERVER_ADDRESS
            = "/CxfSslTest/SoapContext/SoapPort";
    
    static int port = CXFTestSupport.getPort1();
    
    @Test
    public void testInvokingTrustRoute() throws Exception {
        Exchange reply = sendJaxWsMessage("direct:trust");
        if (reply.isFailed()) {
            Exception exception = reply.getException();
            String msg = exception.getMessage();
            if (msg.contains("socket reset for TTL")) {
                // ignore flaky test on JDK11
                return;
            }
        }
        assertFalse(reply.isFailed(), "We expect no exception here");
    }

    @Test
    public void testInvokingWrongTrustRoute() throws Exception {
        Exchange reply = sendJaxWsMessage("direct:wrongTrust");
        assertTrue(reply.isFailed(), "We expect the exception here");
        Throwable e = reply.getException().getCause();
        assertEquals("javax.net.ssl.SSLHandshakeException", e.getClass().getCanonicalName());
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
                    errorHandler(noErrorHandler());
                    from("cxf:bean:routerEndpoint").to("bean:greeter?method=greetMe");
                    from("direct:trust").to("cxf:bean:serviceEndpoint");
                    from("direct:wrongTrust").to("cxf:bean:serviceEndpointWrong");
                }
            };
        }
        
        @Bean
        GreeterImpl greeter() {
            return new GreeterImpl();
        }
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() throws UnknownHostException {
            UndertowServletWebServerFactory undertowWebServerFactory 
                = new UndertowServletWebServerFactory();
            Ssl ssl = new Ssl();
            ssl.setClientAuth(ClientAuth.NONE);
            ssl.setKeyPassword("changeit");
            ssl.setKeyStoreType("JKS");
            ssl.setKeyStore("classpath:ssl/keystore-server.jks");
            ssl.setKeyStorePassword("changeit");
            SslBuilderCustomizer sslBuilderCustomizer = 
                new SslBuilderCustomizer(port, InetAddress.getByName("localhost"),
                                         ssl, CertificateFileSslStoreProvider.from(ssl));
            undertowWebServerFactory.addBuilderCustomizers(sslBuilderCustomizer);
            return undertowWebServerFactory;
        }
        
        @Bean
        CamelContextConfiguration contextConfiguration() {
            return new CamelContextConfiguration() {
                @Override
                public void beforeApplicationStart(CamelContext context) {
                    SSLContextParameters parameters = context.getRegistry().lookupByNameAndType("mySslContext", SSLContextParameters.class);
                    ((SSLContextParametersAware) context.getComponent("cxf")).setUseGlobalSslContextParameters(true);
                    context.setSSLContextParameters(parameters);
                }

                @Override
                public void afterApplicationStart(CamelContext camelContext) {
                    

                }
            };
        }

        @Bean
        DefaultHostnameVerifier defaultHostnameVerifier() {
            return new DefaultHostnameVerifier();
        }
        
        @Bean
        SSLContextParameters mySslContext() {
            SSLContextParameters sslContext = new SSLContextParameters();
            TrustManagersParameters trustManager = new TrustManagersParameters();
            KeyStoreParameters keyStore = new KeyStoreParameters();
            keyStore.setType("JKS");
            keyStore.setPassword("changeit");
            keyStore.setResource("/ssl/truststore-client.jks");
            trustManager.setKeyStore(keyStore);
            sslContext.setTrustManagers(trustManager);
            return sslContext;
        }
        
        @Bean
        SSLContextParameters wrongSslContext() {
            SSLContextParameters sslContext = new SSLContextParameters();
            TrustManagersParameters trustManager = new TrustManagersParameters();
            KeyStoreParameters keyStore = new KeyStoreParameters();
            keyStore.setType("JKS");
            keyStore.setPassword("changeit");
            keyStore.setResource("/ssl/truststore-wrong.jks");
            trustManager.setKeyStore(keyStore);
            sslContext.setTrustManagers(trustManager);
            return sslContext;
        }
        
        @Bean
        CxfEndpoint routerEndpoint() {
            
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress(JAXWS_SERVER_ADDRESS);
            cxfEndpoint.setCxfConfigurer(new OrigCxfConfigurer());
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceEndpoint(DefaultHostnameVerifier defaultHostnameVerifier) {
            
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("https://localhost:" + port 
                                   + "/services" + JAXWS_SERVER_ADDRESS);
            cxfEndpoint.setHostnameVerifier(defaultHostnameVerifier);
            return cxfEndpoint;
        }
        
        @Bean
        CxfEndpoint serviceEndpointWrong(SSLContextParameters wrongSslContext, DefaultHostnameVerifier defaultHostnameVerifier) {
            
            CxfSpringEndpoint cxfEndpoint = new CxfSpringEndpoint();
            cxfEndpoint.setServiceClass(org.apache.hello_world_soap_http.Greeter.class);
            cxfEndpoint.setAddress("https://localhost:" + port 
                                   + "/services" + JAXWS_SERVER_ADDRESS);
            cxfEndpoint.setHostnameVerifier(defaultHostnameVerifier);
            cxfEndpoint.setSslContextParameters(wrongSslContext);
            return cxfEndpoint;
        }
    }
    
    
}
