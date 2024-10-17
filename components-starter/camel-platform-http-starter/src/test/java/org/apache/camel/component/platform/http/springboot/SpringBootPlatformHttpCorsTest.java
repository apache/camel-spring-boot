package org.apache.camel.component.platform.http.springboot;

import io.restassured.RestAssured;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.RestConfiguration;
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

import static io.restassured.RestAssured.given;

@EnableAutoConfiguration(exclude = {OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpCorsTest.class, SpringBootPlatformHttpCorsTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
public class SpringBootPlatformHttpCorsTest {

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

//        @Bean
//        public WebMvcConfigurer corsConfigurer() {
//            return new WebMvcConfigurer() {
//                @Override
//                public void addCorsMappings(CorsRegistry registry) {
//                    registry.addMapping("/**");
//                }
//            };
//        }

        @Bean
        public RouteBuilder springBootPlatformHttpRestDSLRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    restConfiguration().component("platform-http").enableCORS(true);

                    rest("/rest")
                            .post()
                            .consumes("application/json")
                            .to("direct:rest");

                    from("direct:rest")
                            .setBody(simple("Hello ${body}"));

                    from("platform-http:/cors")
                            .transform().constant("cors");
                }
            };
        }
    }

    @Test
    @Disabled("Test is failing, work in progress")
    public void cors() {
        final String origin = "http://custom.origin.springboot";
        final String methods = "GET,POST";
        final String headers = "X-Custom";

        given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when()
                .get("/cors")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", methods)
                .header("Access-Control-Allow-Headers", headers);
    }

    @Test
    @Disabled("Test is failing, work in progress")
    public void corsWithConsumes() {
        final String origin = "http://custom.origin.springboot";

        given()
                .header("Origin", origin)
                .when()
                .options("/rest")
                .then()
                .statusCode(204)
                .header("Access-Control-Allow-Origin", RestConfiguration.CORS_ACCESS_CONTROL_ALLOW_ORIGIN)
                .header("Access-Control-Allow-Methods", RestConfiguration.CORS_ACCESS_CONTROL_ALLOW_METHODS)
                .header("Access-Control-Allow-Headers", RestConfiguration.CORS_ACCESS_CONTROL_ALLOW_HEADERS)
                .header("Access-Control-Max-Age", RestConfiguration.CORS_ACCESS_CONTROL_MAX_AGE);
    }
}
