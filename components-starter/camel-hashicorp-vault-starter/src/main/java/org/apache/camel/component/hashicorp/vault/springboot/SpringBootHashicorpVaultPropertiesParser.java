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
import org.apache.camel.component.hashicorp.vault.HashicorpVaultClient;
import org.apache.camel.component.hashicorp.vault.HashicorpVaultClientSettings;
import org.apache.camel.component.hashicorp.vault.HashicorpVaultPropertiesFunction;
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.AbstractEarlyResolutionPropertiesParser;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.vault.HashicorpVaultConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Resolves the {@code {{hashicorp:...}}} placeholders of the Spring Boot properties before the CamelContext exists. It
 * logs in to HashiCorp Vault with the {@code camel.vault.hashicorp} options, as the properties function of the
 * CamelContext does, and revokes the token of its login once the properties are resolved.
 */
public class SpringBootHashicorpVaultPropertiesParser extends AbstractEarlyResolutionPropertiesParser {

    private static final String PREFIX = "camel.vault.hashicorp";

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
        // bound as Spring Boot binds the options of the CamelContext, such as camel.vault.hashicorp.role-id
        HashicorpVaultConfiguration configuration = Binder.get(environment)
                .bind(PREFIX, HashicorpVaultConfiguration.class)
                .orElseGet(HashicorpVaultConfiguration::new);
        if (isTokenLogin(configuration)) {
            required(configuration.getToken(), "token", "Hashicorp Vault token is required");
        }
        required(configuration.getHost(), "host", "Hashicorp Vault host is required");
        required(configuration.getPort(), "port", "Hashicorp Vault port is required");
        required(configuration.getScheme(), "scheme", "Hashicorp Vault scheme is required");
        try {
            Integer.parseInt(configuration.getPort());
        } catch (NumberFormatException e) {
            throw new RuntimeCamelException(
                    PREFIX + ".port must be a number but was: " + configuration.getPort(), e);
        }

        HashicorpVaultClient client;
        try {
            client = HashicorpVaultClient.create(HashicorpVaultClientSettings.fromConfiguration(configuration));
        } catch (IllegalArgumentException e) {
            throw new RuntimeCamelException(
                    "Cannot create the Hashicorp Vault client of the " + PREFIX + " options: " + e.getMessage(), e);
        }
        return new EarlyResolutionFunction(client);
    }

    @Override
    protected void release(PropertiesFunction propertiesFunction) {
        if (propertiesFunction instanceof EarlyResolutionFunction function) {
            // stops renewing the token, and revokes the token of the login (but not a token given for the token login)
            function.client.close();
        }
    }

    private static boolean isTokenLogin(HashicorpVaultConfiguration configuration) {
        String authentication = configuration.getAuthentication();
        return ObjectHelper.isEmpty(authentication) || "token".equalsIgnoreCase(authentication.trim());
    }

    private static void required(String value, String option, String message) {
        if (ObjectHelper.isEmpty(value)) {
            throw new RuntimeCamelException(message + " (set " + PREFIX + "." + option + ")");
        }
    }

    /**
     * The properties function of the early resolution, with the client it reads the secrets with.
     */
    private static final class EarlyResolutionFunction extends HashicorpVaultPropertiesFunction {

        private final HashicorpVaultClient client;

        private EarlyResolutionFunction(HashicorpVaultClient client) {
            super(client.getVaultTemplate());
            this.client = client;
        }
    }
}
