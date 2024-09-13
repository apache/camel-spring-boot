package org.apache.camel.component.jasypt.springboot;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = { EncryptedPropertiesTest.TestConfiguration.class },
        properties = { "camel.component.jasypt.early-decryption-enabled=true" })
public class EarlyEncryptedPropertiesTest extends EncryptedPropertiesTest {
}
