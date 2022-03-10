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
package org.apache.camel.component.telegram.springboot;


import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.telegram.TelegramConfiguration;
import org.apache.camel.component.telegram.TelegramEndpoint;
import org.apache.camel.component.telegram.TelegramProxyType;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        TelegramConfigurationTest.class,
        TelegramConfigurationTest.TestConfiguration.class
    }
)
public class TelegramConfigurationTest extends TelegramTestSupport {

    
    
    
    @EndpointInject("mock:telegram")
    private MockEndpoint endpoint;

    @Test
    public void testChatBotResult() {
        TelegramEndpoint endpoint = (TelegramEndpoint)context.getEndpoints().stream()
                .filter(e -> e instanceof TelegramEndpoint).findAny().get();
        TelegramConfiguration config = endpoint.getConfiguration();

        assertEquals("bots", config.getType());
        assertEquals("mock-token", config.getAuthorizationToken());
        assertEquals("12345", config.getChatId());
        assertEquals(2000L, endpoint.getDelay());
        assertEquals(Integer.valueOf(10), config.getTimeout());
        assertEquals(Integer.valueOf(60), config.getLimit());
        assertEquals("127.0.0.1", config.getProxyHost());
        assertEquals(Integer.valueOf(1234), config.getProxyPort());
        assertEquals(TelegramProxyType.SOCKS5, config.getProxyType());
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

                    from("direct:telegram")
                            .to("telegram:bots/?authorizationToken=mock-token&chatId=12345&delay=2000&timeout=10&limit=60&proxyHost=127.0.0.1&proxyPort=1234&proxyType=SOCKS5");
                }
            };
        }

    }
    
    
}
