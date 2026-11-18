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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.ObservationView;
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
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * The route runs on an executor thread. With Spring Boot's spring.task.execution.propagate-context, that thread gets the
 * context of the request, so the route sees the Micrometer Observation that Spring MVC opened for it and the route spans
 * belong to the trace of the request.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = "spring.task.execution.propagate-context=true", classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpContextPropagationTest.class,
        SpringBootPlatformHttpContextPropagationTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpContextPropagationTest {

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void routeSeesTheObservationOfTheRequest() {
        given()
                .get("/observed")
                .then()
                .statusCode(200)
                .header("RouteThread", startsWith("task-"))
                .body(equalTo("http.server.requests GET /observed"));
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
        public RouteBuilder routeBuilder(ObservationRegistry observationRegistry) {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/observed")
                            .process(exchange -> {
                                exchange.getMessage().setHeader("RouteThread", Thread.currentThread().getName());
                                // the current observation may be a child of the request one (Spring Security adds
                                // its own), so look for the request up the chain. Its key values are only computed
                                // when it stops, so read the request it carries.
                                String found = "no observation of the request";
                                for (ObservationView view = observationRegistry.getCurrentObservation(); view != null;
                                     view = view.getContextView() instanceof Observation.Context ctx
                                             ? ctx.getParentObservation() : null) {
                                    if (view.getContextView() instanceof ServerRequestObservationContext ctx) {
                                        found = ctx.getName() + " " + ctx.getCarrier().getMethod() + " "
                                                + ctx.getCarrier().getRequestURI();
                                        break;
                                    }
                                }
                                exchange.getMessage().setBody(found);
                            });
                }
            };
        }
    }
}
