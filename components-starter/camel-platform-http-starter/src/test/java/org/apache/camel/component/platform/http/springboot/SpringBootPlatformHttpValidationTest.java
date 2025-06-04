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
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.spi.Method;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpValidationTest.class, SpringBootPlatformHttpValidationTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
public class SpringBootPlatformHttpValidationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    CamelContext camelContext;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder springBootPlatformHttpRestDSLRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    rest("/rest")
                            .post("/validate/body")
                            .clientRequestValidation(true)
                            .param().name("body").type(RestParamType.body).required(true).endParam()
                            .to("direct:rest");
                    from("direct:rest")
                            .setBody(simple("Hello ${body}"));

                    from("platform-http:/echo")
                            .setBody().simple("${body}");

                    from("platform-http:/test")
                            .setBody().simple("Hello ${body[method]}");

                    rest("/invalidContentTypeClientRequestValidation")
                            .clientRequestValidation(true)
                            .bindingMode(RestBindingMode.json)
                            .post("/validate/body")
                            .consumes("text/plain")
                            .produces("application/json")
                            .to("direct:invalidContentTypeClientRequestValidation");
                    from("direct:invalidContentTypeClientRequestValidation")
                            .setBody(simple("Hello ${body}"));
                }
            };
        }
    }

    @Test
    public void requestValidation() throws Exception {
        given()
                .when()
                .post("/rest/validate/body")
                .then()
                .statusCode(400)
                .body(is("The request body is missing."));

        given()
                .body(" ")
                .when()
                .post("/rest/validate/body")
                .then()
                .statusCode(400)
                .body(is("The request body is missing."));

        given()
                .body("Camel Platform HTTP Vert.x")
                .when()
                .post("/rest/validate/body")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Platform HTTP Vert.x"));
    }

    @Test
    public void requestBodyAllowed() {
        for (Method method : Method.values()) {
            if (method == Method.TRACE || method == Method.CONNECT) {
                // These methods are not supported OOB in tomcat
                continue;
            }

            ValidatableResponse validatableResponse = given()
                    .contentType(ContentType.JSON)
                    .when()
                    .body("{\"method\": \"" + method + "\"}")
                    .request(method.name(), "/echo")
                    .then()
                    .statusCode(200);

            Matcher<String> expectedBody;
            if (method.equals(Method.HEAD)) {
                // HEAD response body is ignored
                validatableResponse.body(emptyString());
            } else {
                validatableResponse.body("method", equalTo(method.name()));
            }
        }
    }

    @Test
    public void requestBodyAllowedFormUrlEncoded() {
        final List<Method> methodsWithBodyAllowed = List.of(Method.POST, Method.PUT, Method.PATCH, Method.DELETE);

        given()
                .when()
                .contentType(ContentType.URLENC)
                .body("method=" + "PUT" + "&test=value")
                .request("PUT", "/test")
                .then()
                .statusCode(200)
                .body(equalTo("Hello PUT"));

        for (Method method : Method.values()) {
            if (method == Method.TRACE || method == Method.CONNECT) {
                // These methods are not supported OOB in tomcat
                continue;
            }

            if (methodsWithBodyAllowed.contains(method)) {
                given()
                        .when()
                        .contentType(ContentType.URLENC)
                        .body("method=" + method + "&test=value")
                        .request(method.name(), "/test")
                        .then()
                        .statusCode(200)
                        .body(equalTo("Hello " + method));
            } else {
                given()
                        .when()
                        .contentType(ContentType.URLENC)
                        .body(method)
                        .request(method.name(), "/test")
                        .then()
                        .statusCode(500);
            }
        }
    }

    @Test
    public void invalidContentTypeClientRequestValidation() {
        given()
                .when()
                .body("{\"name\": \"Donald\"}")
                .contentType("application/json")
                .post("/invalidContentTypeClientRequestValidation/validate/body")
                .then()
                .statusCode(415);
    }
}
