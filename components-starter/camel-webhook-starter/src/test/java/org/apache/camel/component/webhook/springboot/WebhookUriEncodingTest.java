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
package org.apache.camel.component.webhook.springboot;



import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        WebhookUriEncodingTest.class,
        WebhookUriEncodingTest.TestConfiguration.class
    }
)
public class WebhookUriEncodingTest {
    
    private static int port;

   
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:authors")
    MockEndpoint mock;
    
    @BeforeAll
    public static void initPort() {
        port = AvailablePortFinder.getNextAvailable();
    }
    
    @Test
    public void test() {
        Exchange exchange = template.send("netty-http:http://localhost:" + port + "/base/uri", ExchangePattern.InOut,
                e -> e.getMessage().setBody(""));
        Message result = exchange.getMessage();
        assertEquals("msg: webhook", result.getBody(String.class));
        assertEquals("hello} world", result.getHeader("foo"));
        assertEquals("hello} world", result.getHeader("bar"));
    }
    
    @Bean("wb-delegate-component")
    private TestComponent getTestComponent() {
        return new TestComponent(endpoint -> {
            endpoint.setWebhookHandler(proc -> ex -> {
                ex.getMessage().setBody("webhook");
                ex.getMessage().setHeader("foo", endpoint.getFoo());
                ex.getMessage().setHeader("bar", endpoint.getBar());
                proc.process(ex);
            });
        });
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
                public void configure() throws Exception {

                    restConfiguration()
                            .host("0.0.0.0")
                            .port(port);

                    from("webhook:wb-delegate://xx?webhookBasePath=/base&webhookPath=/uri&foo=hello} world&bar=RAW(hello} world)")
                            .transform(body().prepend("msg: "));

                }
            };
        }
    }
}
