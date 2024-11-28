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

import org.apache.camel.component.hashicorp.vault.HashicorpVaultPropertiesFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.util.Objects;
import java.util.Properties;

public class SpringBootHashicorpVaultPropertiesParser implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootHashicorpVaultPropertiesParser.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        if (Boolean.parseBoolean(environment.getProperty("camel.component.hashicorp-vault.early-resolve-properties"))) {
            Objects.requireNonNull(environment.getProperty("camel.vault.hashicorp.token"), "Hashicorp Vault token is required");
            Objects.requireNonNull(environment.getProperty("camel.vault.hashicorp.host"), "Hashicorp Vault host is required");
            Objects.requireNonNull(environment.getProperty("camel.vault.hashicorp.port"), "Hashicorp Vault port is required");
            Objects.requireNonNull(environment.getProperty("camel.vault.hashicorp.scheme"), "Hashicorp Vault scheme is required");

            String token = environment.getProperty("camel.vault.hashicorp.token");
            String host = environment.getProperty("camel.vault.hashicorp.host");

            int port = Integer.parseInt(environment.getProperty("camel.vault.hashicorp.port"));
            String scheme = environment.getProperty("camel.vault.hashicorp.scheme");

            VaultEndpoint vaultEndpoint = new VaultEndpoint();
            vaultEndpoint.setHost(host);
            vaultEndpoint.setPort(port);
            vaultEndpoint.setScheme(scheme);

            VaultTemplate client = new VaultTemplate(
                    vaultEndpoint,
                    new TokenAuthentication(token));
            HashicorpVaultPropertiesFunction hashicorpVaultPropertiesFunction = new HashicorpVaultPropertiesFunction(client);

            final Properties props = new Properties();
            for (PropertySource mutablePropertySources : event.getEnvironment().getPropertySources()) {
                if (mutablePropertySources instanceof MapPropertySource mapPropertySource) {
                    mapPropertySource.getSource().forEach((key, value) -> {
                        String stringValue = null;
                        if ((value instanceof OriginTrackedValue originTrackedValue &&
                                originTrackedValue.getValue() instanceof String v)) {
                            stringValue = v;
                        } else if (value instanceof String v) {
                            stringValue = v;
                        }

                        if (stringValue != null &&
                                stringValue.startsWith("{{hashicorp:") &&
                                stringValue.endsWith("}}")) {
                            LOG.debug("decrypting and overriding property {}", key);
                            try {
                                props.put(key, hashicorpVaultPropertiesFunction.apply(stringValue
                                        .replace("{{hashicorp:", "")
                                        .replace("}}", "")));
                            } catch (Exception e) {
                                // Log and do nothing
                                LOG.debug("failed to parse property {}. This exception is ignored.", key, e);
                            }
                        }
                    });
                }
            }

            environment.getPropertySources().addFirst(new PropertiesPropertySource("overridden-camel-hashicorp-vault-properties", props));
        }
    }
}
