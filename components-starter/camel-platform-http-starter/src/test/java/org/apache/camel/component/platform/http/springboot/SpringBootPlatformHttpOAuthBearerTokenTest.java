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

import com.google.gson.JsonObject;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.oauth.Credentials;
import org.apache.camel.oauth.OAuth;
import org.apache.camel.oauth.OAuthBearerTokenProcessor;
import org.apache.camel.oauth.OAuthCodeFlowParams;
import org.apache.camel.oauth.OAuthException;
import org.apache.camel.oauth.OAuthFactory;
import org.apache.camel.oauth.OAuthLogoutParams;
import org.apache.camel.oauth.TokenCredentials;
import org.apache.camel.oauth.UserProfile;
import org.apache.camel.oauth.jakarta.ServletOAuthFactory;
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
import org.springframework.security.web.SecurityFilterChain;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * camel-oauth reads the bearer token from the request of the platform-http message of Spring Boot
 * ({@link PlatformHttpMessage}), which it matches by class name: these tests fail if that class is renamed or no longer
 * gives the request, because the token of a form parameter would then be read from the message headers.
 */
@EnableAutoConfiguration
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CamelAutoConfiguration.class,
        SpringBootPlatformHttpOAuthBearerTokenTest.class, SpringBootPlatformHttpOAuthBearerTokenTest.TestConfiguration.class,
        PlatformHttpComponentAutoConfiguration.class, SpringBootPlatformHttpAutoConfiguration.class })
class SpringBootPlatformHttpOAuthBearerTokenTest {

    private static final String TOKEN = "alice-token";

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        RestAssured.port = env.getRequiredProperty("local.server.port", Integer.class);
    }

    @Test
    void bearerTokenIsReadFromTheAuthorizationHeader() {
        given().header("Authorization", "Bearer " + TOKEN)
                .get("/bearer")
                .then().statusCode(200).body(equalTo("alice"));
    }

    @Test
    void bearerTokenIsNotReadFromAFormParameter() {
        given().contentType(ContentType.URLENC).formParam("Authorization", "Bearer " + TOKEN)
                .post("/bearer")
                .then().statusCode(401);
    }

    @Test
    void bearerTokenIsNotReadFromAQueryParameter() {
        given().queryParam("Authorization", "Bearer " + TOKEN)
                .get("/bearer")
                .then().statusCode(401);
    }

    @Test
    void requestIsFoundWhenTheRouteRemovedTheCamelHeaders() {
        given().header("Authorization", "Bearer " + TOKEN)
                .get("/bearer-without-camel-headers")
                .then().statusCode(200).body(equalTo("alice"));
        given().contentType(ContentType.URLENC).formParam("Authorization", "Bearer " + TOKEN)
                .post("/bearer-without-camel-headers")
                .then().statusCode(401);
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        // camel-oauth looks up its factory and its OAuth instance in the registry by class name. Without a factory
        // bean it looks for the platform-http router of Vert.x, whose class is not on this classpath.
        @Bean(name = "org.apache.camel.oauth.OAuthFactory")
        OAuthFactory oauthFactory(CamelContext camelContext) {
            return new ServletOAuthFactory(camelContext);
        }

        @Bean(name = "org.apache.camel.oauth.OAuth")
        OAuth oauth() {
            return new StubOAuth();
        }

        @Bean
        RouteBuilder oauthBearerTokenRoutes() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("platform-http:/bearer")
                            .process(new OAuthBearerTokenProcessor())
                            .process(exchange -> exchange.getMessage().setBody(subject(exchange)));
                    // as the security model advises for untrusted callers
                    from("platform-http:/bearer-without-camel-headers")
                            .removeHeaders("Camel*")
                            .process(new OAuthBearerTokenProcessor())
                            .process(exchange -> exchange.getMessage().setBody(subject(exchange)));
                }
            };
        }

        private static String subject(Exchange exchange) {
            return exchange.getProperty(OAuth.CAMEL_OAUTH_USER_PROFILE, UserProfile.class).subject();
        }
    }

    /**
     * An identity provider stand-in that knows one access token.
     */
    static final class StubOAuth extends OAuth {

        @Override
        public void discoverOAuthConfig(CamelContext ctx) {
            // nothing to discover
        }

        @Override
        public UserProfile authenticate(Credentials creds) {
            if (creds instanceof TokenCredentials tokenCreds && TOKEN.equals(tokenCreds.getToken())) {
                var principal = new JsonObject();
                principal.addProperty("access_token", TOKEN);
                principal.addProperty("username", "alice");
                var attributes = new JsonObject();
                attributes.addProperty("exp", System.currentTimeMillis() / 1000L + 3600);
                return new UserProfile(principal, attributes);
            }
            throw new OAuthException("Unknown token");
        }

        @Override
        public String buildLogoutRequestUrl(OAuthLogoutParams params) {
            return "https://idp.example/logout";
        }

        @Override
        public String buildCodeFlowAuthRequestUrl(OAuthCodeFlowParams params) {
            return "https://idp.example/auth";
        }
    }
}
