package org.apache.camel.component.hashicorp.vault.springboot;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.infra.hashicorp.vault.services.HashicorpServiceFactory;
import org.apache.camel.test.infra.hashicorp.vault.services.HashicorpVaultService;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.assertj.core.api.Assertions;
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

import java.util.Map;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = { EarlyResolvedPropertiesTest.TestConfiguration.class },
        properties = {
                "camel.component.hashicorp-vault.early-resolve-properties=true",
                "early.resolved.property.simple={{hashicorp:secret:simple#string}}"
        })
public class EarlyResolvedPropertiesTest {

    @RegisterExtension
    public static HashicorpVaultService service = HashicorpServiceFactory.createService();

    @BeforeAll
    public static void setup() {
        System.setProperty("camel.vault.hashicorp.host", service.host());
        System.setProperty("camel.vault.hashicorp.port", String.valueOf(service.port()));
        System.setProperty("camel.vault.hashicorp.scheme", "http");
        System.setProperty("camel.vault.hashicorp.token", service.token());

        VaultEndpoint vaultEndpoint = new VaultEndpoint();
        vaultEndpoint.setHost(service.host());
        vaultEndpoint.setPort(service.port());
        vaultEndpoint.setScheme("http");

        VaultTemplate client = new VaultTemplate(
                vaultEndpoint,
                new TokenAuthentication(service.token()));
        VaultKeyValueOperations vaultKeyValueOperations = client.opsForKeyValue("secret", VaultKeyValueOperationsSupport.KeyValueBackend.versioned());
        vaultKeyValueOperations.put("simple", Map.of("string", "test"));
        vaultKeyValueOperations.put("database/password", Map.of("string", "pazzword"));
    }

    @Value("${early.resolved.property}")
    private String earlyResolvedProperty;

    @Value("${early.resolved.property.simple}")
    private String earlyResolvedPropertySimple;

    @Test
    public void testEarlyResolvedProperties() {
        Assertions.assertThat(earlyResolvedProperty).isEqualTo("pazzword");
        Assertions.assertThat(earlyResolvedPropertySimple).isEqualTo("test");
    }

    @Configuration
    @AutoConfigureBefore(CamelAutoConfiguration.class)
    public static class TestConfiguration {
    }
}