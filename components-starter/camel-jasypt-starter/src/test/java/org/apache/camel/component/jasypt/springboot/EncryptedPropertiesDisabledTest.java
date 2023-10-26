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


import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_ENCRYPTED_PROPS_IN_CC;
import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_ENCRYPTED_PROPS_OUT_CC;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
        properties = {"camel.component.jasypt.enabled = false"},
        classes = {EncryptedPropertiesCustomConfigurationBeansTest.TestConfiguration.class})
public class EncryptedPropertiesDisabledTest extends EncryptedPropertiesTestBase{


    /**
     * Disabling the encryption, properties will not fm decrypted
     */
    @Test
    public void testEncryptionInsideCamelContext() {
        testEncryption(START_URI_TEST_ENCRYPTED_PROPS_IN_CC, "ENC(ngTGZvEjfnNnKMTrbRCR3tHEnFShMGdBSgfW5K9mlg23u+ygbtNCgJGmDriQBVcB)");
    }

    @Test
    public void testEncryptionOutsideCamelContext() {
        testEncryption(START_URI_TEST_ENCRYPTED_PROPS_OUT_CC, "ENC(ngTGZvEjfnNnKMTrbRCR3tHEnFShMGdBSgfW5K9mlg23u+ygbtNCgJGmDriQBVcB)");
    }
}
