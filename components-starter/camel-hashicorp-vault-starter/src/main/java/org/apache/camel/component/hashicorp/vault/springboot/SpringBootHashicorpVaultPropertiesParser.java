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
package org.apache.camel.component.hashicorp.vault.springboot;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.hashicorp.vault.HashicorpVaultPropertiesFunction;
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.AbstractEarlyResolutionPropertiesParser;
import org.apache.camel.util.ObjectHelper;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

public class SpringBootHashicorpVaultPropertiesParser extends AbstractEarlyResolutionPropertiesParser {

    @Override
    protected String getEarlyResolutionProperty() {
        return "camel.component.hashicorp-vault.early-resolve-properties";
    }

    @Override
    protected String getOverridePropertySourceName() {
        return "overridden-camel-hashicorp-vault-properties";
    }

    @Override
    protected PropertiesFunction createPropertiesFunction(ConfigurableEnvironment environment) {
        String token = required(environment, "camel.vault.hashicorp.token", "Hashicorp Vault token is required");
        String host = required(environment, "camel.vault.hashicorp.host", "Hashicorp Vault host is required");
        String portValue = required(environment, "camel.vault.hashicorp.port", "Hashicorp Vault port is required");
        String scheme = required(environment, "camel.vault.hashicorp.scheme", "Hashicorp Vault scheme is required");

        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException e) {
            throw new RuntimeCamelException(
                    "camel.vault.hashicorp.port must be a number but was: " + portValue, e);
        }

        VaultEndpoint vaultEndpoint = new VaultEndpoint();
        vaultEndpoint.setHost(host);
        vaultEndpoint.setPort(port);
        vaultEndpoint.setScheme(scheme);

        VaultTemplate client = new VaultTemplate(
                vaultEndpoint,
                new TokenAuthentication(token));
        return new HashicorpVaultPropertiesFunction(client);
    }

    private static String required(ConfigurableEnvironment environment, String key, String message) {
        String value = environment.getProperty(key);
        if (ObjectHelper.isEmpty(value)) {
            throw new RuntimeCamelException(message + " (set " + key + ")");
        }
        return value;
    }
}
