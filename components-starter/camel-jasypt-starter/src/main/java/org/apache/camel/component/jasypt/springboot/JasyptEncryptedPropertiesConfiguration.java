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

import org.springframework.beans.factory.annotation.Value;

public class JasyptEncryptedPropertiesConfiguration {

    static final String PREFIX = "camel.component.jasypt";

    /**
     * Enable the component
     */
    @Value("${camel.component.jasypt.enabled}")
    private boolean enabled;

    /**
     * The algorithm to be used for decryption. Default: PBEWithMD5AndDES
     */
    @Value("${camel.component.jasypt.algorithm}")
    private String algorithm = "PBEWithMD5AndDES";

    /**
     * The master password used by Jasypt for decrypting the values.
     * This option supports prefixes which influence the master password lookup behaviour:
     * sysenv: means to lookup the OS system environment with the given key.
     * sys: means to lookup a JVM system property.
     */
    @Value("${camel.component.jasypt.password}")
    private String password;

    /**
     * The initialization vector (IV) generator applied in decryption operations.
     * Default: org.jasypt.iv.
     */
    @Value("${camel.component.jasypt.iv-generator-class-name}")
    private String ivGeneratorClassName;

    /**
     * The salt generator applied in decryption operations. Default: org.jasypt.salt.RandomSaltGenerator
     */
    @Value("${camel.component.jasypt.salt-generator-class-name}")
    private String saltGeneratorClassName = "org.jasypt.salt.RandomSaltGenerator";

    /**
     * The class name of the security provider to be used for obtaining the encryption
     * algorithm.
     */
    @Value("${camel.component.jasypt.provider-class-name}")
    private String providerClassName = "com.sun.crypto.provider.SunJCE";


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getProviderClassName() {
        return providerClassName;
    }

    public void setProviderClassName(String providerClassName) {
        this.providerClassName = providerClassName;
    }
}

