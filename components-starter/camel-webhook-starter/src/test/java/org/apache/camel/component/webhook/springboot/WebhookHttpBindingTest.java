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


import java.util.Arrays;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.webhook.WebhookConfiguration;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        WebhookHttpBindingTest.class,
        WebhookHttpBindingTest.TestConfiguration.class
    }
)
public class WebhookHttpBindingTest {
    
    private static int port;

   
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:authors")
    MockEndpoint mock;
    
    @BeforeAll
    public static void initPort() {
        port = AvailablePortFinder.getNextAvailable();
    }
    
    @Bean("wb-delegate-component")
    private TestComponent getTestComponent() {
        return new TestComponent(endpoint -> {
            endpoint.setWebhookHandler(proc -> ex -> {
                ex.getMessage().setBody("webhook");
                proc.process(ex);
            });
            endpoint.setWebhookMethods(() -> Arrays.asList("POST", "PUT"));
        });
    }

    @Test
    public void testWrapper() {
        String result = template.requestBody("netty-http:http://localhost:" + port
                                             + WebhookConfiguration.computeDefaultPath("wb-delegate://xx"),
                "", String.class);
        assertEquals("msg: webhook", result);

        result = template.requestBodyAndHeader("netty-http:http://localhost:" + port
                                               + WebhookConfiguration.computeDefaultPath("wb-delegate://xx"),
                "", Exchange.HTTP_METHOD, "PUT", String.class);
        assertEquals("msg: webhook", result);
    }

    @Test
    public void testGetError() {
        assertThrows(CamelExecutionException.class,
                () -> template.requestBodyAndHeader("netty-http:http://localhost:" + port, "",
                        Exchange.HTTP_METHOD, "GET", String.class));
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

                    from("webhook:wb-delegate://xx")
                            .transform(body().prepend("msg: "));

                }
            };
        }
    }
}
