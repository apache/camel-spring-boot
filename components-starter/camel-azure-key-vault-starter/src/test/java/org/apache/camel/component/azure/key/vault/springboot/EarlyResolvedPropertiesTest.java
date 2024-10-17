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

import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.DeletedSecret;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperties;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = { EarlyResolvedPropertiesTest.TestConfiguration.class },
        properties = {
                "camel.component.azure-key-vault.early-resolve-properties=true",
                "early.resolved.property.simple={{azure:dbTestPassword}}"
        })

// Must be manually tested. Provide your own projectId using -Dcamel.vault.test.azure.tenantId -Dcamel.vault.test.azure.clientId -Dcamel.vault.test.azure.clientSecret -Dcamel.vault.test.azure.vaultName
@EnabledIfSystemProperties({
        @EnabledIfSystemProperty(named = "camel.vault.test.azure.tenantId", matches = ".*",
                disabledReason = "Azure Tenant Id not provided"),
        @EnabledIfSystemProperty(named = "camel.vault.test.azure.clientId", matches = ".*",
                disabledReason = "Azure Key Vault Client Id not provided"),
        @EnabledIfSystemProperty(named = "camel.vault.test.azure.clientSecret", matches = ".*",
                disabledReason = "Azure Key Vault Client Secret not provided"),
        @EnabledIfSystemProperty(named = "camel.vault.test.azure.vaultName", matches = ".*",
                disabledReason = "Azure Key Vault Name not provided"),
})
public class EarlyResolvedPropertiesTest {

    static SecretClient client;
    static String secretId;

    @BeforeAll
    public static void setup() throws IOException {
        String tenantId = System.getProperty("camel.vault.test.azure.tenantId");
        String clientId = System.getProperty("camel.vault.test.azure.clientId");
        String clientSecret = System.getProperty("camel.vault.test.azure.clientSecret");
        String vaultName = System.getProperty("camel.vault.test.azure.vaultName");
        System.setProperty("camel.vault.azure.tenantId", tenantId);
        System.setProperty("camel.vault.azure.clientId", clientId);
        System.setProperty("camel.vault.azure.clientSecret", clientSecret);
        System.setProperty("camel.vault.azure.vaultName", vaultName);

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

        KeyVaultSecret p = client.
                setSecret(new KeyVaultSecret("dbTestPassword", "string"));
    }

    @AfterAll
    public static void teardown() throws IOException {
        SyncPoller<DeletedSecret, Void> p = client
                .beginDeleteSecret("dbTestPassword");
        p.waitForCompletion();
        client.purgeDeletedSecret("dbTestPassword");
    }

    @Value("${early.resolved.property}")
    private String earlyResolvedProperty;

    @Value("${early.resolved.property.simple}")
    private String earlyResolvedPropertySimple;

    @Test
    public void testEarlyResolvedProperties() {
        Assertions.assertThat(earlyResolvedProperty).isEqualTo("string");
        Assertions.assertThat(earlyResolvedPropertySimple).isEqualTo("string");
    }

    @Configuration
    @AutoConfigureBefore(CamelAutoConfiguration.class)
    public static class TestConfiguration {
    }
}
