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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpRequestTimeoutTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, },
        properties = {"camel.component.platform-http.request-timeout=10"})
@AutoConfigureMockMvc
public class SpringBootPlatformHttpRequestTimeoutTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestConfiguration testConfiguration;

    @Test
    public void testGetAsync() {
        testConfiguration.routeRelease = new CompletableFuture<>();
        try {
            assertTimeoutResponse("/slow-get");
        } finally {
            testConfiguration.routeRelease.complete(null);
        }
    }

    @Test
    public void testInterruptedRoute() {
        testConfiguration.interruptibleRouteRelease = new CountDownLatch(1);
        try {
            assertTimeoutResponse("/slow-interruptible-get");
        } finally {
            testConfiguration.interruptibleRouteRelease.countDown();
        }
    }

    private void assertTimeoutResponse(String path) {
        ResponseEntity<String> response = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> restTemplate.getForEntity(path, String.class));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(503));
        Assertions.assertThat(response.getHeaders().getContentType())
                .satisfies(contentType -> Assertions.assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue());
        Assertions.assertThat(response.getBody()).contains("\"status\":503");
    }

    @Configuration
    public static class TestConfiguration {

        private volatile CompletableFuture<Void> routeRelease;
        private volatile CountDownLatch interruptibleRouteRelease;

        @Bean
        public RouteBuilder platformHttpRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/slow-get").id("slow-route")
                            .process(exchange -> routeRelease.join())
                            .setBody().constant("get");
                    from("platform-http:/slow-interruptible-get").id("slow-interruptible-route")
                            .process(exchange -> interruptibleRouteRelease.await())
                            .setBody().constant("get");
                }
            };
        }
    }


}
