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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.component.jasypt")
public class JasyptEncryptedPropertiesConfiguration {

    static final String PREFIX = "camel.component.jasypt";

    /**
     * Enable the component
     */
    private boolean enabled;

    /**
     * Enable the early properties decryption during Spring Start Up.
     * Enabling this feature, encrypted properties can be decrypted before the Spring Boot AutoConfiguration
     * kicks in, for example, server.port=ENC(oBpQDDUvFY0c4WNAG0o4LIS5bWqmlxYlUUDTW2iXJIAZFYvM+3vOredaMcVfL4xW)
     * will be decrypted to 8082, and the application will start using that port.
     */
    private boolean earlyDecryptionEnabled;

    /**
     * The algorithm to be used for decryption. Default: PBEWithMD5AndDES
     */
    private String algorithm = "PBEWithMD5AndDES";

    /**
     * The master password used by Jasypt for decrypting the values. This option supports prefixes which influence the
     * master password lookup behaviour: sysenv: means to lookup the OS system environment with the given key. sys:
     * means to lookup a JVM system property.
     */
    private String password;

    /**
     * The initialization vector (IV) generator applied in decryption operations. Default: org.jasypt.iv.
     */
    private String ivGeneratorClassName;

    /**
     * The salt generator applied in decryption operations. Default: org.jasypt.salt.RandomSaltGenerator
     */
    private String saltGeneratorClassName = "org.jasypt.salt.RandomSaltGenerator";

    /**
     * The algorithm for the random iv generator
     */
    private String randomIvGeneratorAlgorithm = "SHA1PRNG";

    /**
     * The algorithm for the salt generator
     */
    private String randomSaltGeneratorAlgorithm = "SHA1PRNG";

    /**
     * The class name of the security provider to be used for obtaining the encryption algorithm.
     */
    private String providerName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEarlyDecryptionEnabled() {
        return earlyDecryptionEnabled;
    }

    public void setEarlyDecryptionEnabled(boolean earlyDecryptionEnabled) {
        this.earlyDecryptionEnabled = earlyDecryptionEnabled;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIvGeneratorClassName() {
        return ivGeneratorClassName;
    }

    public void setIvGeneratorClassName(String ivGeneratorClassName) {
        this.ivGeneratorClassName = ivGeneratorClassName;
    }

    public String getSaltGeneratorClassName() {
        return saltGeneratorClassName;
    }

    public void setSaltGeneratorClassName(String saltGeneratorClassName) {
        this.saltGeneratorClassName = saltGeneratorClassName;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getRandomIvGeneratorAlgorithm() {
        return randomIvGeneratorAlgorithm;
    }

    public void setRandomIvGeneratorAlgorithm(String randomIvGeneratorAlgorithm) {
        this.randomIvGeneratorAlgorithm = randomIvGeneratorAlgorithm;
    }

    public String getRandomSaltGeneratorAlgorithm() {
        return randomSaltGeneratorAlgorithm;
    }

    public void setRandomSaltGeneratorAlgorithm(String randomSaltGeneratorAlgorithm) {
        this.randomSaltGeneratorAlgorithm = randomSaltGeneratorAlgorithm;
    }
}
