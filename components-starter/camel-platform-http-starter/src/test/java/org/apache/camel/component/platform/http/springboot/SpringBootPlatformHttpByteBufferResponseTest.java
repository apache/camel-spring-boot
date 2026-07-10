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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.restassured.RestAssured;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Verifies that a ByteBuffer response body only writes the buffer's remaining window, not the whole backing array,
 * and that direct buffers (no backing array) are supported.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpByteBufferResponseTest.class,
        SpringBootPlatformHttpByteBufferResponseTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpByteBufferResponseTest {

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void slicedByteBufferWritesOnlyRemainingWindow() {
        given()
                .get("/bytebuffer-sliced")
                .then()
                .statusCode(200)
                .body(equalTo("hello"));
    }

    @Test
    void directByteBufferIsWritten() {
        given()
                .get("/bytebuffer-direct")
                .then()
                .statusCode(200)
                .body(equalTo("direct"));
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/bytebuffer-sliced")
                            .process(exchange -> {
                                byte[] data = "XXXhelloYYY".getBytes(StandardCharsets.UTF_8);
                                exchange.getMessage().setBody(ByteBuffer.wrap(data, 3, 5));
                                exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "text/plain");
                                exchange.getMessage().setHeader(Exchange.HTTP_CHUNKED, false);
                            });

                    from("platform-http:/bytebuffer-direct")
                            .process(exchange -> {
                                ByteBuffer direct = ByteBuffer.allocateDirect(6);
                                direct.put("direct".getBytes(StandardCharsets.UTF_8));
                                direct.flip();
                                exchange.getMessage().setBody(direct);
                                exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "text/plain");
                                exchange.getMessage().setHeader(Exchange.HTTP_CHUNKED, false);
                            });
                }
            };
        }
    }
}
