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
package org.apache.camel.component.hashicorp.vault.springboot;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.infra.hashicorp.vault.services.HashicorpServiceFactory;
import org.apache.camel.test.infra.hashicorp.vault.services.HashicorpVaultService;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultMount;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The early resolution logs in with the AppRole login of the {@code camel.vault.hashicorp} options, and revokes the
 * token of its login once the properties are resolved.
 */
@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = { EarlyResolvedPropertiesAppRoleTest.TestConfiguration.class },
        properties = {
                "camel.component.hashicorp-vault.early-resolve-properties=true",
                "early.resolved.property.approle={{hashicorp:secret:approle#string}}"
        })
class EarlyResolvedPropertiesAppRoleTest {

    @RegisterExtension
    static HashicorpVaultService service = HashicorpServiceFactory.createService();

    // the system properties are seen by the early resolution, and the JVM is shared with the other tests
    private static final List<String> OPTIONS = List.of(
            "camel.vault.hashicorp.host", "camel.vault.hashicorp.port", "camel.vault.hashicorp.scheme",
            "camel.vault.hashicorp.token", "camel.vault.hashicorp.authentication", "camel.vault.hashicorp.role-id",
            "camel.vault.hashicorp.secret-id");

    @BeforeAll
    static void setup() {
        VaultTemplate root = root();
        VaultKeyValueOperations secrets
                = root.opsForKeyValue("secret", VaultKeyValueOperationsSupport.KeyValueBackend.versioned());
        secrets.put("approle", Map.of("string", "from-approle"));
        // read by application.properties
        secrets.put("database/password", Map.of("string", "pazzword"));

        root.write("sys/policies/acl/early-resolution",
                Map.of("policy", "path \"secret/data/*\" { capabilities = [\"read\"] }"));
        if (!root.opsForSys().getAuthMounts().containsKey("approle/")) {
            root.opsForSys().authMount("approle", VaultMount.create("approle"));
        }
        root.write("auth/approle/role/camel", Map.of("token_policies", "early-resolution"));
        String roleId = (String) root.read("auth/approle/role/camel/role-id").getRequiredData().get("role_id");
        String secretId = (String) root.write("auth/approle/role/camel/secret-id", Map.of())
                .getRequiredData().get("secret_id");

        // no token: the AppRole login gets one
        OPTIONS.forEach(System::clearProperty);
        System.setProperty("camel.vault.hashicorp.host", service.host());
        System.setProperty("camel.vault.hashicorp.port", String.valueOf(service.port()));
        System.setProperty("camel.vault.hashicorp.scheme", "http");
        // the kebab-case names of Spring Boot
        System.setProperty("camel.vault.hashicorp.authentication", "approle");
        System.setProperty("camel.vault.hashicorp.role-id", roleId);
        System.setProperty("camel.vault.hashicorp.secret-id", secretId);
    }

    @AfterAll
    static void clearOptions() {
        OPTIONS.forEach(System::clearProperty);
    }

    @Value("${early.resolved.property}")
    private String earlyResolvedProperty;

    @Value("${early.resolved.property.approle}")
    private String earlyResolvedPropertyAppRole;

    @Test
    void resolvesThePropertiesWithTheAppRoleLoginAndRevokesItsToken() {
        assertEquals("pazzword", earlyResolvedProperty);
        assertEquals("from-approle", earlyResolvedPropertyAppRole);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(appRoleTokens().isEmpty(),
                "the token of the AppRole login must be revoked once the properties are resolved"));
    }

    private static VaultTemplate root() {
        VaultEndpoint endpoint = VaultEndpoint.create(service.host(), service.port());
        endpoint.setScheme("http");
        return new VaultTemplate(endpoint, new TokenAuthentication(service.token()));
    }

    // the accessors of the tokens that the AppRole login gave and Vault has not revoked
    private static List<String> appRoleTokens() {
        VaultTemplate root = root();
        return root.list("auth/token/accessors").stream()
                .filter(accessor -> "auth/approle/login".equals(
                        root.write("auth/token/lookup-accessor", Map.of("accessor", accessor))
                                .getRequiredData().get("path")))
                .toList();
    }

    @Configuration
    @AutoConfigureBefore(CamelAutoConfiguration.class)
    static class TestConfiguration {
    }
}
