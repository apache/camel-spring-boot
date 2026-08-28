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

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A token signed by the realm is not automatically a token meant for this client: every client of the realm shares the
 * signing key, so the audience has to be checked explicitly.
 */
public class JwtAudienceValidatorTest {

    private static final String CLIENT_ID = "camel-client";

    private final JwtAudienceValidator validator = new JwtAudienceValidator(CLIENT_ID);

    private static Jwt token(List<String> audience, String authorizedParty) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("sub", "someone");
        if (audience != null) {
            builder.audience(audience);
        }
        if (authorizedParty != null) {
            builder.claim("azp", authorizedParty);
        }
        return builder.build();
    }

    @Test
    public void acceptsTokenWithThisClientInAudience() {
        OAuth2TokenValidatorResult result = validator.validate(token(List.of(CLIENT_ID), null));
        assertFalse(result.hasErrors(), "a token addressed to this client should be accepted");
    }

    @Test
    public void rejectsMatchingAuthorizedPartyWhenAudienceTargetsAnotherService() {
        OAuth2TokenValidatorResult result = validator.validate(token(List.of("account"), CLIENT_ID));
        assertTrue(result.hasErrors(), "the authorized party must not override a mismatching audience");
    }

    @Test
    public void rejectsTokenMintedForAnotherClientInTheSameRealm() {
        OAuth2TokenValidatorResult result = validator.validate(token(List.of("other-spa"), "other-spa"));
        assertTrue(result.hasErrors(), "a token issued to a different client of the same realm must be rejected");
        assertTrue(result.getErrors().iterator().next().getDescription().contains(CLIENT_ID),
                "the failure should name the expected client id");
    }

    @Test
    public void rejectsTokenCarryingNoAudience() {
        OAuth2TokenValidatorResult result = validator.validate(token(null, null));
        assertTrue(result.hasErrors(), "a token without an audience must be rejected");
    }

    @Test
    public void acceptsWhenThisClientIsOneOfSeveralAudiences() {
        OAuth2TokenValidatorResult result = validator.validate(token(List.of("account", CLIENT_ID), "gateway"));
        assertFalse(result.hasErrors(), "aud may legitimately carry several entries");
    }
}
