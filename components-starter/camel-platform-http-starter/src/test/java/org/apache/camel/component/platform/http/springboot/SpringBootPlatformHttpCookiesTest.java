package org.apache.camel.component.platform.http.springboot;

import io.restassured.RestAssured;
import jakarta.servlet.http.Cookie;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
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
import org.springframework.test.annotation.DirtiesContext;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpCookiesTest.class, SpringBootPlatformHttpCookiesTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
public class SpringBootPlatformHttpCookiesTest {

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
                    from("platform-http:/add")
                            .process(exchange -> {
                                PlatformHttpMessage message = (PlatformHttpMessage) exchange.getMessage();
                                message.getResponse().addCookie(new Cookie("foo", "bar"));
                            })
                            .setBody().constant("add");

                    from("platform-http:/remove")
                            .process(exchange -> {
                                PlatformHttpMessage message = (PlatformHttpMessage) exchange.getMessage();

                                Cookie cookie = Arrays.stream(message.getRequest().getCookies()).findFirst().get();
                                cookie.setMaxAge(0);
                                cookie.setValue(null);

                                message.getResponse().addCookie(cookie);
                            })
                            .setBody().constant("remove");

                    from("platform-http:/replace")
                            .process(exchange -> {
                                PlatformHttpMessage message = (PlatformHttpMessage) exchange.getMessage();
                                assertEquals(1, message.getRequest().getCookies().length);

                                Cookie cookie = new Cookie("XSRF-TOKEN", "88533580000c314");
                                cookie.setPath("/");
                                message.getResponse().addCookie(cookie);
                            })
                            .setBody().constant("replace");

                    from("platform-http:/echo")
                            .setBody().constant("echo");
                }
            };
        }
    }

    @Test
    public void addCookie() {
        given()
                .header("cookie", "foo=bar")
                .when()
                .get("/add")
                .then()
                .statusCode(200)
                .header("set-cookie", "foo=bar")
                .body(equalTo("add"));
    }

    @Test
    public void removeCookie() {
        given()
                .header("cookie", "foo=bar")
                .when()
                .get("/remove")
                .then()
                .statusCode(200)
                .header("set-cookie", startsWith("foo=; Max-Age=0; Expires="))
                .body(equalTo("remove"));
    }

    @Test
    public void replaceCookie() {
        given()
                .header("cookie", "XSRF-TOKEN=c359b44aef83415")
                .when()
                .get("/replace")
                .then()
                .statusCode(200)
                .header("set-cookie", "XSRF-TOKEN=88533580000c314; Path=/")
                .body(equalTo("replace"));
    }

    @Test
    public void echoCookie() {
        given()
                .header("cookie", "echo=cookie")
                .when()
                .get("/echo")
                .then()
                .statusCode(200)
                .header("cookie", startsWith("echo=cookie"))
                .body(equalTo("echo"));
    }

}
