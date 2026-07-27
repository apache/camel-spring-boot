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
package org.apache.camel.spring.boot.actuate.console;

import java.util.Map;

import org.apache.camel.console.DevConsole;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringBootConfigurationMetadataDevConsoleTest {

    private SpringBootConfigurationMetadataDevConsole console;

    @BeforeEach
    void setUp() {
        console = new SpringBootConfigurationMetadataDevConsole();
    }

    @Test
    void testConsoleMetadata() {
        assertEquals("spring", console.getGroup());
        assertEquals("spring-boot-configuration", console.getId());
        assertEquals("Spring Boot Configuration", console.getDisplayName());
    }

    @Test
    void testJsonReturnsProperties() {
        JsonObject result = (JsonObject) console.call(DevConsole.MediaType.JSON, Map.of());

        assertNotNull(result);
        JsonArray properties = (JsonArray) result.get("properties");
        assertNotNull(properties, "Should have a 'properties' array");
        assertFalse(properties.isEmpty(), "Should find properties from classpath metadata");
    }

    @Test
    void testJsonContainsCamelProperties() {
        JsonObject result = (JsonObject) console.call(DevConsole.MediaType.JSON, Map.of());
        JsonArray properties = (JsonArray) result.get("properties");

        boolean found = false;
        for (Object item : properties) {
            JsonObject prop = (JsonObject) item;
            String name = prop.getString("name");
            if (name != null && name.startsWith("camel.")) {
                found = true;
                assertNotNull(prop.getString("type"), "Property should have a type");
                break;
            }
        }
        assertTrue(found, "Should find camel.* properties from this project's own metadata");
    }

    @Test
    void testFilterOption() {
        JsonObject all = (JsonObject) console.call(DevConsole.MediaType.JSON, Map.of());
        JsonArray allProps = (JsonArray) all.get("properties");

        JsonObject filtered = (JsonObject) console.call(DevConsole.MediaType.JSON,
                Map.of("filter", "camel.main.*"));
        JsonArray filteredProps = (JsonArray) filtered.get("properties");

        assertFalse(filteredProps.isEmpty(), "Filter should return matching properties");
        assertTrue(filteredProps.size() < allProps.size(),
                "Filtered result should be smaller than unfiltered");

        for (Object item : filteredProps) {
            JsonObject prop = (JsonObject) item;
            assertTrue(prop.getString("name").startsWith("camel.main."),
                    "Filtered properties should match the pattern");
        }
    }

    @Test
    void testTextOutput() {
        String text = (String) console.call(DevConsole.MediaType.TEXT, Map.of());

        assertNotNull(text);
        assertTrue(text.startsWith("Spring Boot Configuration Properties:"));
        assertTrue(text.contains("camel."), "Text output should contain camel properties");
    }

    @Test
    void testPropertyHasExpectedFields() {
        JsonObject result = (JsonObject) console.call(DevConsole.MediaType.JSON,
                Map.of("filter", "camel.main.name"));
        JsonArray properties = (JsonArray) result.get("properties");

        assertFalse(properties.isEmpty(), "Should find camel.main.name property");
        JsonObject prop = (JsonObject) properties.get(0);
        assertEquals("camel.main.name", prop.getString("name"));
        assertNotNull(prop.getString("description"), "Should have description");
        assertNotNull(prop.getString("type"), "Should have type");
    }
}
