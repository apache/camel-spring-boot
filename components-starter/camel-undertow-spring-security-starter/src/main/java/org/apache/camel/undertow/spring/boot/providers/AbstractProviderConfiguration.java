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

    abstract TYPE getType();

    public abstract ClientRegistration getClientRegistration() throws URISyntaxException;

    public abstract String getUserNameAttribute();

    public Converter<Jwt, ? extends AbstractAuthenticationToken> getJwtAuthenticationConverter() {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public String toString() {
        return getType().name();
    }
}
