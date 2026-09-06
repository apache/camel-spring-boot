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
 * Integration test for the stripUriPrefix consumer option on the Spring Boot (servlet) platform-http engine. Combined
 * with matchOnUriPrefix and the http producer's bridgeEndpoint, it turns a platform-http route into a path-based
 * reverse proxy that forwards only the path relative to the consumer:
 *
 * <pre>
 * from("platform-http:/reverse-proxy?matchOnUriPrefix=true&amp;stripUriPrefix=true")
 *         .to("http://backend?bridgeEndpoint=true");
 * </pre>
 *
 * The "backend" is a second platform-http route on the same embedded server: WireMock's Spring Boot integration is
 * incompatible with this module's Spring Boot 4/Jetty version (see the disabled
 * {@link SpringBootPlatformHttpBridgedEndpointTest}). A request that was <em>not</em> stripped would not match the
 * {@code /get} backend route and would fall back into this very reverse-proxy consumer, so the successful response
 * below proves the consumer path was removed before the request was bridged.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpStripUriPrefixTest.class,
        SpringBootPlatformHttpStripUriPrefixTest.TestConfiguration.class, PlatformHttpComponentAutoConfiguration.class,
        SpringBootPlatformHttpAutoConfiguration.class, HttpComponentAutoConfiguration.class })
public class SpringBootPlatformHttpStripUriPrefixTest {

    @Autowired
    private Environment env;

    @Autowired
    private CamelContext camelContext;

    @BeforeEach
    void setUp() throws Exception {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);

        // the reverse proxy bridges to a real loopback HTTP call, so it needs the actual (random) server port and is
        // added once that port is known, rather than as a statically configured @Bean route
        if (camelContext.getRoute("reverse-proxy-strip") == null) {
            final String backend = "http://localhost:" + RestAssured.port;
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
                    // the "downstream" backend service, reachable only at its own path (not under /reverse-proxy)
                    from("platform-http:/get").routeId("backend-get").setBody().simple("get:${header.CamelHttpQuery}");
                }
            };
        }
    }

    @Test
    void reverseProxyStripsTheConsumerPathBeforeBridging() {
        given().when().get("/reverse-proxy/get?arg1=val1").then().statusCode(200).body(equalTo("get:arg1=val1"));
    }
}
