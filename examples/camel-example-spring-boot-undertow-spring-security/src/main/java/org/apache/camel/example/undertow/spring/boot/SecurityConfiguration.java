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
package org.apache.camel.example.undertow.spring.boot;

import org.apache.camel.component.spring.security.keycloak.KeycloakJwtAuthenticationConverter;
import org.apache.camel.component.spring.security.keycloak.KeycloakUsernameSubClaimAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Collections;

/**
 * Security configuration for spring-security 5 and keycloak server.
 */
//CHECKSTYLE:OFF
@Configuration
public class SecurityConfiguration {

    private static final String PREFERRED_USERNAME = "preferred_username";

    @EnableWebSecurity
    public static class OAuth2Security extends WebSecurityConfigurerAdapter {

        @Override
        public void init(WebSecurity web) throws Exception {
            super.init(web);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .oauth2ResourceServer()
                    .jwt()
                    .jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter());
        }
    }

    @Bean
    public JwtDecoder jwtDecoderByIssuerUri(ClientRegistrationRepository repository) {
        final String jwkSetUri = repository.findByRegistrationId("keycloak").getProviderDetails().getJwkSetUri();
        final NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        // Use preferred_username from claims as authentication name, instead of UUID subject
        jwtDecoder.setClaimSetConverter(new KeycloakUsernameSubClaimAdapter(PREFERRED_USERNAME));
        return jwtDecoder;
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(Collections.singletonList(getKeycloakRegistration()));
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService() {

        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository());
    }

    /**
     * @return Configuration of keycloak client.
     */
    private ClientRegistration getKeycloakRegistration() {
        return ClientRegistration.withRegistrationId("keycloak")
                .clientId("example-app")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid","profile", "email")
                .authorizationUri("http://localhost:8080/auth/realms/example-app/protocol/openid-connect/auth")
                .tokenUri("http://localhost:8080/auth/realms/example-app/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:8080/auth/realms/example-app/protocol/openid-connect/certs")
                .userNameAttributeName(PREFERRED_USERNAME)
                .build();
    }
//CHECKSTYLE:ON
}
