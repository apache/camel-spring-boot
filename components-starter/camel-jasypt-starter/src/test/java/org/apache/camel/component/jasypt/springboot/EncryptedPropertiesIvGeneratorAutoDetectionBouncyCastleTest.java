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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.registry.AlgorithmRegistry;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.Security;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class EncryptedPropertiesIvGeneratorAutoDetectionBouncyCastleTest extends AbstractEncryptedPropertiesIvGeneratorAutoDetectionTest {

    public EncryptedPropertiesIvGeneratorAutoDetectionBouncyCastleTest() {
        provider = BOUNCY_CASTLE_PROVIDER_NAME;
    }

    @Before
    public void setUp(){
        Security.addProvider( new BouncyCastleProvider());
    }


    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> data() {
        return new BouncyCastleProvider().keySet()
                .stream()
                .filter(x->((String)x).startsWith("Cipher"))
                .map(x->((String) x).split("\\.",2)[1])
                .filter(x->x.startsWith("PBE"))
                .collect(Collectors.toList());
    }
}
