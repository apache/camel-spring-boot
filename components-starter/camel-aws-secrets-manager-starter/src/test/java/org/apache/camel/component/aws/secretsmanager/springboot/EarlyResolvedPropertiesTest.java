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
package org.apache.camel.component.aws.secretsmanager.springboot;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperties;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import org.assertj.core.api.Assertions;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = { EarlyResolvedPropertiesTest.TestConfiguration.class },
        properties = {
                "camel.component.aws-secrets-manager.early-resolve-properties=true",
                "early.resolved.property.simple={{aws:databaseTest/password}}"
        })

// Must be manually tested. Provide your own accessKey and secretKey using -Dcamel.vault.aws.accessKey, -Dcamel.vault.aws.secretKey and -Dcamel.vault.aws.region
@EnabledIfSystemProperties({
        @EnabledIfSystemProperty(named = "camel.vault.test.aws.accessKey", matches = ".*",
                disabledReason = "Access key not provided"),
        @EnabledIfSystemProperty(named = "camel.vault.test.aws.secretKey", matches = ".*",
                disabledReason = "Secret key not provided"),
        @EnabledIfSystemProperty(named = "camel.vault.test.aws.region", matches = ".*", disabledReason = "Region not provided"),
})
public class EarlyResolvedPropertiesTest {

    @BeforeAll
    public static void setup() {
        String accessKey = System.getProperty("camel.vault.test.aws.accessKey");
        String secretKey = System.getProperty("camel.vault.test.aws.secretKey");
        String region = System.getProperty("camel.vault.test.aws.region");
        System.setProperty("camel.vault.aws.accessKey", accessKey);
        System.setProperty("camel.vault.aws.secretKey", secretKey);
        System.setProperty("camel.vault.aws.region", region);

        SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder();
        AwsBasicCredentials cred = AwsBasicCredentials.create(accessKey, secretKey);
        clientBuilder = clientBuilder.credentialsProvider(StaticCredentialsProvider.create(cred));
        clientBuilder.region(Region.of(region));
        SecretsManagerClient client = clientBuilder.build();
        CreateSecretRequest req = CreateSecretRequest.builder().name("databaseTest/password").secretString("string").build();
        client.createSecret(req);
    }

    @AfterAll
    public static void teardown() {
        String accessKey = System.getProperty("camel.vault.test.aws.accessKey");
        String secretKey = System.getProperty("camel.vault.test.aws.secretKey");
        String region = System.getProperty("camel.vault.test.aws.region");

        SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder();
        AwsBasicCredentials cred = AwsBasicCredentials.create(accessKey, secretKey);
        clientBuilder = clientBuilder.credentialsProvider(StaticCredentialsProvider.create(cred));
        clientBuilder.region(Region.of(region));
        SecretsManagerClient client = clientBuilder.build();
        DeleteSecretRequest req = DeleteSecretRequest.builder().secretId("databaseTest/password").forceDeleteWithoutRecovery(true).build();
        client.deleteSecret(req);
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
