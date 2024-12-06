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
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpCorsCredentialsTest.class, SpringBootPlatformHttpCorsCredentialsTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
public class SpringBootPlatformHttpCorsCredentialsTest {

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
                    restConfiguration().component("platform-http")
                        .enableCORS(true)
                        .corsAllowCredentials(true)
                        .corsHeaderProperty("Access-Control-Allow-Origin", "http://custom.origin.springboot");

                    rest("/rest")
                        .get().to("direct:restGet")
                        .post().consumes("application/json").to("direct:restPost");

                    from("direct:restPost")
                        .transform().constant("corsPost");

                    from("direct:restGet")
                        .transform().constant("corsGet");
                }
            };
        }
    }

    @Test
    public void get() {
        given()
            .when()
            .get("/rest")
            .then()
            .statusCode(200)
            .body(equalTo("corsGet"));
    }

    @Test
    public void post() {
        given()
            .header("Content-type","application/json")
            .when()
            .post("/rest")
            .then()
            .statusCode(200)
            .body(equalTo("corsPost"));
    }

    @Test
    public void options() {
        final String origin = "http://custom.origin.springboot";
        final String method = "POST";
        final String headers = "X-Requested-With";

        given()
            .header("Origin", origin)
            .header("Access-Control-Request-Method", method)
            .header("Access-Control-Request-Headers", headers)
            .when()
            .options("/rest")
            .then()
            .statusCode(200)
            .header("Access-Control-Allow-Origin", origin)
            .header("Access-Control-Allow-Methods", containsString(method))
            .header("Access-Control-Allow-Headers", containsString(headers))
            .header("Access-Control-Allow-Credentials", "true")
            .header("Access-Control-Max-Age", RestConfiguration.CORS_ACCESS_CONTROL_MAX_AGE)
            .body(emptyString());
    }

    @Test
    public void getForbiddenOrigin() {
        final String origin = "http://custom2.origin.springboot";

        given()
            .header("Origin", origin)
            .when()
            .get("/rest")
            .then()
            .statusCode(403);
    }

    @Test
    public void postForbiddenOrigin() {
        final String origin = "http://custom2.origin.springboot";

        given()
            .header("Origin", origin)
            .header("Content-type","application/json")
            .when()
            .post("/rest")
            .then()
            .statusCode(403);
    }

    @Test
    public void optionsForbiddenOrigin() {
        final String origin = "http://custom2.origin.springboot";

        given()
            .header("Origin", origin)
            .when()
            .options("/rest")
            .then()
            .header("Access-Control-Allow-Origin", not(containsString(origin)));
    }

    @Test
    public void optionsForbiddenMethod() {
        String method = "DELETE";

        given()
            .header("Access-Control-Request-Method", method)
            .when()
            .options("/rest")
            .then()
            .header("Access-Control-Allow-Methods", not(containsString(method)));
    }

    @Test
    public void optionsForbiddenHeader() {
        String header = "X-Custom-Header";

        given()
            .header("Access-Control-Request-Headers", header)
            .when()
            .options("/rest")
            .then()
            .header("Access-Control-Allow-Headers", not(containsString(header)));
    }

    @Test
    public void optionNonCors() {
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
    
}
