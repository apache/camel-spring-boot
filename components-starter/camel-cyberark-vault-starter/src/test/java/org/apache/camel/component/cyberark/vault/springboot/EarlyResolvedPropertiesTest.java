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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = {EarlyResolvedPropertiesTest.TestConfiguration.class},
        properties = {
                "camel.component.cyberark-vault.early-resolve-properties=true",
                "early.resolved.property.simple={{cyberark:test/secret}}"
        })

/*
Must be manually tested. Provide your own credentials using system properties:

    -Dcamel.vault.test.cyberark.url=http://localhost:8080
    -Dcamel.vault.test.cyberark.account=myConjurAccount
    -Dcamel.vault.test.cyberark.username=admin
    -Dcamel.vault.test.cyberark.apiKey=your-api-key
*/
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

    static final Logger LOG = LoggerFactory.getLogger(EarlyResolvedPropertiesTest.class);

    record CyberarkClientConfig(
            String url, String account, String username, String apiKey) {
    }

    static HttpClient httpClient;

    @BeforeAll
    public static void setup() throws Exception {

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        var cfg = new CyberarkClientConfig(
                System.getProperty("camel.vault.test.cyberark.url"),
                System.getProperty("camel.vault.test.cyberark.account"),
                System.getProperty("camel.vault.test.cyberark.username"),
                System.getProperty("camel.vault.test.cyberark.apiKey")
        );

        System.setProperty("camel.vault.cyberark.url", cfg.url);
        System.setProperty("camel.vault.cyberark.account", cfg.account);
        System.setProperty("camel.vault.cyberark.username", cfg.username);
        System.setProperty("camel.vault.cyberark.apiKey", cfg.apiKey);

        // Create a test secret in CyberArk Conjur
        try (ConjurClient client = ConjurClientFactory.createWithApiKey(cfg.url, cfg.account, cfg.username, cfg.apiKey)) {

            loadPolicy(cfg, """
                    - !variable test/secret
                    """);

            client.createSecret("test/secret", "mySecretValue");
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
        Assertions.assertThat(earlyResolvedPropertySimple).isEqualTo("mySecretValue");
    }

    @Configuration
    @AutoConfigureBefore(CamelAutoConfiguration.class)
    public static class TestConfiguration {
    }

    static String authenticate(CyberarkClientConfig cfg) throws Exception {

        String url = String.format("%s/authn/%s/%s/authenticate",
                cfg.url, cfg.account, cfg.username);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/plain")
                .header("Accept-Encoding", "base64")
                .POST(HttpRequest.BodyPublishers.ofString(cfg.apiKey))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        requireSuccess("Authenticate", response);

        return response.body();
    }

    static void loadPolicy(CyberarkClientConfig cfg, String policy) throws Exception {

        String authToken = authenticate(cfg);

        String policyUrl = String.format("%s/policies/%s/policy/%s",
                cfg.url, cfg.account, "root");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(policyUrl))
                .header("Authorization", "Token token=\"" + authToken + "\"")
                .header("Content-Type", "text/plain")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(policy))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        requireSuccess("Load policy", response);
    }

    static void requireSuccess(String op, HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    String.format("%s failed: HTTP %d: %s", op, response.statusCode(), response.body()));
        }
        LOG.info("{} ok - HTTP {}", op, response.statusCode());
    }
}
