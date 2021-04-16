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

import org.apache.camel.component.undertow.UndertowComponent;
import org.apache.camel.undertow.spring.boot.providers.KeycloakProviderConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for camel-undertow-spring-security provider.
 *
 * Contains configuration object for each provider (see AbstractUndertowSpringSecurityProviderConfiguration.TYPE)
 */
@AutoConfigureAfter(UndertowComponent.class)
@ConfigurationProperties(prefix = "camel.security.undertow")
public class UndertowSpringSecurityConfiguration {

    /**
     * Properties defined for keycloak provider. Value is gathered together from properties with prefix
     * "camel.component.undertow.spring.security.keycloak"
     */
    private KeycloakProviderConfiguration keycloak;

    @ConfigurationProperties(prefix = "camel.security.undertow.keycloak")
    public KeycloakProviderConfiguration getKeycloak() {
        return keycloak;
    }

    public void setKeycloak(KeycloakProviderConfiguration keycloak) {
        this.keycloak = keycloak;
    }
}