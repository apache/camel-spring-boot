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

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.http.ContentType;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.springboot.HttpComponentAutoConfiguration;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpBridgedEndpointTest.class, SpringBootPlatformHttpBridgedEndpointTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, HttpComponentAutoConfiguration.class})
@EnableWireMock(@ConfigureWireMock(portProperties = "customPort"))
public class SpringBootPlatformHttpBridgedEndpointTest {

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        WireMock.stubFor(get(urlPathEqualTo("/backend"))
                .willReturn(aResponse()
                        .withBody(
                                "{\"message\": \"Hello World\"}")));
    }

    @Configuration
    public static class TestConfiguration {

        @Value("${wiremock.server.baseUrl}")
        private String wiremockUrl;

        @Bean
        public RouteBuilder springBootPlatformHttpRestDSLRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    rest()
                            .get("mock").id("mock").to("direct:mock");

                    from("direct:mock")
                            .setHeader("wiremockUrl", () -> wiremockUrl)
                            .log("${headers}")
                            .toD("${headers.wiremockUrl}/backend?bridgeEndpoint=true");
                }
            };
        }
    }

    @Test
    public void bridgedEndpointTest() {
        final var proxyURI = "http://localhost:%s/mock".formatted(port);

        given()
                .contentType(ContentType.JSON)
                .when().get(proxyURI)
                .then()
                .statusCode(200)
                .body(containsString("{\"message\": \"Hello World\"}"));
    }
}
