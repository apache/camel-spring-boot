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
package org.apache.camel.component.google.secret.manager.springboot;

import com.google.cloud.secretmanager.v1.*;
import com.google.protobuf.ByteString;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
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
                "camel.component.google-secret-manager.early-resolve-properties=true",
                "early.resolved.property.simple={{gcp:databaseTestPassword}}"
        })

// Must be manually tested. Provide your own projectId using -Dcamel.vault.test.gcp.projectId and -Dcamel.vault.test.gcp.useDefaultInstance
@EnabledIfSystemProperties({
        @EnabledIfSystemProperty(named = "camel.vault.test.gcp.projectId", matches = ".*",
                disabledReason = "Access key not provided"),
        @EnabledIfSystemProperty(named = "camel.vault.test.gcp.useDefaultInstance", matches = ".*",
                disabledReason = "Secret key not provided"),
})
@EnabledIfEnvironmentVariables({
    @EnabledIfEnvironmentVariable(named="GOOGLE_APPLICATION_CREDENTIALS", matches = ".*", disabledReason = "No environment variables for google credentials set")
})
public class EarlyResolvedPropertiesTest {

    static SecretManagerServiceClient client;
    static String secretId;

    @BeforeAll
    public static void setup() throws IOException {
        String projectId = System.getProperty("camel.vault.test.gcp.projectId");
        String useDefaultInstance = System.getProperty("camel.vault.test.gcp.useDefaultInstance");
        System.setProperty("camel.vault.gcp.projectId", projectId);
        System.setProperty("camel.vault.gcp.useDefaultInstance", useDefaultInstance);

        SecretManagerServiceSettings settings = SecretManagerServiceSettings.newBuilder().build();
        client = SecretManagerServiceClient.create(settings);
        Secret secret = Secret.newBuilder()
                .setReplication(
                        Replication.newBuilder()
                                .setAutomatic(Replication.Automatic.newBuilder().build())
                                .build())
                .build();

        Secret createdSecret = client.createSecret(ProjectName.of(projectId), "databaseTestPassword", secret);

        SecretPayload payload = SecretPayload.newBuilder()
                .setData(ByteString.copyFromUtf8("string")).build();
        client.addSecretVersion(createdSecret.getName(), payload);
    }

    @AfterAll
    public static void teardown() throws IOException {
        String projectId = System.getProperty("camel.vault.test.gcp.projectId");

        client.deleteSecret(SecretName.of(projectId, "databaseTestPassword"));
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
