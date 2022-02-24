package org.apache.camel.spring.boot.vault;

import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(
        classes = {
                AwsVaultConfigurationTest.class},
        properties = {
                "camel.vault.aws.accessKey=myAccessKey",
                "camel.vault.aws.secretKey=mySecretKey",
                "camel.vault.aws.region=myRegion",
                "camel.vault.aws.defaultCredentialsProvider=true"}
)
public class AwsVaultConfigurationTest {

    @Autowired
    private CamelContext camelContext;

    @Test
    public void testAwsVault() throws Exception {
        Assertions.assertEquals("myAccessKey", camelContext.getVaultConfiguration().aws().getAccessKey());
        Assertions.assertEquals("mySecretKey", camelContext.getVaultConfiguration().aws().getSecretKey());
        Assertions.assertEquals("myRegion", camelContext.getVaultConfiguration().aws().getRegion());
        Assertions.assertEquals(true, camelContext.getVaultConfiguration().aws().isDefaultCredentialsProvider());
    }
}
