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
package org.apache.camel.component.ibm.secrets.manager.springboot;

import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.secrets_manager_sdk.secrets_manager.v2.SecretsManager;
import com.ibm.cloud.secrets_manager_sdk.secrets_manager.v2.model.CreateSecretOptions;
import com.ibm.cloud.secrets_manager_sdk.secrets_manager.v2.model.DeleteSecretOptions;
import com.ibm.cloud.secrets_manager_sdk.secrets_manager.v2.model.KVSecretPrototype;
import com.ibm.cloud.secrets_manager_sdk.secrets_manager.v2.model.Secret;
import org.apache.camel.component.ibm.secrets.manager.IBMSecretsManagerConstants;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.apache.camel.util.ObjectHelper;
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
import java.util.HashMap;
import java.util.Map;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = { EarlyResolvedPropertiesTest.TestConfiguration.class },
        properties = {
                "camel.component.ibm-secrets-manager.early-resolve-properties=true",
                "early.resolved.property.simple={{ibm:default:databaseTestPassword#username}}"
        })

// Must be manually tested. Provide your own accessKey and secretKey using -Dsecrets-manager and -Dcamel.ibm.sm.serviceurl
@EnabledIfSystemProperties({
        @EnabledIfSystemProperty(named = "camel.ibm.test.sm.token", matches = ".*",
                disabledReason = "Secrets Manager Token not provided"),
        @EnabledIfSystemProperty(named = "camel.ibm.test.sm.serviceurl", matches = ".*",
                disabledReason = "Secrets Manager Service URL not provided")
})
public class EarlyResolvedPropertiesTest {

    static SecretsManager client;

    static String secretId = "";
    @BeforeAll
    public static void setup() throws IOException {
        String token = System.getProperty("camel.ibm.test.sm.token");
        String serviceUrl = System.getProperty("camel.ibm.test.sm.serviceurl");
        System.setProperty("camel.vault.ibm.token", token);
        System.setProperty("camel.vault.ibm.serviceUrl", serviceUrl);

        IamAuthenticator iamAuthenticator = new IamAuthenticator.Builder()
                .apikey(token)
                .build();
        client = new SecretsManager("Camel Secrets Manager Service for Properties", iamAuthenticator);
        client.setServiceUrl(serviceUrl);

        KVSecretPrototype.Builder kvSecretResourceBuilder = new KVSecretPrototype.Builder();
        kvSecretResourceBuilder
                    .name("databaseTestPassword");
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "admin");
        payload.put("password", "password");
        kvSecretResourceBuilder.data(payload);
        kvSecretResourceBuilder.secretType(KVSecretPrototype.SecretType.KV);
        KVSecretPrototype kvSecretResource = kvSecretResourceBuilder.build();

        CreateSecretOptions createSecretOptions = new CreateSecretOptions.Builder()
                .secretPrototype(kvSecretResource)
                .build();
        Response<Secret> createResp = client.createSecret(createSecretOptions).execute();

        secretId = createResp.getResult().getId();
    }

    @AfterAll
    public static void teardown() throws IOException {

        DeleteSecretOptions.Builder deleteSecretOptionsBuilder = new DeleteSecretOptions.Builder();
        deleteSecretOptionsBuilder.id(secretId);
        client.deleteSecret(deleteSecretOptionsBuilder.build()).execute();
    }

    @Value("${early.resolved.property}")
    private String earlyResolvedProperty;

    @Value("${early.resolved.property.simple}")
    private String earlyResolvedPropertySimple;

    @Test
    public void testEarlyResolvedProperties() {
        Assertions.assertThat(earlyResolvedProperty).isEqualTo("admin");
        Assertions.assertThat(earlyResolvedPropertySimple).isEqualTo("admin");
    }

    @Configuration
    @AutoConfigureBefore(CamelAutoConfiguration.class)
    public static class TestConfiguration {
    }
}
