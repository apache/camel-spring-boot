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

import org.apache.camel.component.cyberark.vault.client.ConjurClient;
import org.apache.camel.component.cyberark.vault.client.ConjurClientFactory;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
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

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = { EarlyResolvedPropertiesTest.TestConfiguration.class },
        properties = {
                "camel.component.cyberark-vault.early-resolve-properties=true",
                "early.resolved.property.simple={{cyberark:test/secret}}"
        })

// Must be manually tested. Provide your own credentials using system properties:
// -Dcamel.vault.test.cyberark.url, -Dcamel.vault.test.cyberark.account,
// -Dcamel.vault.test.cyberark.username, -Dcamel.vault.test.cyberark.apiKey
@EnabledIfSystemProperties({
        @EnabledIfSystemProperty(named = "camel.vault.test.cyberark.url", matches = ".*",
                disabledReason = "CyberArk Conjur URL not provided"),
        @EnabledIfSystemProperty(named = "camel.vault.test.cyberark.account", matches = ".*",
                disabledReason = "CyberArk Conjur account not provided"),
        @EnabledIfSystemProperty(named = "camel.vault.test.cyberark.username", matches = ".*",
                disabledReason = "Username not provided"),
        @EnabledIfSystemProperty(named = "camel.vault.test.cyberark.apiKey", matches = ".*",
                disabledReason = "API key not provided"),
})
public class EarlyResolvedPropertiesTest {

    @BeforeAll
    public static void setup() {
        String url = System.getProperty("camel.vault.test.cyberark.url");
        String account = System.getProperty("camel.vault.test.cyberark.account");
        String username = System.getProperty("camel.vault.test.cyberark.username");
        String apiKey = System.getProperty("camel.vault.test.cyberark.apiKey");

        System.setProperty("camel.vault.cyberark.url", url);
        System.setProperty("camel.vault.cyberark.account", account);
        System.setProperty("camel.vault.cyberark.username", username);
        System.setProperty("camel.vault.cyberark.apiKey", apiKey);

        // Create a test secret in CyberArk Conjur
        ConjurClient client = ConjurClientFactory.createWithApiKey(url, account, username, apiKey);
        try {
            // Note: Creating secrets in Conjur requires proper permissions and policy setup
            // This is a placeholder - actual secret creation depends on Conjur policy configuration
            // In a real test, you would need to create the secret "test/secret" with value "testValue"
            // through Conjur CLI or API with appropriate permissions
        } catch (Exception e) {
            // Log or handle exception
            System.err.println("Warning: Could not create test secret. Ensure 'test/secret' exists in Conjur: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @AfterAll
    public static void teardown() {
        // Clean up test properties
        System.clearProperty("camel.vault.cyberark.url");
        System.clearProperty("camel.vault.cyberark.account");
        System.clearProperty("camel.vault.cyberark.username");
        System.clearProperty("camel.vault.cyberark.apiKey");

        // Note: CyberArk Conjur secrets cleanup would require proper permissions
        // This is typically handled through policy management rather than programmatic deletion
    }

    @Value("${early.resolved.property.simple}")
    private String earlyResolvedPropertySimple;

    @Test
    public void testEarlyResolvedProperties() {
        // Verify that the property was resolved from CyberArk Conjur vault
        // The actual value depends on what's stored in the 'test/secret' in your Conjur instance
        Assertions.assertThat(earlyResolvedPropertySimple).isNotNull();
        Assertions.assertThat(earlyResolvedPropertySimple).isNotEmpty();
    }

    @Configuration
    @AutoConfigureBefore(CamelAutoConfiguration.class)
    public static class TestConfiguration {
    }
}
