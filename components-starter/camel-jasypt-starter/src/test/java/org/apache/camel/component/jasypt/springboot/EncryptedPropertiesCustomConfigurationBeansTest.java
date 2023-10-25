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

import java.io.IOException;
import java.util.Properties;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        classes = {EncryptedPropertiesCustomConfigurationBeansTest.TestConfiguration.class},
        properties = {"encrypted.password=ENC(6q7H+bWqPbSZVW1hUzDVgnl7iSnC04zRmKwD31ounBMPM/2CtDS7fwb4u1OGZ2Q4)"})
public class EncryptedPropertiesCustomConfigurationBeansTest extends EncryptedPropertiesTestBase {

    public static Properties loadAuthProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(EncryptedPropertiesCustomConfigurationBeansTest.class.getClassLoader().getResourceAsStream("test.properties"));
        return properties;
    }

    @Test
    public void testCustomEnvironmentVariablesConfiguration() {
        Assertions.assertFalse(context.containsBean("environmentVariablesConfiguration"));
        Assertions.assertTrue(context.containsBean("customEnvironmentStringPBEConfig"));

        Assertions.assertTrue(context.containsBean("customStringEncryptor"));
        Assertions.assertFalse(context.containsBean("stringEncryptor"));

    }

    @Configuration
    @Import(Routes.class)
    @AutoConfigureBefore(CamelAutoConfiguration.class)
    public static class TestConfiguration {
        
        private static String OS = System.getProperty("os.name");

        private String getSecureRandomAlgorithm() {
            String secureRandomAlgorithm = "NativePRNG";
            if (OS != null && OS.toLowerCase().indexOf("win") != -1) {
                secureRandomAlgorithm = "Windows-PRNG";
            }
            return secureRandomAlgorithm;
        }

        @Bean("customEnvironmentStringPBEConfig")
        public EnvironmentStringPBEConfig environmentVariablesConfiguration() throws IOException {
            Properties props = loadAuthProperties();

            EnvironmentStringPBEConfig environmentStringPBEConfig = new EnvironmentStringPBEConfig();
            environmentStringPBEConfig.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
            environmentStringPBEConfig.setIvGenerator(new RandomIvGenerator(getSecureRandomAlgorithm()));
            environmentStringPBEConfig.setSaltGenerator(new RandomSaltGenerator(getSecureRandomAlgorithm()));
            environmentStringPBEConfig.setPassword(props.getProperty("mainpassword"));
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
