package org.apache.camel.component.platform.http.springboot;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.internal.ValidatableResponseImpl;
import io.restassured.response.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;

@EnableAutoConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpSessionTest.class, SpringBootPlatformHttpSessionTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class, })
public class SpringBootPlatformHttpSessionTest {

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
    @EnableWebSecurity
    public static class TestConfiguration {

        @Bean
        public UserDetailsService userDetailsService() {
            UserDetails user = User.builder()
                    .username("user")
                    .password("{noop}password")
                    .authorities("USER")
                    .build();

            UserDetails admin = User.builder()
                    .username("admin")
                    .password("{noop}password")
                    .authorities("ADMIN")
                    .build();

            return new InMemoryUserDetailsManager(user, admin);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.disable())
                    .authorizeHttpRequests(authorizeRequests ->
                            authorizeRequests.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }

        @Bean
        public RouteBuilder springBootPlatformHttpRestDSLRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    rest("/authenticated")
                        .post("/first")
                            .to("direct:first")
                        .get("/second")
                            .to("direct:second");

                    from("direct:first")
                            .process(exchange -> {
                                PlatformHttpMessage message = (PlatformHttpMessage) exchange.getMessage();
                                message.getRequest().getSession()
                                        .setAttribute("json-attribute", exchange.getMessage().getBody(String.class));
                            });

                    from("direct:second")
                            .process(exchange -> {
                                PlatformHttpMessage message = (PlatformHttpMessage) exchange.getMessage();

                                exchange.getIn().setBody(
                                        message.getRequest().getSession().getAttribute("json-attribute"));
                            });
                }
            };
        }
    }

    protected String getSessionKey() {
        return "JSESSIONID";
    }

    @Test
    public void session() {
        String jsonAttribute = """
                {"test":"attribute"}
                """;

        var result = given()
                .auth().basic("user", "password")
                .body(jsonAttribute)
                .when()
                .post("/authenticated/first")
                .then()
                .statusCode(200)
                .cookie(getSessionKey());

        String session = ((ValidatableResponseImpl) result).originalResponse().cookies().get(getSessionKey());

        result = given()
                .auth().basic("user", "password")
                .cookie(new Cookie.Builder(getSessionKey(), session).build())
                .when()
                .get("/authenticated/second")
                .then()
                .statusCode(200)
                .body(Matchers.is(jsonAttribute));

        Response response = ((ValidatableResponseImpl) result).originalResponse();

        result = given()
                .auth().basic("admin", "password")
                .when()
                .get("/authenticated/second")
                .then()
                .statusCode(204)
                .body(Matchers.is(""));

        response = ((ValidatableResponseImpl) result).originalResponse();
    }
}
