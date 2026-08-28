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
package org.apache.camel.component.ibm.secrets.manager.springboot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IBMSecretsManagerVaultPropertiesParserTest {

    private final IBMSecretsManagerVaultPropertiesParser parser = new IBMSecretsManagerVaultPropertiesParser();

    @Test
    public void guardPropertyIsUnchanged() {
        assertEquals("camel.component.ibm-secrets-manager.early-resolve-properties",
                parser.getEarlyResolutionProperty(),
                "a wrong guard key silently disables early resolution, leaving placeholders as literal values");
    }

    @Test
    public void overridePropertySourceNameKeepsItsHistoricalSpelling() {
        assertEquals("overridden-ibm-secrets-manager-properties",
                parser.getOverridePropertySourceName(),
                "this name lacks the camel- segment the other starters use; it is observable through "
                        + "/actuator/env so it must not be normalised");
    }
}
