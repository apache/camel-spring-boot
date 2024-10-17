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
package org.apache.camel.component.azure.key.vault.springboot;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.azure.key.vault.KeyVaultPropertiesFunction;
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

public class SpringBootAzureKeyVaultPropertiesParser implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootAzureKeyVaultPropertiesParser.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        SecretClient client;
        ConfigurableEnvironment environment = event.getEnvironment();
        if (Boolean.parseBoolean(environment.getProperty("camel.component.azure-key-vault.early-resolve-properties"))) {
            String vaultName = environment.getProperty("camel.vault.azure.vaultName");
            String clientId = environment.getProperty("camel.vault.azure.clientId");
            String clientSecret = environment.getProperty("camel.vault.azure.clientSecret");
            String tenantId = environment.getProperty("camel.vault.azure.tenantId");
            boolean azureIdentityEnabled = Boolean.parseBoolean(System.getenv("camel.vault.azure.azureIdentityEnabled"));
            if (ObjectHelper.isNotEmpty(vaultName) && ObjectHelper.isNotEmpty(clientId) && ObjectHelper.isNotEmpty(clientSecret)
                    && ObjectHelper.isNotEmpty(tenantId) && !azureIdentityEnabled) {
                String keyVaultUri = "https://" + vaultName + ".vault.azure.net";

                // Credential
                ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                        .tenantId(tenantId)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .build();

                // Build Client
                client = new SecretClientBuilder()
                        .vaultUrl(keyVaultUri)
                        .credential(credential)
                        .buildClient();
            } else if (ObjectHelper.isNotEmpty(vaultName) && azureIdentityEnabled) {
                String keyVaultUri = "https://" + vaultName + ".vault.azure.net";

                // Credential
                TokenCredential credential = new DefaultAzureCredentialBuilder().build();

                // Build Client
                client = new SecretClientBuilder()
                        .vaultUrl(keyVaultUri)
                        .credential(credential)
                        .buildClient();
            } else {
                throw new RuntimeCamelException(
                        "Using the Azure Key Vault Properties Function requires setting Azure credentials as application properties or environment variables or enable the Azure Identity Authentication mechanism");
            }
            KeyVaultPropertiesFunction keyVaultPropertiesFunction = new KeyVaultPropertiesFunction(client);
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
                                stringValue.startsWith("{{azure:") &&
                                stringValue.endsWith("}}")) {
                            LOG.debug("decrypting and overriding property {}", key);
                            try {
                                String element = keyVaultPropertiesFunction.apply(stringValue
                                        .replace("{{azure:", "")
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
            environment.getPropertySources().addFirst(new PropertiesPropertySource("overridden-camel-azure-key-vault-properties", props));
        }
    }
}
