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

import java.util.Locale;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.hashicorp.vault.HashicorpVaultClientSettings;
import org.apache.camel.component.hashicorp.vault.HashicorpVaultPropertiesFunction;
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.AbstractEarlyResolutionPropertiesParser;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.vault.HashicorpVaultConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Resolves the {@code {{hashicorp:...}}} placeholders of the Spring Boot properties before the CamelContext exists. It
 * reads the secrets as the properties function of the CamelContext does, with the same settings: the
 * {@code CAMEL_HASHICORP_VAULT_*} environment variables when one of them is set, else the {@code camel.vault.hashicorp}
 * options. It logs in once, and revokes the token of its login once the properties are resolved: the base class stops
 * the function, which closes its client.
 */
public class SpringBootHashicorpVaultPropertiesParser extends AbstractEarlyResolutionPropertiesParser {

    private static final String PREFIX = "camel.vault.hashicorp";
    private static final String ENVIRONMENT_PREFIX = "CAMEL_HASHICORP_VAULT_";

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
        // bound as Spring Boot binds the options of the CamelContext, such as camel.vault.hashicorp.role-id, while a
        // ${...} placeholder that cannot be resolved fails, as when the property is read from the environment
        HashicorpVaultConfiguration configuration = new Binder(ConfigurationPropertySources.get(environment),
                value -> value instanceof String text ? environment.resolveRequiredPlaceholders(text) : value)
                .bind(PREFIX, HashicorpVaultConfiguration.class)
                .orElseGet(HashicorpVaultConfiguration::new);
        HashicorpVaultClientSettings settings = HashicorpVaultClientSettings.fromEnvironmentOrConfiguration(configuration);
        required(settings.getHost(), "host", "Hashicorp Vault host is required");
        required(settings.getPort(), "port", "Hashicorp Vault port is required");
        required(settings.getScheme(), "scheme", "Hashicorp Vault scheme is required");
        try {
            Integer.parseInt(settings.getPort());
        } catch (NumberFormatException e) {
            throw new RuntimeCamelException(
                    PREFIX + ".port (or " + ENVIRONMENT_PREFIX + "PORT) must be a number but was: " + settings.getPort(), e);
        }

        try {
            return new HashicorpVaultPropertiesFunction(settings);
        } catch (RuntimeException e) {
            throw new RuntimeCamelException(
                    "Cannot create the Hashicorp Vault client of the early resolution: " + e.getMessage() + " (set the "
                                            + PREFIX + " options, or the " + ENVIRONMENT_PREFIX
                                            + "* environment variables)",
                    e);
        }
    }

    private static void required(String value, String option, String message) {
        if (ObjectHelper.isEmpty(value)) {
            throw new RuntimeCamelException(
                    message + " (set " + PREFIX + "." + option + ", or " + ENVIRONMENT_PREFIX
                                            + option.toUpperCase(Locale.ROOT) + " when a " + ENVIRONMENT_PREFIX
                                            + "* environment variable is set)");
        }
    }
}
