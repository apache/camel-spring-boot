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
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Path variables must be taken from the path Spring matched the request against, so a percent encoded segment or a
 * segment carrying matrix parameters is reported decoded and without its matrix parameters, as the vertx engine does.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { CamelAutoConfiguration.class,
                SpringBootPlatformHttpPathVariableTest.class,
                SpringBootPlatformHttpPathVariableTest.TestConfiguration.class,
                PlatformHttpComponentAutoConfiguration.class,
                SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpPathVariableTest {

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);
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
        public WebSecurityCustomizer allowMatrixParametersCustomizer() {
            // the strict firewall rejects matrix parameters by default
            StrictHttpFirewall firewall = new StrictHttpFirewall();
            firewall.setAllowSemicolon(true);
            return web -> web.httpFirewall(firewall);
        }

        @Bean
        public RouteBuilder pathVariableRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/greeting/{name}")
                            .transform().simple("${header.name}|${header.CamelHttpPath}");

                    rest("/rest")
                            .get("/{name}").to("direct:restName");

                    from("direct:restName")
                            .setBody().simple("${header.name}");
                }
            };
        }
    }

    @Test
    public void testPlainPathVariable() {
        given()
                .when()
                .get("/greeting/Camel")
                .then()
                .statusCode(200)
                .body(equalTo("Camel|/greeting/Camel"));
    }

    @Test
    public void testPercentEncodedPathVariable() {
        // Spring matched /greeting/admin, so the header must be admin, while CamelHttpPath stays the raw path
        given()
                .urlEncodingEnabled(false)
                .when()
                .get("/greeting/%61dmin")
                .then()
                .statusCode(200)
                .body(equalTo("admin|/greeting/%61dmin"));
    }

    @Test
    public void testPercentEncodedSpaceInPathVariable() {
        given()
                .urlEncodingEnabled(false)
                .when()
                .get("/greeting/John%20Doe")
                .then()
                .statusCode(200)
                .body(equalTo("John Doe|/greeting/John%20Doe"));
    }

    @Test
    public void testMatrixParameterInPathVariable() {
        // Spring matched /greeting/name, the matrix parameter is not part of the segment it matched
        given()
                .urlEncodingEnabled(false)
                .when()
                .get("/greeting/name;v=1")
                .then()
                .statusCode(200)
                .body(equalTo("name|/greeting/name;v=1"));
    }

    @Test
    public void testRestDslPercentEncodedPathVariable() {
        given()
                .urlEncodingEnabled(false)
                .when()
                .get("/rest/%61dmin")
                .then()
                .statusCode(200)
                .body(equalTo("admin"));
    }
}
