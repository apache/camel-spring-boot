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

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Checks that a JWT was actually issued for the configured client.
 * <p/>
 * A signature check alone only proves the token came from the realm; every client of that realm shares it. Without
 * this validator a token minted for any other client in the same realm is accepted here, so the token is bound to the
 * configured client id through the {@code aud} claim.
 */
class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String clientId;

    JwtAudienceValidator(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        final List<String> audience = token.getAudience();
        if (audience != null && audience.contains(clientId)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "The token audience does not contain the configured client id: '" + clientId + "'",
                "https://datatracker.ietf.org/doc/html/rfc9068#section-4"));
    }
}
