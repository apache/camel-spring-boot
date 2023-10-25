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

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.iv.NoIvGenerator;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import static org.apache.camel.component.jasypt.springboot.JasyptEncryptedPropertiesUtils.isIVNeeded;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractEncryptedPropertiesIvGeneratorAutoDetectionTest {

    protected  final Logger LOG = LoggerFactory.getLogger(this.getClass());

    protected static final String SUN_JCE_PROVIDER_NAME = "SunJCE";
    protected static final String BOUNCY_CASTLE_PROVIDER_NAME = "BC";


    String stringToEncrypt = "A password-cracker walks into a bar. Orders a beer. Then a Beer. Then a BEER. beer. b33r. BeeR. Be3r. bEeR. bE3R. BeEr";
    //String password = "s0m3R@nD0mP@ssW0rD";

    protected String provider;

    public static Properties loadAuthProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(AbstractEncryptedPropertiesIvGeneratorAutoDetectionTest.class.getClassLoader().getResourceAsStream("test.properties"));
        return properties;
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testEncryptionAndDecryption(String algorithm) throws IOException {

        LOG.info("Testing Algorithm: '{}', requires IV: {}", algorithm, isIVNeeded(algorithm));

        Properties properties = loadAuthProperties();

        // Create a ByteArrayOutputStream so that we can get the output
        // from the call to print
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Change System.out to point out to our stream
        System.setOut(new PrintStream(baos));

        EnvironmentStringPBEConfig environmentStringPBEConfig = new EnvironmentStringPBEConfig();
        environmentStringPBEConfig.setAlgorithm(algorithm);
        environmentStringPBEConfig.setIvGenerator(isIVNeeded(algorithm)?new RandomIvGenerator():new NoIvGenerator());
        environmentStringPBEConfig.setSaltGenerator(new RandomSaltGenerator());
        environmentStringPBEConfig.setProviderName(provider);
        environmentStringPBEConfig.setPassword(properties.getProperty("password"));

        StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
        standardPBEStringEncryptor.setConfig(environmentStringPBEConfig);

        // Testing Encryption.
        String encrypted = standardPBEStringEncryptor.encrypt(stringToEncrypt);

        // Testing Decryption:
        String actualDecriptedString = standardPBEStringEncryptor.decrypt(encrypted);

        //Assertions
        assertThat(actualDecriptedString).isEqualTo(stringToEncrypt);
    }

}
