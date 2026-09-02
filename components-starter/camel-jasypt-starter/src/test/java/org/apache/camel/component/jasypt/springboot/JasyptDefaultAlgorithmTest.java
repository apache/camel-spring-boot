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

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.iv.NoIvGenerator;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verifies the default encryption algorithm of the starter, and that values encrypted with the previous default can
 * still be read by pinning that algorithm explicitly.
 */
public class JasyptDefaultAlgorithmTest {

    private static final String DEFAULT_ALGORITHM = "PBEWITHHMACSHA256ANDAES_256";

    private static final String LEGACY_ALGORITHM = "PBEWithMD5AndDES";

    private static final String PLAIN_TEXT = "mysecret";

    private static final String MASTER_PASSWORD = "legacy-master-password";

    /**
     * {@value #PLAIN_TEXT} encrypted with {@value #LEGACY_ALGORITHM}, the master password {@value #MASTER_PASSWORD} and
     * no initialization vector generator, which is how the starter encrypted values before the default changed.
     */
    private static final String LEGACY_ENCRYPTED_VALUE = "+1thxaTtHh5z+yuyvFlCl5gafagmagQV";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JasyptEncryptedPropertiesAutoconfiguration.class))
            .withPropertyValues("camel.component.jasypt.password=" + MASTER_PASSWORD);

    @Test
    public void defaultAlgorithmRequiresAndGetsAnInitializationVector() {
        runner.run(context -> {
            assertThat(context.getBean(JasyptEncryptedPropertiesConfiguration.class).getAlgorithm())
                    .isEqualTo(DEFAULT_ALGORITHM);
            EnvironmentStringPBEConfig config = context.getBean(EnvironmentStringPBEConfig.class);
            assertThat(config.getAlgorithm()).isEqualTo(DEFAULT_ALGORITHM);
            assertThat(config.getIvGenerator()).isInstanceOf(RandomIvGenerator.class);
        });
    }

    @Test
    public void defaultAlgorithmEncryptsAndDecrypts() {
        runner.run(context -> {
            StringEncryptor encryptor = context.getBean(StringEncryptor.class);
            String encrypted = encryptor.encrypt(PLAIN_TEXT);
            assertThat(encrypted).isNotEqualTo(PLAIN_TEXT);
            assertThat(encryptor.decrypt(encrypted)).isEqualTo(PLAIN_TEXT);
        });
    }

    @Test
    public void legacyAlgorithmCanBePinnedExplicitly() {
        runner.withPropertyValues("camel.component.jasypt.algorithm=" + LEGACY_ALGORITHM).run(context -> {
            EnvironmentStringPBEConfig config = context.getBean(EnvironmentStringPBEConfig.class);
            assertThat(config.getAlgorithm()).isEqualTo(LEGACY_ALGORITHM);
            assertThat(config.getIvGenerator()).isInstanceOf(NoIvGenerator.class);
            assertThat(context.getBean(StringEncryptor.class).decrypt(LEGACY_ENCRYPTED_VALUE)).isEqualTo(PLAIN_TEXT);
        });
    }

    @Test
    public void legacyValueIsNotReadableUnderTheDefaultAlgorithm() {
        runner.run(context -> {
            StringEncryptor encryptor = context.getBean(StringEncryptor.class);
            assertThatExceptionOfType(EncryptionOperationNotPossibleException.class)
                    .isThrownBy(() -> encryptor.decrypt(LEGACY_ENCRYPTED_VALUE));
        });
    }
}
