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
package org.apache.camel.component.platform.http.springboot;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.component.platform.http.spi.PlatformHttpConsumer;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.http.common.DefaultHttpBinding;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;

/**
 * Verifies that afterProcess() does NOT call sendError(500) when
 * writeResponse() throws AsyncRequestNotUsableException.
 *
 * When Spring's async timeout fires, the timeout handler already sends 503.
 * Camel's executor thread still runs and afterProcess() catches the write
 * failure. The current (buggy) code calls sendError(500) which overwrites
 * the 503. The fix should detect AsyncRequestNotUsableException and skip
 * the sendError(500) fallback.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpAsyncNotUsableTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
@AutoConfigureRestTestClient
public class SpringBootPlatformHttpAsyncNotUsableTest {

    @Autowired
    RestTestClient restTestClient;

    @Test
    public void testAsyncNotUsableShouldNotOverwriteWith500() throws Exception {
        EntityExchangeResult<String> result = restTestClient.get().uri("/async-not-usable")
                .exchange()
                .expectBody(String.class)
                .returnResult();

        // When writeResponse() throws AsyncRequestNotUsableException,
        // afterProcess() should NOT call sendError(500).
        // In a real timeout scenario, Spring's timeout handler already sent 503.
        // Overwriting with 500 is the bug we're fixing.
        Assertions.assertThat(result.getStatus().value()).isNotEqualTo(500);
    }

    @Test
    public void testRegularIOExceptionStillReturns500() throws Exception {
        EntityExchangeResult<String> result = restTestClient.get().uri("/io-error")
                .exchange()
                .expectBody(String.class)
                .returnResult();

        // Regular IOExceptions during writeResponse() should still produce 500.
        // This is the existing correct behavior — only AsyncRequestNotUsableException
        // should be treated differently.
        Assertions.assertThat(result.getStatus().value()).isEqualTo(500);
    }

    @Configuration
    public static class TestConfiguration {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean(name = "platform-http-engine")
        public PlatformHttpEngine asyncNotUsableEngine(Environment env) {
            int port = Integer.parseInt(env.getProperty("server.port", "8080"));
            return new AsyncNotUsableEngine(port);
        }

        @Bean
        public RouteBuilder servletPlatformHttpRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/async-not-usable")
                            .setBody().constant("ok");
                    from("platform-http:/io-error")
                            .setBody().constant("ok");
                }
            };
        }
    }

    private static class AsyncNotUsableBinding extends DefaultHttpBinding {

        @Override
        public void writeResponse(Exchange exchange, HttpServletResponse response) throws IOException {
            String uri = exchange.getMessage().getHeader(Exchange.HTTP_URI, String.class);
            if ("/async-not-usable".equals(uri)) {
                throw new AsyncRequestNotUsableException("Response not usable after async request completion");
            } else if ("/io-error".equals(uri)) {
                throw new IOException("Simulated IO error");
            } else {
                super.writeResponse(exchange, response);
            }
        }
    }

    private static class AsyncNotUsableEngine extends SpringBootPlatformHttpEngine {

        public AsyncNotUsableEngine(int port) {
            super(port);
        }

        @Override
        public PlatformHttpConsumer createConsumer(PlatformHttpEndpoint endpoint, Processor processor) {
            SpringBootPlatformHttpConsumer answer = new SpringBootPlatformHttpConsumer(endpoint, processor);
            answer.setBinding(new AsyncNotUsableBinding());
            return answer;
        }
    }
}
