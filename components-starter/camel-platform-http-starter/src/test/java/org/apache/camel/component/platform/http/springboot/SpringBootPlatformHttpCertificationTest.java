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
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.platform.http.cookie.CookieConfiguration;
import org.apache.camel.component.platform.http.cookie.CookieHandler;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.util.IOHelper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.component.platform.http.springboot.SpringBootPlatformHttpConsumer;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpCertificationTest.class, SpringBootPlatformHttpCertificationTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class})
public class SpringBootPlatformHttpCertificationTest extends PlatformHttpBase {

    private static final String postRouteId = "SpringBootPlatformHttpRestDSLTest_mypost";
    private static final String getRouteId = "SpringBootPlatformHttpRestDSLTest_myget";

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder springBootPlatformHttpRestDSLRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    getCamelContext().getStreamCachingStrategy().setSpoolEnabled(true);
                    rest()
                            .get("myget").id(getRouteId).to("direct:get")
                            .post("mypost").id(postRouteId).to("direct:post");

                    from("direct:post").transform().body(String.class, b -> b.toUpperCase());
                    from("direct:get").setBody().constant("get")
                            .log("hello");

                    rest("rest").get("/test")
                            .consumes("application/json,application/xml")
                            .produces("application/json,application/xml")
                            .bindingMode(RestBindingMode.auto)
                            .to("direct:rest");

                    rest("rest").post("/test")
                            .consumes("application/json,application/xml")
                            .produces("application/json,application/xml")
                            .bindingMode(RestBindingMode.auto)
                            .to("direct:rest");

                    from("direct:rest")
                            .setBody(simple("Hello"));

                    from("platform-http:/streaming?useStreaming=true")
                            .log("Done echoing back request body as response body");

                    from("platform-http:/helloStreaming?useStreaming=true")
                            .transform().simple("Hello ${body}");

                    from("platform-http:/streamingFileRequestResponseBody?useStreaming=true")
                            .log("Done processing request");

                    from("platform-http:/streamingWithFormUrlEncodedBody?useStreaming=true")
                            .setBody().simple("foo = ${header.foo}");

                    from("platform-http:/streamingWithSpecificEncoding?useStreaming=true")
                            .log("Done echoing back request body as response body");

                    from("platform-http:/streamingWithClosedInputStreamResponse?useStreaming=true")
                            .process(new Processor() {
                                @Override
                                public void process(Exchange exchange) throws Exception {
                                    // Simulate an error processing an input stream by closing it ahead of the response being written
                                    // Verifies the response promise.fail is called correctly
                                    InputStream stream = getClass().getResourceAsStream("/application.properties");
                                    if (stream != null) {
                                        stream.close();
                                    }
                                    exchange.getMessage().setBody(stream);
                                }
                            });

                    from("platform-http:/streamingWithUnconvertableResponseType?useStreaming=true")
                            .process(new Processor() {
                                @Override
                                public void process(Exchange exchange) {
                                    // Force a type conversion exception and verify the response promise.fail is called correctly
                                    exchange.getMessage().setBody(new TestBean());
                                }
                            })
                            .log("Conversion done");

                    from("platform-http:/session")
                            .setBody().constant("session");

                    from("platform-http:/streamingWithLargeRequestAndResponseBody?useStreaming=true")
                            .log("Done echoing back request body as response body");

                    from("platform-http:/addCookie?useCookieHandler=true")
                            .process(exchange -> {
                                exchange.getProperty(Exchange.COOKIE_HANDLER, CookieHandler.class).addCookie("foo", "bar");
                            })
                            .setBody().constant("add");
                }
            };
        }
    }

    @Override
    protected String getPostRouteId() {
        return postRouteId;
    }

    @Override
    protected String getGetRouteId() {
        return getRouteId;
    }

    @Test
    public void testLoad() throws Exception {
        waitUntilRouteIsStarted(1, getGetRouteId());

        for (int i = 0; i < 1_000; i++) {
            Assertions.assertThat(restTemplate.getForEntity("/myget", String.class).getStatusCode().value()).isEqualTo(200);
        }
    }

    @Test
    public void nonSupportedContentType() {
        RestAssured.given()
                .header("Content-Type", "notSupported")
                .post("rest/test")
                .then()
                .statusCode(415);
    }

    @Test
    public void oneContentType() {
        RestAssured.given()
                .header("Content-type", ContentType.XML)
                .header("Accept", ContentType.XML)
                .get("rest/test")
                .then()
                .statusCode(200)
                .body(is("Hello"));
    }

    @Test
    public void noContentTypeOkForGet() {
        RestAssured.given()
                .get("rest/test")
                .then()
                .statusCode(200)
                .body(is("\"Hello\""));
    }

    @Test
    public void nonSupportedAccept() {
        RestAssured.given()
                .header("Content-type", ContentType.XML)
                .header("Accept", ContentType.BINARY)
                .get("rest/test")
                .then()
                .statusCode(406);
    }

    @Test
    public void streaming() {
        String requestBody = "Vert.x Platform HTTP";
        given()
                .body(requestBody)
                .post("/helloStreaming")
                .then()
                .statusCode(200)
                .body(is("Hello " + requestBody));
    }

    @Test
    void streamingFileRequestResponseBody() throws Exception {
        String content = "Hello World";
        Path testFile = Files.createTempFile("platform-http-testing", "txt");
        Files.writeString(testFile, content);

        given()
                .body(testFile.toFile())
                .post("/streamingFileRequestResponseBody")
                .then()
                .statusCode(200)
                .body(is(content));
    }

    @Test
    void streamingWithFormUrlEncodedBody() throws Exception {
        given()
                .contentType(ContentType.URLENC)
                .formParam("foo", "bar")
                .post("/streamingWithFormUrlEncodedBody")
                .then()
                .statusCode(200)
                .body(is("foo = bar"));
    }

    @Test
    void streamingWithSpecificEncoding() throws Exception {
        Path input = Files.createTempFile("platform-http-input", "dat");
        Path output = Files.createTempFile("platform-http-output", "dat");

        String fileContent = "Content with special character ð";
        Files.writeString(input, fileContent, StandardCharsets.ISO_8859_1);

        InputStream response = given()
                .body(new FileInputStream(input.toFile()))
                .post("/streamingWithSpecificEncoding")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asInputStream();

        try (FileOutputStream fos = new FileOutputStream(output.toFile())) {
            IOHelper.copy(response, fos);
        }

        assertEquals(fileContent, Files.readString(output, StandardCharsets.ISO_8859_1));
    }

    @Test
    @Disabled("Test is failing, work in progress")
    void streamingWithClosedInputStreamResponse() throws Exception {

        given()
                .get("/streamingWithClosedInputStreamResponse")
                .then()
                .statusCode(500);
    }

    @Test
    void streamingWithUnconvertableResponseType() throws Exception {
            given()
                    .get("/streamingWithUnconvertableResponseType")
                    .then()
                    .statusCode(500);
    }

    static final class TestBean {
    }

    @Test
    public void session() {
        Map<String, String> cookies = given()
                .when()
                .get("/session")
                .then()
                .statusCode(200)
//                .cookie("vertx-web.session",
//                        detailedCookie()
//                                .path("/").value(notNullValue())
//                                .httpOnly(false)
//                                .secured(false)
//                                .sameSite("Strict"))
//                .header("cookie", nullValue())
                .body(equalTo("session"))
                .extract().cookies();

        System.out.println(cookies);
    }

    @Autowired
    CamelContext camelContext;

    @Test
    @DisabledIfSystemProperty(named = "ci.env.name", matches = "apache.org",
            disabledReason = "File too large for Apache CI")
    void streamingWithLargeRequestAndResponseBody() throws Exception {
        camelContext.getStreamCachingStrategy().setSpoolEnabled(true);

        Path input = createLargeFile();
        Path output = Files.createTempFile("platform-http-output", "dat");

        InputStream response = given()
                .body(new FileInputStream(input.toFile()))
                .post("/streaming")
                .then()
                .extract()
                .asInputStream();

        try (FileOutputStream fos = new FileOutputStream(output.toFile())) {
            IOHelper.copy(response, fos);
        }

        assertEquals(input.toFile().length(), output.toFile().length());
    }

    private Path createLargeFile() throws IOException {
        // Create a 4GB file containing random data
        Path path = Files.createTempFile("platform-http-input", "dat");
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            Random random = new Random();
            long targetFileSize = (long) (4 * Math.pow(1024, 3));
            long bytesWritten = 0L;

            byte[] data = new byte[1024];
            while (bytesWritten < targetFileSize) {
                random.nextBytes(data);
                fos.write(data);
                bytesWritten += data.length;
            }
        }
        return path;
    }

    private static CookieHandler getCookieHandler(Exchange exchange) {
        return exchange.getProperty(Exchange.COOKIE_HANDLER, CookieHandler.class);
    }

    @Test
    public void testAddCookie() throws Exception {
        given()
                .when()
                .get("/addCookie")
                .then()
                .statusCode(200)
                .cookie("foo",
                        detailedCookie()
                                .value("bar")
                                .path(CookieConfiguration.DEFAULT_PATH)
                                .domain((String) null)
                                .sameSite(CookieConfiguration.DEFAULT_SAME_SITE.getValue()))
                .body(equalTo("add"));
    }
}
