/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.component.http.springboot;

import java.nio.charset.StandardCharsets;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.impl.bootstrap.HttpServer;
import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        HttpComponentTimeoutConverter.class,
        CamelAutoConfiguration.class,
        HttpComponentAutoConfiguration.class,
        HttpComponentTimeoutConverterTest.TestConfiguration.class
    },
    properties = {"camel.component.http.so-timeout = 30000"}
)
class HttpComponentTimeoutConverterTest {

    @Autowired
    ProducerTemplate template;
    @Autowired
    private CamelContext context;

    private static CamelContext currentContext;
    private static HttpServer localServer;

    private static String baseUrl;

    @BeforeAll
    public static void setUp() throws Exception {
        localServer = ServerBootstrap.bootstrap()
            .register("/checkSoTimeout", (request, response, ctx) -> {
                response.setCode(HttpStatus.SC_OK);
                assertNotNull(currentContext);
                response.setEntity(new StringEntity(String.valueOf(currentContext.getComponent("http", HttpComponent.class).getSoTimeout().toSeconds()), StandardCharsets.US_ASCII));
            })
            .create();
        localServer.start();
    
        baseUrl = "http://localhost:" + localServer.getLocalPort();
    }

    @AfterAll
    public static void tearDown() {
        if (localServer != null) {
            localServer.stop();
        }
    }

    @BeforeEach
    public void init() {
        currentContext = context;
    }

    @Test
    void checkSoTimeout() {
        Exchange exchange = template.request("direct:checkSoTimeout", exchange1 -> {});
        assertNotNull(exchange);
        assertNull(exchange.getException());
        Message out = exchange.getMessage();
        assertNotNull(out);
        assertEquals("30", out.getBody(String.class));
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:checkSoTimeout").to(baseUrl + "/checkSoTimeout");
                }
            };
        }
    }
}
