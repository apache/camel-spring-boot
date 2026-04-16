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

import io.restassured.RestAssured;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpResponseCodeTest.class,
        SpringBootPlatformHttpResponseCodeTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpResponseCodeTest {

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void responseCodeViaHeader() {
        given()
                .get("/response-code-299")
                .then()
                .statusCode(299);
    }

    @Test
    void code204Null() {
        given()
                .get("/null-body")
                .then()
                .statusCode(204);
    }

    @Test
    void code204EmptyString() {
        given()
                .get("/empty-string-body")
                .then()
                .statusCode(204);
    }

    @Test
    void code204SomeString() {
        given()
                .get("/some-string")
                .then()
                .statusCode(200)
                .body(equalTo("No Content"));
    }

    @Test
    void code200EmptyString() {
        given()
                .get("/empty-string-200")
                .then()
                .statusCode(200)
                .body(equalTo(""));
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
                    from("platform-http:/response-code-299?httpMethodRestrict=GET")
                            .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(299);

                    from("platform-http:/null-body")
                            .setBody().constant(null);

                    from("platform-http:/empty-string-body")
                            .setBody().constant("");

                    from("platform-http:/some-string")
                            .setBody().constant("No Content");

                    from("platform-http:/empty-string-200")
                            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                            .setBody().constant("");
                }
            };
        }
    }
}
