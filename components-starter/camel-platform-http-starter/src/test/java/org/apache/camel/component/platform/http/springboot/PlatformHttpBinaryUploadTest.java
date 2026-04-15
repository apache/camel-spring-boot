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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests binary file upload through platform-http REST DSL.
 * Reproducer for <a href="https://issues.apache.org/jira/browse/CAMEL-23320">CAMEL-23320</a>:
 * binary data corruption caused by Reader-based parsing in non-streaming mode.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpTest.class, PlatformHttpBinaryUploadTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class PlatformHttpBinaryUploadTest {

    static final String FILE_NAME = "example.pdf";
    static Path uploadDir;

    @LocalServerPort
    private int port;

    @BeforeAll
    public static void createUploadDir() throws IOException {
        uploadDir = Files.createTempDirectory(Path.of("target"), "platform-http-upload");
    }

    @AfterAll
    public static void cleanUploadDir() throws IOException {
        Files.deleteIfExists(uploadDir.resolve(FILE_NAME));
        Files.deleteIfExists(uploadDir);
    }

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    @Test
    void testBinaryPdfUpload() throws IOException {
        byte[] pdfBytes = getClass().getResourceAsStream(FILE_NAME).readAllBytes();

        given()
                .body(pdfBytes)
                .contentType("application/pdf")
                .post("/upload")
                .then()
                .statusCode(200);

        Path uploadedFile = uploadDir.resolve(FILE_NAME);
        assertTrue(Files.exists(uploadedFile), "The uploaded file should exist");
        assertArrayEquals(pdfBytes, Files.readAllBytes(uploadedFile), "The PDF should not be corrupted");
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    rest().consumes("application/pdf")
                            .post("/upload")
                            .to("direct:upload");

                    from("direct:upload")
                            .convertBodyTo(byte[].class)
                            .process(exchange -> {
                                byte[] body = exchange.getIn().getBody(byte[].class);
                                try (FileOutputStream fos = new FileOutputStream(
                                        uploadDir.resolve(FILE_NAME).toFile())) {
                                    fos.write(body);
                                }
                            })
                            .setBody(constant("uploaded"));
                }
            };
        }
    }
}
