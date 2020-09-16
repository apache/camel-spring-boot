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

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_ENCRYPTED_PROPS_IN_CC;
import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_ENCRYPTED_PROPS_OUT_CC;

@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = {EncryptedPropertiesDisabledCustomConfigurationBeansTest.TestConfiguration.class},
        properties = {
                "camel.component.jasypt.enabled = false",
                "encrypted.password=ENC(6q7H+bWqPbSZVW1hUzDVgnl7iSnC04zRmKwD31ounBMPM/2CtDS7fwb4u1OGZ2Q4)"})
public class EncryptedPropertiesDisabledCustomConfigurationBeansTest extends EncryptedProperiesTestBase {

    @Test
    public void testCustomEnvironmentVariablesConfiguration() {
        Assert.assertFalse(context.containsBean("environmentVariablesConfiguration"));
        Assert.assertTrue(context.containsBean("customEnvironmentStringPBEConfig"));
        Assert.assertTrue(context.containsBean("customStringEncryptor"));
        Assert.assertFalse(context.containsBean("stringEncryptor"));
        Assert.assertFalse(context.containsBean("propertyConfigurer"));
        Assert.assertFalse(context.containsBean("encryptedPropertiesParser"));
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
