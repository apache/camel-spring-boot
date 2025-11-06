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
package org.apache.camel.component.cyberark.vault.springboot;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.cyberark.vault.CyberArkVaultPropertiesFunction;
import org.apache.camel.component.cyberark.vault.client.ConjurClient;
import org.apache.camel.component.cyberark.vault.client.ConjurClientFactory;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Properties;

public class SpringBootCyberArkVaultPropertiesParser implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootCyberArkVaultPropertiesParser.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConjurClient client;
        ConfigurableEnvironment environment = event.getEnvironment();
        if (Boolean.parseBoolean(environment.getProperty("camel.component.cyberark-vault.early-resolve-properties"))) {
            String url = environment.getProperty("camel.vault.cyberark.url");
            String account = environment.getProperty("camel.vault.cyberark.account");
            String username = environment.getProperty("camel.vault.cyberark.username");
            String password = environment.getProperty("camel.vault.cyberark.password");
            String apiKey = environment.getProperty("camel.vault.cyberark.apiKey");
            String authToken = environment.getProperty("camel.vault.cyberark.authToken");

            if (ObjectHelper.isNotEmpty(url) && ObjectHelper.isNotEmpty(account)) {
                // Create Conjur client based on authentication method
                if (ObjectHelper.isNotEmpty(authToken)) {
                    // Use pre-authenticated token
                    client = ConjurClientFactory.createWithToken(url, account, authToken);
                } else if (ObjectHelper.isNotEmpty(apiKey) && ObjectHelper.isNotEmpty(username)) {
                    // Use API key authentication
                    client = ConjurClientFactory.createWithApiKey(url, account, username, apiKey);
                } else if (ObjectHelper.isNotEmpty(username) && ObjectHelper.isNotEmpty(password)) {
                    // Use username/password authentication
                    client = ConjurClientFactory.createWithCredentials(url, account, username, password);
                } else {
                    throw new RuntimeCamelException(
                            "Using the CyberArk Conjur Vault Properties Function requires authentication credentials (authToken, apiKey, or username/password)");
                }
            } else {
                throw new RuntimeCamelException(
                        "Using the CyberArk Conjur Vault Properties Function requires setting URL and account as application properties or environment variables");
            }

            CyberArkVaultPropertiesFunction cyberArkVaultPropertiesFunction = new CyberArkVaultPropertiesFunction(client);

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
                                stringValue.startsWith("{{cyberark:") &&
                                stringValue.endsWith("}}")) {
                            LOG.debug("decrypting and overriding property {}", key);
                            try {
                                String element = cyberArkVaultPropertiesFunction.apply(stringValue
                                        .replace("{{cyberark:", "")
                                        .replace("}}", ""));
                                props.put(key, element);
                            } catch (Exception e) {
                                // Log and do nothing
                                LOG.debug("failed to parse property {}. This exception is ignored.", key, e);
                            }
                        }
                    });
                }
            }

            environment.getPropertySources().addFirst(new PropertiesPropertySource("overridden-camel-cyberark-vault-properties", props));
        }
    }
}
