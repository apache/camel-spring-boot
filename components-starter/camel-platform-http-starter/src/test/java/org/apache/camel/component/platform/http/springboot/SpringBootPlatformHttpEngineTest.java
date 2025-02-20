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
import jakarta.activation.DataHandler;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpEngineTest.class, SpringBootPlatformHttpEngineTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
public class SpringBootPlatformHttpEngineTest {
    private final static String attachmentId = "myTestFile";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    CamelContext camelContext;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    private static final List<String> attachmentIds = new ArrayList<>();

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder springBootPlatformHttpRestDSLRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    rest()
                        .post("uploadSingle").to("direct:uploadSingle")
                        .post("uploadMulti").to("direct:uploadMulti");

                    from("direct:uploadSingle")
                        .process(exchange -> {
                            AttachmentMessage message = exchange.getMessage(AttachmentMessage.class);
                            DataHandler attachment = message.getAttachment(attachmentId);
                            exchange.getMessage().setHeader("myDataHandler", attachment);
                            exchange.getMessage().setHeader("singleFileContentType",
                                    exchange.getIn().getHeader(Exchange.FILE_CONTENT_TYPE));
                            exchange.getMessage().setBody(attachment.getContent());
                        });

                    from("direct:uploadMulti")
                            .process(exchange -> {
                                AttachmentMessage message = exchange.getMessage(AttachmentMessage.class);

                                String result = "";
                                for (String attachmentId : attachmentIds) {
                                    DataHandler attachment = message.getAttachment(attachmentId);
                                    result += IOUtils.toString(attachment.getInputStream(), Charset.defaultCharset());
                                }

                                exchange.getIn().setHeader("ConcatFileContent", result);
                            });

                    from("platform-http:/form/post")
                            .convertBodyTo(String.class);

                    from("platform-http:/greeting/{name}?matchOnUriPrefix=true")
                            .transform().simple("Hello ${header.name}");

                    from("platform-http:/text/post")
                            .log("POST:/test/post has body ${body}");

                    from("platform-http:/responseHeaders")
                            .setHeader("nonEmptyFromRoute", constant("nonEmptyFromRouteValue"))
                            .setHeader("emptyFromRoute", constant(""))
                            .setBody().simple("Hello World");

                    from("platform-http:/multipleHeaders")
                            .setHeader("nonEmptyFromRoute", constant("nonEmptyFromRouteValue"))
                            .setBody().simple("Hello World");

                    from("platform-http:/consumerSuspended")
                            .routeId("consumerSuspended")
                            .setBody().constant("get");

                    from("platform-http:/error/response")
                            // Set the response to something that can't be type converted
                            .setBody().constant(Collections.EMPTY_SET);
                }
            };
        }
    }

    @Test
    public void testSingleAttachment() throws Exception {
        final String attachmentId = "myTestFile";
        final String fileContent = "Test multipart upload content";
        final File tempFile = File.createTempFile("platform-http", ".txt");

        Files.write(tempFile.toPath(), fileContent.getBytes(StandardCharsets.UTF_8));

        String dummyMimeType = "custom/mime-type";

        given()
                .multiPart(attachmentId, tempFile, dummyMimeType)
                .when()
                .post("/uploadSingle")
                .then()
                .statusCode(200)
                // Assert that the attachment is a DataHandler
                .header("myDataHandler", containsString("jakarta.activation.DataHandler"))
                .header("singleFileContentType", is(dummyMimeType))
                .body(is(fileContent));
    }

    @Test
    public void testMultiAttachments() throws Exception {
        attachmentIds.add("myFirstTestFile");
        attachmentIds.add("mySecondTestFile");

        String tmpDirectory = null;
        List<File> tempFiles = new ArrayList<>(attachmentIds.size());
        for (String attachmentId : attachmentIds) {
            final String fileContent = "Test multipart upload content " + attachmentId;
            File tempFile;
            if (tmpDirectory == null) {
                tempFile = File.createTempFile("platform-http-" + attachmentId, ".txt");
            } else {
                tempFile = File.createTempFile("platform-http-" + attachmentId, ".txt", new File(tmpDirectory));
            }

            tempFiles.add(tempFile);
            tmpDirectory = tempFile.getParent();
            Files.write(tempFile.toPath(), fileContent.getBytes(StandardCharsets.UTF_8));
        }

        given()
                .multiPart(attachmentIds.get(0), tempFiles.get(0))
                .multiPart(attachmentIds.get(1), tempFiles.get(1))
                .when()
                .post("/uploadMulti")
                .then()
                .statusCode(204)
                .body(emptyOrNullString())
                .header("ConcatFileContent",
                        is("Test multipart upload content myFirstTestFileTest multipart upload content mySecondTestFile"));
    }

    @Test
    public void formPost() {
        given()
                .formParam("foo", "bar")
                .formParam("cheese", "wine")
                .when()
                .post("/form/post")
                .then()
                .statusCode(200)
                .body(is("{foo=bar, cheese=wine}"));
    }

    @Test
    public void matchOnUriPrefix() {
        final String greeting = "Hello Camel";
        given()
                .when()
                .get("/greeting")
                .then()
                .statusCode(404);

        given()
                .when()
                .get("/greeting/Camel")
                .then()
                .statusCode(200)
                .body(equalTo(greeting));

        given()
                .when()
                .get("/greeting/Camel/other/path/")
                .then()
                .statusCode(200)
                .body(equalTo(greeting));
    }

    @Test
    public void textContentPost() {
        String payload = "Hello World";
        given()
                .contentType(ContentType.TEXT)
                .body(payload)
                .when()
                .post("/text/post")
                .then()
                .statusCode(200)
                .body(is(payload));
    }

    @Test
    public void responseHeaders() {
        RestAssured.given()
                .header("nonEmpty", "nonEmptyValue")
                .header("empty", "")
                .get("/responseHeaders")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World"))
                .header("nonEmpty", "nonEmptyValue")
                .header("empty", "")
                .header("nonEmptyFromRoute", "nonEmptyFromRouteValue")
                .header("emptyFromRoute", "");
    }

    @Test
    public void multipleHeaders() {
        RestAssured.given()
                .header("nonEmpty", "nonEmptyValue")
                .header("empty", "")
                .get("/multipleHeaders?duplicated=1&duplicated=2")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World"))
                .header("nonEmpty", "nonEmptyValue")
                .header("nonEmptyFromRoute", "nonEmptyFromRouteValue");
    }

    @Test
    public void consumerSuspended() throws Exception {
        given()
                .when()
                .get("/consumerSuspended")
                .then()
                .statusCode(200)
                .body(equalTo("get"));

        camelContext.getRouteController().suspendRoute("consumerSuspended");

        given()
                .when()
                .get("/consumerSuspended")
                .then()
                .statusCode(503);

        camelContext.getRouteController().resumeRoute("consumerSuspended");

        given()
                .when()
                .get("/consumerSuspended")
                .then()
                .statusCode(200)
                .body(equalTo("get"));
    }

    @Test
    public void responseTypeConversionErrorHandled() {
        get("/error/response")
                .then()
                .statusCode(500);
    }
}
