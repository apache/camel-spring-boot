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
package org.apache.camel.component.hashicorp.vault.springboot;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.camel.RuntimeCamelException;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringBootHashicorpVaultPropertiesParserTest {

    private final SpringBootHashicorpVaultPropertiesParser parser = new SpringBootHashicorpVaultPropertiesParser();

    /**
     * The integration tests in this module set {@code camel.vault.hashicorp.*} through
     * {@code System.setProperty}, and Surefire reuses the JVM across test classes. Dropping the system
     * property sources keeps these tests independent of execution order.
     */
    private static StandardEnvironment environmentWith(Map<String, ?> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources()
                .addFirst(new MapPropertySource("test-properties", new LinkedHashMap<>(properties)));
        return environment;
    }

    @Test
    public void guardPropertyIsUnchanged() {
        assertEquals("camel.component.hashicorp-vault.early-resolve-properties",
                parser.getEarlyResolutionProperty(),
                "a wrong guard key silently disables early resolution, leaving placeholders as literal values");
    }

    @Test
    public void overridePropertySourceNameIsUnchanged() {
        assertEquals("overridden-camel-hashicorp-vault-properties",
                parser.getOverridePropertySourceName(),
                "the property source name is observable through /actuator/env");
    }

    @Test
    public void missingTokenFailsAsACamelExceptionNamingTheProperty() {
        RuntimeCamelException thrown = assertThrows(RuntimeCamelException.class,
                () -> parser.createPropertiesFunction(environmentWith(Map.of())),
                "a missing setting is an operator error, not a programming defect, so it must not surface "
                        + "as NullPointerException");

        assertTrue(thrown.getMessage().contains("token"),
                "the failure must name the missing setting, was: " + thrown.getMessage());
    }

    @Test
    public void nonNumericPortFailsAsACamelExceptionNamingTheProperty() {
        RuntimeCamelException thrown = assertThrows(RuntimeCamelException.class,
                () -> parser.createPropertiesFunction(environmentWith(Map.of(
                        "camel.vault.hashicorp.token", "a-token",
                        "camel.vault.hashicorp.host", "127.0.0.1",
                        "camel.vault.hashicorp.port", "not-a-number",
                        "camel.vault.hashicorp.scheme", "http"))),
                "a bare NumberFormatException never says which property was malformed");

        assertTrue(thrown.getMessage().contains("camel.vault.hashicorp.port"),
                "the failure must name the malformed property, was: " + thrown.getMessage());
    }
}
