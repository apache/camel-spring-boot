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
import org.apache.camel.CamelContext;
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
 * Verifies that stopping a route unregisters its Spring MVC mapping (404 instead of dispatching to a stopped
 * consumer) and that restarting the route registers it again.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpRouteLifecycleTest.class,
        SpringBootPlatformHttpRouteLifecycleTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpRouteLifecycleTest {

    @Autowired
    private Environment env;

    @Autowired
    CamelContext camelContext;

    @BeforeEach
    void setUp() {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void stoppedRouteIsUnregistered() throws Exception {
        given().get("/lifecycle").then().statusCode(200).body(equalTo("alive"));

        camelContext.getRouteController().stopRoute("lifecycle-route");
        given().get("/lifecycle").then().statusCode(404);

        camelContext.getRouteController().startRoute("lifecycle-route");
        given().get("/lifecycle").then().statusCode(200).body(equalTo("alive"));
    }

    @Test
    void stoppingRouteDoesNotUnregisterOtherConsumerOnSamePath() throws Exception {
        given().get("/shared").then().statusCode(200).body(equalTo("shared-get"));
        given().post("/shared").then().statusCode(200).body(equalTo("shared-post"));

        camelContext.getRouteController().stopRoute("shared-get");
        try {
            // the POST consumer of the same path must keep working
            given().post("/shared").then().statusCode(200).body(equalTo("shared-post"));
        } finally {
            camelContext.getRouteController().startRoute("shared-get");
        }
        given().get("/shared").then().statusCode(200).body(equalTo("shared-get"));
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
                    from("platform-http:/lifecycle").routeId("lifecycle-route")
                            .setBody().constant("alive");

                    from("platform-http:/shared?httpMethodRestrict=GET").routeId("shared-get")
                            .setBody().constant("shared-get");

                    from("platform-http:/shared?httpMethodRestrict=POST").routeId("shared-post")
                            .setBody().constant("shared-post");
                }
            };
        }
    }
}
