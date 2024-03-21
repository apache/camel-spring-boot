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
package org.apache.camel.component.vertx.http.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testing camel.ssl.config and camel.ssl configuration using global ssl context.
 */
@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(classes = { CamelAutoConfiguration.class,
        VertxWebsocketSSLGlobalTest.TestConfiguration.class }, properties = { "debug=false",
                "camel.component.vertx-websocket.use-global-ssl-context-parameters=true",
                // camel.ssl.config
                "camel.ssl.config.cert-alias=web", "camel.ssl.config.key-managers.key-password=changeit",
                "camel.ssl.config.key-managers.key-store.resource=/keystore.p12",
                "camel.ssl.config.key-managers.key-store.password=changeit",
                "camel.ssl.config.key-managers.key-store.type=PKCS12",
                // camel.ssl
                "camel.ssl.trust-managers.key-store.resource=/cacerts",
                "camel.ssl.trust-managers.key-store.password=changeit", "camel.ssl.trust-managers.key-store.type=jks" })
public class VertxWebsocketSSLGlobalTest {
    private static int port;

    @Autowired
    private CamelContext camelContext;

    @BeforeAll
    public static void init() {
        port = AvailablePortFinder.getNextAvailable();
    }

    @Test
    void testConsumeAsSecureClient() throws Exception {
        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Hello world");

        ProducerTemplate template = camelContext.createProducerTemplate();
        template.sendBody("direct:start", "world");

        mockEndpoint.assertIsSatisfied();
    }

    @Configuration
    static class TestConfiguration {

        @Autowired
        private SSLContextParameters sslContext;

        @Autowired
        private CamelContext camelContext;

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    // serverSSLParameters configured via vertx-websocket.use-global-ssl-context-parameters prop
                    camelContext.getRegistry().bind("clientSSLParameters", sslContext);

                    from("direct:start")
                            .toF("vertx-websocket:localhost:%d/echo?sslContextParameters=#clientSSLParameters", port);

                    fromF("vertx-websocket:localhost:%d/echo", port).setBody(simple("Hello ${body}")).to("mock:result");
                }
            };
        }
    }

}
