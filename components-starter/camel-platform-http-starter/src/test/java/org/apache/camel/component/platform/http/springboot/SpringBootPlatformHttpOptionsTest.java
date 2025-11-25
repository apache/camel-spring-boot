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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpOptionsTest.class, SpringBootPlatformHttpOptionsTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
public class SpringBootPlatformHttpOptionsTest {

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Configuration
    public static class TestConfiguration {
        @Bean
        public RouteBuilder springBootPlatformHttpRestDSLRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    restConfiguration().component("platform-http").enableCORS(false);

                    rest("/rest")
                        .get().to("direct:get")
                        .post().to("direct:post");
                    from("direct:get").transform().constant("get");
                    from("direct:post").transform().constant("post");

                    from("platform-http:/restRestricted?httpMethodRestrict=GET,PUT").transform().constant("restricted");
                    from("platform-http:/restDefault").transform().constant("default");
                }
            };
        }
    }

    @Test
    public void optionsRest() {
        given()
            .when()
            .options("/rest")
            .then()
            .statusCode(200)
            .header("Allow", containsString("OPTIONS"))
            .header("Allow", containsString("GET"))
            .header("Allow", containsString("POST"))
            .header("Allow", containsString("HEAD"))
            .header("Allow", not(containsString("PATCH")))
            .header("Allow", not(containsString("PUT")))
            .header("Allow", not(containsString("DELETE")));
    }

    @Test
    public void optionsRestRestricted() {
        given()
            .when()
            .options("/restRestricted")
            .then()
            .statusCode(200)
            .header("Allow", containsString("OPTIONS"))
            .header("Allow", containsString("GET"))
            .header("Allow", not(containsString("POST")))
            .header("Allow", containsString("HEAD"))
            .header("Allow", not(containsString("PATCH")))
            .header("Allow", containsString("PUT"))
            .header("Allow", not(containsString("DELETE")));
    }

    @Test
    public void optionsRestDefault() {
        given()
            .when()
            .options("/restDefault")
            .then()
            .statusCode(200)
            .header("Allow", containsString("OPTIONS"))
            .header("Allow", containsString("GET"))
            .header("Allow", containsString("POST"))
            .header("Allow", containsString("HEAD"))
            .header("Allow", containsString("PATCH"))
            .header("Allow", containsString("PUT"))
            .header("Allow", containsString("DELETE"));
    }

}
