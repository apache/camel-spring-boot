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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the opt-out: with
 * {@code camel.component.platform-http.server.delete-uploaded-files-on-end=false} the temporary copy of the upload
 * survives the exchange and the route is responsible for it.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "camel.component.platform-http.server.delete-uploaded-files-on-end=false",
        classes = { CamelAutoConfiguration.class,
                SpringBootPlatformHttpUploadCleanupDisabledTest.class,
                SpringBootPlatformHttpUploadCleanupDisabledTest.TestConfiguration.class,
                PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpUploadCleanupDisabledTest {

    private static final byte[] CONTENT = "upload content".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void uploadIsKeptWhenCleanupIsTurnedOff() throws IOException {
        String uploadPath = given().multiPart("file", "invoice.txt", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header(UploadCleanupRoute.ATTACHMENT_COUNT, is("1"))
                .header(UploadCleanupRoute.EXISTED_DURING_ROUTING, is("true"))
                .extract()
                .header(UploadCleanupRoute.UPLOAD_PATHS);

        Path uploadedFile = Paths.get(uploadPath);
        try {
            // give any (unwanted) completion driven deletion time to happen before asserting the file is still there
            await().pollDelay(Duration.ofMillis(500))
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> assertTrue(Files.exists(uploadedFile),
                            "Uploaded temporary file should have been kept: " + uploadedFile));
        } finally {
            // the application owns the file when the cleanup is turned off, so do not leave it behind
            Files.deleteIfExists(uploadedFile);
        }
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
        public RouteBuilder uploadCleanupRoute() {
            return new UploadCleanupRoute();
        }
    }
}
