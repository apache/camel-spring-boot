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
package org.apache.camel.component.jasypt.springboot;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_ENCRYPTED_PROPS_IN_CC;
import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_ENCRYPTED_PROPS_OUT_CC;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = {EncryptedPropertiesDisabledCustomConfigurationBeansTest.TestConfiguration.class},
        properties = {
                "camel.component.jasypt.enabled = false",
                "encrypted.password=ENC(6q7H+bWqPbSZVW1hUzDVgnl7iSnC04zRmKwD31ounBMPM/2CtDS7fwb4u1OGZ2Q4)"})
public class EncryptedPropertiesDisabledCustomConfigurationBeansTest extends EncryptedPropertiesTestBase {

    @Test
    public void testCustomEnvironmentVariablesConfiguration() {
        Assertions.assertFalse(context.containsBean("environmentVariablesConfiguration"));
        Assertions.assertTrue(context.containsBean("customEnvironmentStringPBEConfig"));
        Assertions.assertTrue(context.containsBean("customStringEncryptor"));
        Assertions.assertFalse(context.containsBean("stringEncryptor"));
        Assertions.assertFalse(context.containsBean("propertyConfigurer"));
        Assertions.assertFalse(context.containsBean("encryptedPropertiesParser"));
    }

    @Test
    public void testEncryptionInsideCamelContext() {
        testEncryption(START_URI_TEST_ENCRYPTED_PROPS_IN_CC, "ENC(6q7H+bWqPbSZVW1hUzDVgnl7iSnC04zRmKwD31ounBMPM/2CtDS7fwb4u1OGZ2Q4)");
    }

    @Test
    public void testEncryptionOutsideCamelContext() {
        testEncryption(START_URI_TEST_ENCRYPTED_PROPS_OUT_CC, "ENC(6q7H+bWqPbSZVW1hUzDVgnl7iSnC04zRmKwD31ounBMPM/2CtDS7fwb4u1OGZ2Q4)");
    }

    @Configuration
    @Import({Routes.class,EncryptedPropertiesCustomConfigurationBeansTest.TestConfiguration.class})
    public static class TestConfiguration {}
}
