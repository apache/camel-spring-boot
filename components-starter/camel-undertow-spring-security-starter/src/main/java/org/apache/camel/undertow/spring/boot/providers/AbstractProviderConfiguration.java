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

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URISyntaxException;

/**
 * Abstract parent for each security provider.
 */
public abstract class AbstractProviderConfiguration {

    public enum TYPE {
        keycloak
    }

    /**
     * Whether an incoming token must carry the configured client id in its aud claim. Every client of a realm shares
     * the signing key, so with this disabled a token minted for any other client of the same realm is accepted.
     */
    private boolean validateAudience = true;

    abstract TYPE getType();

    public abstract ClientRegistration getClientRegistration() throws URISyntaxException;

    public abstract String getUserNameAttribute();

    /**
     * The issuer the provider stamps into the {@code iss} claim of the tokens it mints.
     */
    public abstract String getIssuerUri() throws URISyntaxException;

    public boolean isValidateAudience() {
        return validateAudience;
    }

    public void setValidateAudience(boolean validateAudience) {
        this.validateAudience = validateAudience;
    }

    public Converter<Jwt, ? extends AbstractAuthenticationToken> getJwtAuthenticationConverter() {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public String toString() {
        return getType().name();
    }
}
