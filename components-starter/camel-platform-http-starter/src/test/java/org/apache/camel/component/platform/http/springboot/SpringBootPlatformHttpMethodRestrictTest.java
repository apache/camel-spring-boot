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

@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpMethodRestrictTest.class,
        SpringBootPlatformHttpMethodRestrictTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpMethodRestrictTest {

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void restrictedMethodIsServed() {
        given()
                .get("/restricted")
                .then()
                .statusCode(200)
                .body(equalTo("restricted"));
    }

    @Test
    void nonRestrictedMethodIsRejected() {
        given()
                .post("/restricted")
                .then()
                .statusCode(405);
    }

    @Test
    void restrictWithWhitespaceServesListedMethods() {
        given()
                .get("/restricted-spaced")
                .then()
                .statusCode(200)
                .body(equalTo("spaced"));

        given()
                .post("/restricted-spaced")
                .then()
                .statusCode(200)
                .body(equalTo("spaced"));
    }

    @Test
    void restrictWithWhitespaceMustNotWidenToAllMethods() {
        // before the fix an unresolvable verb (" POST") registered a mapping
        // without any method restriction, so DELETE was served as well
        given()
                .delete("/restricted-spaced")
                .then()
                .statusCode(405);
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
                    from("platform-http:/restricted?httpMethodRestrict=GET")
                            .setBody().constant("restricted");

                    from("platform-http:/restricted-spaced?httpMethodRestrict=GET, POST")
                            .setBody().constant("spaced");
                }
            };
        }
    }
}
