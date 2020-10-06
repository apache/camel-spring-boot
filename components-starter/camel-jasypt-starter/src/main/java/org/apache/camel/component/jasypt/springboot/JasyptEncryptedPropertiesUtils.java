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

import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.iv.IvGenerator;
import org.jasypt.iv.NoIvGenerator;
import org.jasypt.iv.RandomIvGenerator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.camel.util.ObjectHelper.isNotEmpty;
import static org.apache.camel.util.StringHelper.after;

public class JasyptEncryptedPropertiesUtils {

    private static final String SYSTEM_ENVIRONMENT_PREFIX = "sysenv:";

    private static final String SYSTEM_PROPERTIES_PREFIX = "sys:";

    /**
     *  Algorithms that mandatory require initialization vector
     */
    static final Set<String> ALGORITHMS_THAT_REQUIRE_IV = new HashSet<>(
            Arrays.asList(
                    "PBEWITHHMACSHA1ANDAES_128",
                    "PBEWITHHMACSHA1ANDAES_256",
                    "PBEWITHHMACSHA224ANDAES_128",
                    "PBEWITHHMACSHA224ANDAES_256",
                    "PBEWITHHMACSHA256ANDAES_128",
                    "PBEWITHHMACSHA256ANDAES_256",
                    "PBEWITHHMACSHA384ANDAES_128",
                    "PBEWITHHMACSHA384ANDAES_256",
                    "PBEWITHHMACSHA512ANDAES_128",
                    "PBEWITHHMACSHA512ANDAES_256"
            )
    );

    /**
     * test if algorithm requires an initialization vector
     * @param algorithm the algorithm to test
     * @return true if the algorithm requires initialization vector, false otherwise
     */
    static boolean isIVNeeded(String algorithm) {
        if (isNotBlank(algorithm)) {
            return ALGORITHMS_THAT_REQUIRE_IV.contains(algorithm.toUpperCase());
        }
        return false;
    }

    /**
     * Checks if a CharSequence is empty (""), null or whitespace only.
     * @param cs  the CharSequence to check, may be null
     * @return true if the CharSequence is null, empty or whitespace only
     */
    static boolean isBlank(final CharSequence cs) {
        final int strLen = cs == null ? 0 : cs.length();
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a CharSequence is not empty (""), not null or not whitespace only.
     * @param cs the CharSequence to check, may be null
     * @return true if the CharSequence is not null, not empty or not whitespace only
     */
    static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }


    static void parsePassword(EnvironmentStringPBEConfig environmentStringPBEConfig, JasyptEncryptedPropertiesConfiguration configuration) {
        String passwordReference = configuration.getPassword();
        if (isNotEmpty(passwordReference) && passwordReference.startsWith(SYSTEM_ENVIRONMENT_PREFIX)) {
            environmentStringPBEConfig.setPasswordEnvName(after(passwordReference, SYSTEM_ENVIRONMENT_PREFIX));
            return;
        }
        if (isNotEmpty(passwordReference) && passwordReference.startsWith(SYSTEM_PROPERTIES_PREFIX)) {
            environmentStringPBEConfig.setPasswordSysPropertyName(after(passwordReference, SYSTEM_PROPERTIES_PREFIX));
            return;
        }
        environmentStringPBEConfig.setPassword(passwordReference);
    }

    static IvGenerator getIVGenerator(JasyptEncryptedPropertiesConfiguration configuration) {
        String ivGeneratorClassName = configuration.getIvGeneratorClassName();
        String algorithm = configuration.getAlgorithm();
        if (isBlank(ivGeneratorClassName)) {
            return isIVNeeded(algorithm) ? new RandomIvGenerator() : new NoIvGenerator();
        }
        IvGenerator ivGenerator = loadClass(ivGeneratorClassName);
        return ivGenerator;
    }

    /**
     * search and load the class identified by className parameter
     * @param className fully qualified class name to be loaded
     * @return a new instance of type className
     */
    static <T> T loadClass(String className) {
        try {
            final Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            return (T) clazz.newInstance();
        } catch (Exception e) {
            throw new EncryptionInitializationException(e);
        }
    }
}
