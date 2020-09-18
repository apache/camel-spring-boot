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

import org.apache.camel.component.properties.PropertiesParser;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.jasypt.encryption.StringEncryptor;
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
import org.springframework.core.env.PropertyResolver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = {EncryptedPropertiesCustomConfigurationBeansTest.TestConfiguration.class},
        properties = {"encrypted.password=ENC(6q7H+bWqPbSZVW1hUzDVgnl7iSnC04zRmKwD31ounBMPM/2CtDS7fwb4u1OGZ2Q4)"})
public class EncryptedPropertiesCustomConfigurationBeansTest extends EncryptedProperiesTestBase {



    @Test
    public void testCustomEnvironmentVariablesConfiguration() {
        Assert.assertFalse(context.containsBean("environmentVariablesConfiguration"));
        Assert.assertTrue(context.containsBean("customEnvironmentStringPBEConfig"));

        Assert.assertTrue(context.containsBean("customStringEncryptor"));
        Assert.assertFalse(context.containsBean("stringEncryptor"));

    }

    @Configuration
    @Import(Routes.class)
    @AutoConfigureBefore(CamelAutoConfiguration.class)
    public static class TestConfiguration {

        @Bean("customEnvironmentStringPBEConfig")
        public EnvironmentStringPBEConfig environmentVariablesConfiguration() {
            EnvironmentStringPBEConfig environmentStringPBEConfig = new EnvironmentStringPBEConfig();
            environmentStringPBEConfig.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
            environmentStringPBEConfig.setIvGenerator(new RandomIvGenerator("NativePRNG"));
            environmentStringPBEConfig.setSaltGenerator(new RandomSaltGenerator("NativePRNG"));
            environmentStringPBEConfig.setPassword("mainpassword");
            return environmentStringPBEConfig;
        }

        @Bean("customStringEncryptor")
        public StandardPBEStringEncryptor stringEncryptor(EnvironmentStringPBEConfig environmentVariablesConfiguration) {
            StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
            standardPBEStringEncryptor.setConfig(environmentVariablesConfiguration);
            return standardPBEStringEncryptor;
        }
    }
}
