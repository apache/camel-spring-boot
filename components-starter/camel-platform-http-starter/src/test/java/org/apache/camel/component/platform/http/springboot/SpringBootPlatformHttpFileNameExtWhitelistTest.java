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
import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Verifies that {@code fileNameExtWhitelist} is enforced against the submitted file name, fails closed when the name
 * carries no extension, and matches whole extension tokens rather than substrings. Also covers the
 * {@code CamelFileName} path-segment stripping aligned with CAMEL-24293.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpFileNameExtWhitelistTest.class,
        SpringBootPlatformHttpFileNameExtWhitelistTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
public class SpringBootPlatformHttpFileNameExtWhitelistTest {

    private static final byte[] CONTENT = "upload content".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void acceptsWhitelistedExtension() {
        given().multiPart("file", "invoice.pdf", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header("attachmentCount", is("1"));
    }

    @Test
    void acceptsSecondWhitelistEntryIgnoringSurroundingSpaces() {
        given().multiPart("file", "notes.txt", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header("attachmentCount", is("1"));
    }

    @Test
    void rejectsExtensionOutsideTheWhitelist() {
        given().multiPart("file", "shell.jsp", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header("attachmentCount", is("0"));
    }

    /**
     * The whitelist must be evaluated against the submitted file name, not the multipart field name - the field name
     * is not the value propagated downstream.
     */
    @Test
    void rejectsWhenOnlyTheFieldNameLooksWhitelisted() {
        given().multiPart("report.pdf", "shell.jsp", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header("attachmentCount", is("0"));
    }

    /**
     * A file name with no extension must fail closed while a whitelist is configured.
     */
    @Test
    void rejectsFileNameWithoutExtension() {
        given().multiPart("file", "shell", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header("attachmentCount", is("0"));
    }

    /**
     * "pd" must not be accepted just because it is a substring of the whitelisted "pdf".
     */
    @Test
    void rejectsSubstringOfAWhitelistedExtension() {
        given().multiPart("file", "invoice.pd", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header("attachmentCount", is("0"));
    }

    @Test
    void stripsPathSegmentsFromCamelFileName() {
        given().multiPart("file", "../../evil.pdf", CONTENT)
                .post("/upload")
                .then()
                .statusCode(200)
                .header("attachmentCount", is("1"))
                .header("uploadFileName", is("evil.pdf"));
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
        public RouteBuilder whitelistRoute() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/upload?fileNameExtWhitelist=pdf,txt")
                            .process(exchange -> {
                                AttachmentMessage am = exchange.getMessage(AttachmentMessage.class);
                                int count = am.getAttachments() == null ? 0 : am.getAttachments().size();
                                Object fileName = exchange.getMessage().getHeader(Exchange.FILE_NAME);
                                exchange.getMessage().setHeader("attachmentCount", String.valueOf(count));
                                exchange.getMessage().setHeader("uploadFileName", fileName == null ? "" : fileName.toString());
                                exchange.getMessage().setBody("ok");
                            });
                }
            };
        }
    }
}
