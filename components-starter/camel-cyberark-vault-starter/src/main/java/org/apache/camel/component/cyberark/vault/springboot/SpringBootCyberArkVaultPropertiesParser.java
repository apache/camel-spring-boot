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
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.AbstractEarlyResolutionPropertiesParser;
import org.apache.camel.util.ObjectHelper;
import org.springframework.core.env.ConfigurableEnvironment;

public class SpringBootCyberArkVaultPropertiesParser extends AbstractEarlyResolutionPropertiesParser {

    @Override
    protected String getEarlyResolutionProperty() {
        return "camel.component.cyberark-vault.early-resolve-properties";
    }

    @Override
    protected String getOverridePropertySourceName() {
        return "overridden-camel-cyberark-vault-properties";
    }

    @Override
    protected PropertiesFunction createPropertiesFunction(ConfigurableEnvironment environment) {
        ConjurClient client;
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

        return new CyberArkVaultPropertiesFunction(client);
    }
}
