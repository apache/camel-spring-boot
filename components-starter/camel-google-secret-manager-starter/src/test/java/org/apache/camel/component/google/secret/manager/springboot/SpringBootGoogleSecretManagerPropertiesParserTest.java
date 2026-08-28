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
package org.apache.camel.component.google.secret.manager.springboot;

import org.apache.camel.RuntimeCamelException;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SpringBootGoogleSecretManagerPropertiesParserTest {

    private final SpringBootGoogleSecretManagerPropertiesParser parser
            = new SpringBootGoogleSecretManagerPropertiesParser();

    @Test
    public void guardPropertyIsUnchanged() {
        assertEquals("camel.component.google-secret-manager.early-resolve-properties",
                parser.getEarlyResolutionProperty(),
                "a wrong guard key silently disables early resolution, leaving placeholders as literal values");
    }

    @Test
    public void overridePropertySourceNameIsUnchanged() {
        assertEquals("overridden-camel-google-secret-manager-properties",
                parser.getOverridePropertySourceName(),
                "the property source name is observable through /actuator/env");
    }

    /**
     * The integration test in this module sets {@code camel.vault.gcp.*} through {@code System.setProperty},
     * and Surefire reuses the JVM across test classes. Dropping the system property sources keeps this test
     * independent of execution order.
     */
    private static StandardEnvironment isolatedEnvironment() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        return environment;
    }

    @Test
    public void missingConfigurationFailsAsACamelException() {
        assertThrows(RuntimeCamelException.class,
                () -> parser.createPropertiesFunction(isolatedEnvironment()),
                "configuration problems must surface as RuntimeCamelException so operators and callers can "
                        + "treat them as one category");
    }
}
