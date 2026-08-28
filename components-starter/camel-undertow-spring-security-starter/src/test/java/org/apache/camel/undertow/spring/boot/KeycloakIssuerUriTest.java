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
package org.apache.camel.undertow.spring.boot;

import org.apache.camel.undertow.spring.boot.providers.KeycloakProviderConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The issuer used to validate the {@code iss} claim must be the realm the client registration already points at,
 * otherwise every token would be rejected.
 */
public class KeycloakIssuerUriTest {

    private static KeycloakProviderConfiguration provider(String url) {
        KeycloakProviderConfiguration provider = new KeycloakProviderConfiguration();
        provider.setUrl(url);
        provider.setRealmId("my-realm");
        provider.setClientId("camel-client");
        return provider;
    }

    @Test
    public void derivesTheRealmIssuer() throws Exception {
        assertEquals("http://localhost:8080/auth/realms/my-realm", provider("http://localhost:8080").getIssuerUri());
    }

    @Test
    public void ignoresAnyPathOnTheConfiguredUrl() throws Exception {
        // the client registration resolves from the root too, so both must agree
        assertEquals("https://sso.example.com/auth/realms/my-realm",
                provider("https://sso.example.com/some/path").getIssuerUri());
    }

    @Test
    public void issuerIsThePrefixOfTheJwkSetUri() throws Exception {
        KeycloakProviderConfiguration provider = provider("http://localhost:8080");
        String jwkSetUri = provider.getClientRegistration().getProviderDetails().getJwkSetUri();
        assertTrue(jwkSetUri.startsWith(provider.getIssuerUri()),
                "issuer " + provider.getIssuerUri() + " should prefix the jwk set uri " + jwkSetUri);
        assertEquals("http://localhost:8080/auth/realms/my-realm/protocol/openid-connect/certs", jwkSetUri);
    }

    @Test
    public void audienceValidationIsOnByDefault() {
        assertTrue(provider("http://localhost:8080").isValidateAudience());
    }
}
