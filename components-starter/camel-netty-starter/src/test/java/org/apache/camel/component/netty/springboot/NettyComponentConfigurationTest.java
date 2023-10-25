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
package org.apache.camel.component.netty.springboot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Properties;

import javax.net.ssl.SSLSession;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.netty.NettyComponent;
import org.apache.camel.component.netty.NettyConstants;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(
    classes = {
        NettyComponentConfigurationTest.class,
        NettyComponentConfigurationTest.TestConfiguration.class
    },
    properties = { 
        "camel.component.netty.ssl=true", 
        "camel.component.netty.ssl-context-parameters=#sslContextParameters" 
    }
)
class NettyComponentConfigurationTest {

    private static final String TLSv13 = "TLSv1.3";
    
    @Autowired
    CamelContext context;
    
    @Autowired
    protected ProducerTemplate template;
    
    @EndpointInject("mock:result")
    private MockEndpoint mockResult;
    
    private static Properties loadAuthProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(NettyComponentConfigurationTest.class.getClassLoader().getResourceAsStream("nettycomponentconfigurationtest.properties"));
        return properties;
    }

    @Test
    void testAutoConfiguration() throws IOException {
        Properties properties = loadAuthProperties();
        NettyComponent component = context.getComponent("netty", NettyComponent.class);
        assertNotNull(component);
        assertNotNull(component.getConfiguration());
        assertTrue(component.getConfiguration().isSsl());
        SSLContextParameters scp = component.getConfiguration().getSslContextParameters();
        assertNotNull(scp);
        assertEquals(TLSv13, scp.getSecureSocketProtocol());
        KeyManagersParameters kmp = scp.getKeyManagers();
        assertEquals(properties.getProperty("password"), kmp.getKeyPassword());
    }
    
    @Test
    void testRoute() {
        template.requestBody(
            "netty:tcp://localhost:5150",
            "Hello Netty",
            String.class);
        Exchange exchange = mockResult.getReceivedExchanges().get(0);
        assertNotNull(exchange);
        SSLSession session = exchange.getIn().getHeader(NettyConstants.NETTY_SSL_SESSION, SSLSession.class);
        assertNotNull(session);
    }

    @Configuration
    public static class TestConfiguration {

        @Bean(name = "sslContextParameters")
        public SSLContextParameters getSSLContextParameters() throws IOException {
            Properties authProperties = loadAuthProperties();
            SSLContextParameters scp = new SSLContextParameters();
            scp.setSecureSocketProtocol(TLSv13);
            KeyStoreParameters ksp = new KeyStoreParameters();
            ksp.setResource(this.getClass().getClassLoader().getResource("keystore.jks").toString());
            ksp.setPassword(authProperties.getProperty("password"));
            KeyManagersParameters kmp = new KeyManagersParameters();
            kmp.setKeyStore(ksp);
            kmp.setKeyPassword(authProperties.getProperty("password"));
            TrustManagersParameters tmp = new TrustManagersParameters();
            tmp.setKeyStore(ksp);
            scp.setKeyManagers(kmp);
            scp.setTrustManagers(tmp);
            return scp;
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("netty:tcp://localhost:5150")
                    .to("mock:result");
                }
            };
        }
    }
}
