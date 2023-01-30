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
package org.apache.camel.undertow.spring.boot.providers;

import org.apache.camel.component.spring.security.keycloak.KeycloakJwtAuthenticationConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configuration of provider for the Keycloak server.
 *
 * Requires 4 attributes: server url, realmId, clientId and userNameAttribute (with default value "preferred_username")
 */
public class KeycloakProviderConfiguration extends AbstractProviderConfiguration {

    /**
     * Url to keycloak server which will be used in spring security configuration. (Example "http://localhost:8080")
     */
    private String url;
    /**
     * Realm id from the keycloak server used for authentication.
     */
    private String realmId;
    /**
     * Client id from the Keycloak server used for authentication.
     */
    private String clientId;
    /**
     * Name of the attribute, which will be used as username.
     */
    private String userNameAttribute = "preferred_username";

    @Override
    public TYPE getType() {
        return TYPE.keycloak;
    }

    @Override
    public Converter<Jwt, ? extends AbstractAuthenticationToken> getJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }

    @Override
    public ClientRegistration getClientRegistration() throws URISyntaxException {
        URI keycloakUri = new URI(getUrl()).resolve("/auth/realms/" + getRealmId() + "/protocol/openid-connect");
        return ClientRegistration.withRegistrationId(getType().name())
                .clientId(getClientId())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid","profile", "email")
                .authorizationUri(keycloakUri + "/auth")
                .tokenUri(keycloakUri + "/token")
                .jwkSetUri(keycloakUri + "/certs")
                .userNameAttributeName(getUserNameAttribute())
                .build();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }
}
