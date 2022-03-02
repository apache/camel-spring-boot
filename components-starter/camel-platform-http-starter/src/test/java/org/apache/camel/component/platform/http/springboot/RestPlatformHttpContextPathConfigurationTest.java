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


import static org.hamcrest.CoreMatchers.containsString;


import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

import org.junit.jupiter.api.Test;


import io.restassured.RestAssured;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        RestPlatformHttpContextPathConfigurationTest.class,
        RestPlatformHttpContextPathConfigurationTest.TestConfiguration.class
    }
)
public class RestPlatformHttpContextPathConfigurationTest {
    
    static int port = AvailablePortFinder.getNextAvailable();

   
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.getRegistry().bind(PlatformHttpConstants.PLATFORM_HTTP_ENGINE_FACTORY, new JettyCustomPlatformHttpEngine());

                
                JettyServerTest server = new JettyServerTest(port);

                context.getRegistry().bind(JettyServerTest.JETTY_SERVER_NAME, server);
                try {
                    server.start();
                } catch (Exception e) {
                    
                    e.printStackTrace();
                }

            }
            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here
            }

        };
    }
    
    @Test
    public void contextPath() {
        RestAssured.given()
                .port(port)
                .get("/rest/get")
                .then()
                .body(containsString("GET: /get"));

        RestAssured.given()
                .port(port)
                .contentType("text/plain")
                .post("/rest/post")
                .then()
                .body(containsString("POST: /post"));
    }
    
    

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    restConfiguration()
                            .component("platform-http")
                            .contextPath("/rest");

                    rest()
                            .get("/get").to("direct:get")
                            .post("/post").consumes("text/plain").produces("text/plain").to("direct:post");

                    from("direct:get")
                            .setBody(constant("GET: /get"));
                    from("direct:post")
                            .setBody(constant("POST: /post"));

                }
            };
        }
    }
}
