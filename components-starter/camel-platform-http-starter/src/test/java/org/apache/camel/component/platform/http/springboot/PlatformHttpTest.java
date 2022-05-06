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

import java.util.Iterator;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.HttpEndpointModel;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import org.springframework.beans.factory.annotation.Autowired;
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
        PlatformHttpTest.class,
        PlatformHttpTest.TestConfiguration.class
    }
)
public class PlatformHttpTest {
    
    static int port = AvailablePortFinder.getNextAvailable();

    @Autowired
    private CamelContext context;
    


    
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
    public void testGet() throws Exception {
        given()
                .header("Accept", "application/json")
                .port(port)
                .expect()
                .statusCode(200)
                .when()
                .get("/get");
    }

    @Test
    public void testPost() {
        RequestSpecification request = RestAssured.given();
        request.port(port);
        request.body("test");
        Response response = request.get("/post");

        int statusCode = response.getStatusCode();
        assertEquals(200, statusCode);
        assertEquals("TEST", response.body().asString().trim());

        PlatformHttpComponent phc = context.getComponent("platform-http", PlatformHttpComponent.class);
        assertEquals(2, phc.getHttpEndpoints().size());
        Iterator<HttpEndpointModel> it = phc.getHttpEndpoints().iterator();
        assertEquals("/get", it.next().getUri());
        assertEquals("/post", it.next().getUri());
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
                    from("platform-http:/get")
                            .setBody().constant("get");
                    from("platform-http:/post")
                            .transform().body(String.class, b -> b.toUpperCase());
                }
            };
        }
    }
}
