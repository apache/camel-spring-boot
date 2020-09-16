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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JasyptEncryptedPropertiesUtils {

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

    static boolean isIVNeeded(String algorithm) {
        return ALGORITHMS_THAT_REQUIRE_IV.contains(algorithm.toUpperCase());
    }
}
