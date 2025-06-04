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

import jakarta.servlet.http.Cookie;

import io.restassured.RestAssured;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.cookie.CookieConfiguration;
import org.apache.camel.component.platform.http.cookie.CookieHandler;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
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

import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpCookiesTest.class, SpringBootPlatformHttpCookiesTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
public class SpringBootPlatformHttpCookiesTest {

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
                    getCamelContext().getStreamCachingStrategy().setSpoolEnabled(true);
                    from("platform-http:/addCookie?useCookieHandler=true")
                            .process(exchange -> {
                                exchange.getProperty(Exchange.COOKIE_HANDLER, CookieHandler.class).addCookie("foo", "bar");
                            })
                            .setBody().constant("add");

                    from("platform-http:/removeCookie?useCookieHandler=true")
                            .process(exchange -> {
                                exchange.getProperty(Exchange.COOKIE_HANDLER, CookieHandler.class).addCookie("foo", "bar");
                                exchange.getProperty(Exchange.COOKIE_HANDLER, CookieHandler.class).removeCookie("foo");
                            })
                            .setBody().constant("remove");

                    from("platform-http:/getCookieValue?useCookieHandler=true")
                            .process(exchange -> {
                                exchange.getProperty(Exchange.COOKIE_HANDLER, CookieHandler.class).addCookie("foo", "bar");
                                exchange.getProperty(Exchange.COOKIE_HANDLER, CookieHandler.class).getCookieValue("foo");
                            })
                            .setBody().constant("get");

                    from("platform-http:/replace")
                            .process(exchange -> {
                                PlatformHttpMessage message = (PlatformHttpMessage) exchange.getMessage();
                                assertEquals(1, message.getRequest().getCookies().length);

                                Cookie cookie = new Cookie("XSRF-TOKEN", "88533580000c314");
                                cookie.setPath("/");
                                message.getResponse().addCookie(cookie);
                            })
                            .setBody().constant("replace");

                    from("platform-http:/echo")
                            .setBody().constant("echo");
                }
            };
        }
    }

    @Test
    public void testAddCookie() {
        given()
                .when()
                .get("/addCookie")
                .then()
                .statusCode(200)
                .cookie("foo",
                        detailedCookie()
                                .value("bar")
                                .path(CookieConfiguration.DEFAULT_PATH)
                                .domain((String) null)
                                .sameSite(CookieConfiguration.DEFAULT_SAME_SITE.getValue()))
                .body(equalTo("add"));
    }

    @Test
    public void testRemoveCookie() {
        given()
                .when()
                .get("/removeCookie")
                .then()
                .statusCode(200)
                .cookie("foo",
                        detailedCookie()
                                .maxAge(equalTo(0L)))
                .body(equalTo("remove"));
    }

    @Test
    public void testGetCookieValue() {
        given()
                .when()
                .get("/getCookieValue")
                .then()
                .statusCode(200)
                .cookie("foo",
                        detailedCookie()
                                .value("bar"))
                .body(equalTo("get"));
    }

    @Test
    public void replaceCookie() {
        given()
                .header("cookie", "XSRF-TOKEN=c359b44aef83415")
                .when()
                .get("/replace")
                .then()
                .statusCode(200)
                .header("set-cookie", "XSRF-TOKEN=88533580000c314; Path=/")
                .body(equalTo("replace"));
    }

    @Test
    public void echoCookie() {
        given()
                .header("cookie", "echo=cookie")
                .when()
                .get("/echo")
                .then()
                .statusCode(200)
                .header("cookie", startsWith("echo=cookie"))
                .body(equalTo("echo"));
    }

}
