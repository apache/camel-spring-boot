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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.apache.camel.util.IOHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpTest.class, PlatformHttpStreamingTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class PlatformHttpStreamingTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    @Test
    void testStreamingWithStringRequestAndResponseBody() {
        String requestBody = "Spring Boot Platform HTTP";
        given().body(requestBody).post("/streaming").then().statusCode(200).body(is("Hello " + requestBody));
    }

    @Test
    void testNonStreamingWithStringRequestAndResponseBody() {
        String requestBody = "Spring Boot Platform HTTP";
        given().body(requestBody).post("/nonStreaming").then().statusCode(200).body(is("Hello " + requestBody));
    }

    @Test
    void testStreamingWithFileRequestAndResponseBody() throws Exception {
        Path testFile = null;
        try {
            testFile = Files.createTempFile("platform-http-testing", "txt");
            String content = "Hello World";
            Files.writeString(testFile, content);
            given().body(testFile.toFile()).post("/streamingFile").then().statusCode(200).body(is(content));
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void testNonStreamingWithFileRequestAndResponseBody() throws Exception {
        Path testFile = null;
        try {
            testFile = Files.createTempFile("platform-http-testing", "txt");
            String content = "Hello World";
            Files.writeString(testFile, content);
            given().body(testFile.toFile()).post("/nonStreamingFile").then().statusCode(200).body(is(content));
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void testStreamingWithFormUrlEncodedBody() throws Exception {
        given().contentType(ContentType.URLENC).formParam("foo", "bar")
                .post("/streamingUrlEncoded")
                .then().statusCode(200).body(is("foo=bar"));
    }

    @Test
    void testNonStreamingWithFormUrlEncodedBody() throws Exception {
        given().contentType(ContentType.URLENC).formParam("foo", "bar")
                .post("/nonStreamingUrlEncoded")
                .then().statusCode(200).body(is("foo=bar"));
    }

    @Test
    void testHeaderAndBodyWithFormUrlEncodedBody() throws Exception {
        given().contentType(ContentType.URLENC).formParam("foo", "bar")
                .post("/headerAndBodyUrlEncoded")
                .then()
                .statusCode(200)
                .header("foo", "bar")
                .header("BodyClass", containsString("HashMap"))
                .body(is("{foo=bar}"));
    }

    @Test
    void testOnlyHeaderWithFormUrlEncodedBody() throws Exception {
        given().contentType(ContentType.URLENC).formParam("foo", "bar")
                .post("/headerUrlEncoded")
                .then()
                .statusCode(200)
                .header("foo", "bar")
                .header("BodyClass", containsString("ReaderCache"))
                .body(is("foo=bar"));
    }

    @Test
    void testStreamingWithSpecificEncoding() throws Exception {
        Path input = Files.createTempFile("platform-http-input", "dat");
        Path output = Files.createTempFile("platform-http-output", "dat");

        String fileContent = "Content with special character ð";
        Files.writeString(input, fileContent, StandardCharsets.ISO_8859_1);

        InputStream response = given()
                .body(new FileInputStream(input.toFile())).post("/streamingSpecificEncoding")
                .then().statusCode(200)
                .extract().body()
                .asInputStream();

            try (FileOutputStream fos = new FileOutputStream(output.toFile())) {
                IOHelper.copy(response, fos);
            }

        assertEquals(fileContent, Files.readString(output, StandardCharsets.ISO_8859_1));
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
                public void configure() throws Exception {
                    getCamelContext().setStreamCaching(false);

                    from("platform-http:/streaming?useStreaming=true").transform().simple("Hello ${body}");
                    from("platform-http:/streamingFile?useStreaming=true").log("Done processing request");
                    from("platform-http:/streamingUrlEncoded?useStreaming=true").setBody().simple("foo=${header.foo}");
                    from("platform-http:/streamingSpecificEncoding?useStreaming=true").log("Done echoing back request body as response body");

                    from("platform-http:/nonStreaming").transform().simple("Hello ${body}");
                    from("platform-http:/nonStreamingFile").log("Done processing request");
                    from("platform-http:/nonStreamingUrlEncoded").setBody().simple("foo=${header.foo}");
                    from("platform-http:/headerAndBodyUrlEncoded")
                            .process(exchange ->
                                    exchange.getMessage().setHeader("BodyClass", exchange.getIn().getBody().getClass().getName()))
                            .convertBodyTo(String.class);
                    from("platform-http:/headerUrlEncoded?populateBodyWithForm=false")
                            .process(exchange ->
                                    exchange.getMessage().setHeader("BodyClass", exchange.getIn().getBody().getClass().getName()))
                            .log("Done processing request");
                }
            };
        }
    }
}
