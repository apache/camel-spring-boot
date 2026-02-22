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
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class})
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {CamelAutoConfiguration.class,
        SpringBootPlatformHttpServerRequestValidationFalseTest.class, SpringBootPlatformHttpServerRequestValidationFalseTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class,})
public class SpringBootPlatformHttpServerRequestValidationFalseTest {

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    // *************************************
    // Config
    // *************************************
    @Configuration
    public static class TestConfiguration {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        public RouteBuilder servletPlatformHttpRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    PlatformHttpComponent phc = getContext().getComponent("platform-http", PlatformHttpComponent.class);
                    phc.setServerRequestValidation(false);

                    restConfiguration().component("platform-http")
                            .contextPath("/rest");

                    rest().post("/test")
                            .consumes("application/json")
                            .produces("application/json")
                            .to("direct:rest");

                    from("direct:rest")
                            .setBody(simple("Hello"));
                }
            };
        }
    }

    @Test
    void testServerRequestFalse() throws Exception {
        given()
                .body("<hello>World</hello>")
                .contentType("application/xml")
                .post("/rest/test")
                .then()
                .statusCode(200)
                .body(is("Hello"));

        given()
                .body("{ \"name\": \"jack\" }")
                .contentType("application/json")
                .accept("application/xml")
                .post("/rest/test")
                .then()
                .statusCode(200)
                .body(is("Hello"));
    }

}
