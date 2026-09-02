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
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Multipart uploads are copied out of the servlet container into the servlet temp directory, which makes Camel the
 * owner of the copy. Verifies the copy is readable while the exchange is routed and is deleted once the exchange is
 * done, which is the default behaviour.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpUploadCleanupTest.class,
        SpringBootPlatformHttpUploadCleanupTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpUploadCleanupTest {

    private static final byte[] CONTENT = "upload content".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void singleUploadIsDeletedWhenTheExchangeIsDone() {
        ExtractableResponse<Response> response = given().multiPart("file", "invoice.txt", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header(UploadCleanupRoute.ATTACHMENT_COUNT, is("1"))
                .header(UploadCleanupRoute.EXISTED_DURING_ROUTING, is("true"))
                .extract();

        String uploadPath = response.header(UploadCleanupRoute.UPLOAD_PATHS);
        // the single upload is also handed to the route as the message body and as the CamelFilePath header
        assertEquals(uploadPath, response.header(UploadCleanupRoute.FILE_PATH_HEADER));
        assertEquals(uploadPath, response.header(UploadCleanupRoute.BODY_PATH));

        Path uploadedFile = Paths.get(uploadPath);
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertFalse(Files.exists(uploadedFile),
                        "Uploaded temporary file should have been deleted: " + uploadedFile));
    }

    @Test
    void allUploadsAreDeletedWhenTheExchangeIsDone() {
        String uploadPaths = given().multiPart("first", "first.txt", CONTENT)
                .multiPart("second", "second.txt", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header(UploadCleanupRoute.ATTACHMENT_COUNT, is("2"))
                .header(UploadCleanupRoute.EXISTED_DURING_ROUTING, is("true"))
                .extract()
                .header(UploadCleanupRoute.UPLOAD_PATHS);

        String[] paths = uploadPaths.split(",");
        assertEquals(2, paths.length, "Expected both uploads to be reported: " + uploadPaths);
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            for (String path : paths) {
                assertFalse(Files.exists(Paths.get(path)), "Uploaded temporary file should have been deleted: " + path);
            }
        });
    }

    /**
     * A multipart request that carries no file part registers no cleanup and keeps working.
     */
    @Test
    void requestWithoutFilePartIsNotAffected() {
        given().multiPart("field", "value")
                .post("/upload")
                .then()
                .statusCode(200)
                .header(UploadCleanupRoute.ATTACHMENT_COUNT, is("0"));
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
