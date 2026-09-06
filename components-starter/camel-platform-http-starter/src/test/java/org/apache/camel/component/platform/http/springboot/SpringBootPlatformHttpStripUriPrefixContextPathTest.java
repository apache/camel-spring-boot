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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.springboot.HttpComponentAutoConfiguration;
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

/**
 * Verifies that stripUriPrefix keeps working correctly when the Spring Boot application also has a non-default
 * {@code server.servlet.context-path} configured (see {@link SpringBootPlatformHttpContextPathTest} for the same setup
 * applied to plain platform-http/REST DSL routes). The servlet context path is already excluded from CamelHttpPath by
 * the servlet container itself, before Camel sees the request; stripUriPrefix then additionally removes the
 * platform-http consumer's own registered path on top of that - so a client request under both prefixes ends up with
 * neither at the backend.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpStripUriPrefixContextPathTest.class,
        SpringBootPlatformHttpStripUriPrefixContextPathTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class,
        HttpComponentAutoConfiguration.class }, properties = { "server.servlet.context-path=/test" })
public class SpringBootPlatformHttpStripUriPrefixContextPathTest {

    @Autowired
    private Environment env;

    @Autowired
    private CamelContext camelContext;

    @BeforeEach
    void setUp() throws Exception {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);

        // the bridged target includes the context path, since that path prefix is required for every servlet
        // mapping of this application, backend included - only the platform-http consumer path is stripped by
        // stripUriPrefix
        if (camelContext.getRoute("reverse-proxy-strip") == null) {
            final String backend = "http://localhost:" + RestAssured.port + "/test";
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/reverse-proxy?matchOnUriPrefix=true&stripUriPrefix=true")
                            .routeId("reverse-proxy-strip").to(backend + "?bridgeEndpoint=true");
                }
            });
        }
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        public RouteBuilder backendRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/get").routeId("backend-get").setBody().simple("get:${header.CamelHttpQuery}");
                }
            };
        }
    }

    @Test
    void stripUriPrefixCombinesWithTheServletContextPath() {
        given().when().get("/test/reverse-proxy/get?arg1=val1").then().statusCode(200).body(equalTo("get:arg1=val1"));
    }
}
