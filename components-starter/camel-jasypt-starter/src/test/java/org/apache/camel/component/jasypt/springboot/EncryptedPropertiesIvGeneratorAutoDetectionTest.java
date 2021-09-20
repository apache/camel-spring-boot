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

import org.jasypt.registry.AlgorithmRegistry;

import java.security.Security;
import java.util.Collection;
import java.util.Set;

public class EncryptedPropertiesIvGeneratorAutoDetectionTest extends AbstractEncryptedPropertiesIvGeneratorAutoDetectionTest{


    public EncryptedPropertiesIvGeneratorAutoDetectionTest() {
        provider = SUN_JCE_PROVIDER_NAME;
    }

    public static Collection<String> data() {
        Security.removeProvider(BOUNCY_CASTLE_PROVIDER_NAME);
        return (Set<String>) AlgorithmRegistry.getAllPBEAlgorithms();
    }



}
