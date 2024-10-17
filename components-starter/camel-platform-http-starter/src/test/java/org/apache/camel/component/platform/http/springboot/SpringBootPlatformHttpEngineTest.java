package org.apache.camel.component.platform.http.springboot;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.activation.DataHandler;
import org.apache.camel.CamelContext;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder springBootPlatformHttpRestDSLRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    rest()
                        .post("upload").to("direct:upload");

                    from("direct:upload")
                        .process(exchange -> {
                            AttachmentMessage message = exchange.getMessage(AttachmentMessage.class);
                            DataHandler attachment = message.getAttachment(attachmentId);
                            message.setBody(attachment.getContent());
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

                    from("platform-http:/error/query/param")
                            .setBody().constant("Error");
                }
            };
        }
    }

    @Test
    public void testAttachment() throws Exception {
        final String attachmentId = "myTestFile";
        final String fileContent = "Test multipart upload content";
        final File tempFile = File.createTempFile("platform-http", ".txt");

        Files.write(tempFile.toPath(), fileContent.getBytes(StandardCharsets.UTF_8));

        given()
                .multiPart(attachmentId, tempFile)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .body(is(fileContent));
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
                .body(is("foo=bar&cheese=wine"));
    }

    @Test
    @Disabled("Test is failing, work in progress")
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
    @Disabled("Test is failing, work in progress")
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
                .get("/get")
                .then()
                .statusCode(503);
    }

    @Test
    public void responseTypeConversionErrorHandled() {
        get("/error/response")
                .then()
                .statusCode(500);
    }

    @Test
    @Disabled("Test is failing, work in progress")
    public void responseBadQueryParamErrorHandled() {
        get("/error/query/param?::")
                .then()
                .statusCode(500);
    }
}
