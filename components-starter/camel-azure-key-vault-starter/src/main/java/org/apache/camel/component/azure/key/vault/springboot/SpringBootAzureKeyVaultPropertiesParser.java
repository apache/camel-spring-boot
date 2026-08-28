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
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.AbstractEarlyResolutionPropertiesParser;
import org.apache.camel.util.ObjectHelper;
import org.springframework.core.env.ConfigurableEnvironment;

public class SpringBootAzureKeyVaultPropertiesParser extends AbstractEarlyResolutionPropertiesParser {

    @Override
    protected String getEarlyResolutionProperty() {
        return "camel.component.azure-key-vault.early-resolve-properties";
    }

    @Override
    protected String getOverridePropertySourceName() {
        return "overridden-camel-azure-key-vault-properties";
    }

    @Override
    protected PropertiesFunction createPropertiesFunction(ConfigurableEnvironment environment) {
        SecretClient client;
        String vaultName = environment.getProperty("camel.vault.azure.vaultName");
        String clientId = environment.getProperty("camel.vault.azure.clientId");
        String clientSecret = environment.getProperty("camel.vault.azure.clientSecret");
        String tenantId = environment.getProperty("camel.vault.azure.tenantId");
        boolean azureIdentityEnabled = Boolean.parseBoolean(System.getenv("CAMEL_VAULT_AZURE_IDENTITY_ENABLED"));
        if (ObjectHelper.isNotEmpty(vaultName) && ObjectHelper.isNotEmpty(clientId)
                && ObjectHelper.isNotEmpty(clientSecret)
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
        return new KeyVaultPropertiesFunction(client);
    }
}
