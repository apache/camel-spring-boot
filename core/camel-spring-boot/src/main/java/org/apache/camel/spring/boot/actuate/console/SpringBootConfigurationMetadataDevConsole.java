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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.spi.Metadata;
import org.apache.camel.support.PatternHelper;
import org.apache.camel.support.console.AbstractDevConsole;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev console that scans all {@code META-INF/spring-configuration-metadata.json} resources on the classpath, merges
 * them, and exposes the combined property metadata as JSON. This allows the TUI to provide quick docs for Spring Boot
 * properties (e.g. {@code server.port}, {@code spring.datasource.url}) alongside the existing Camel catalog metadata.
 */
public class SpringBootConfigurationMetadataDevConsole extends AbstractDevConsole {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootConfigurationMetadataDevConsole.class);

    private static final String METADATA_RESOURCE = "META-INF/spring-configuration-metadata.json";

    @Metadata(label = "query", description = "Filters the properties matching by name pattern", javaType = "java.lang.String")
    public static final String FILTER = "filter";

    private volatile List<JsonObject> cachedProperties;

    public SpringBootConfigurationMetadataDevConsole() {
        super("spring", "spring-boot-configuration", "Spring Boot Configuration",
                "Displays Spring Boot configuration metadata from classpath");
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        cachedProperties = null;
    }

    private List<JsonObject> getProperties() {
        if (cachedProperties == null) {
            cachedProperties = loadAndMergeMetadata();
            LOG.debug("Loaded {} Spring Boot configuration properties from classpath", cachedProperties.size());
        }
        return cachedProperties;
    }

    @Override
    protected String doCallText(Map<String, Object> options) {
        String filter = optionString(options, FILTER);

        StringBuilder sb = new StringBuilder();
        sb.append("Spring Boot Configuration Properties:");
        sb.append("\n");

        List<JsonObject> properties = getProperties();
        if (properties != null) {
            for (JsonObject prop : properties) {
                String name = prop.getString("name");
                if (!accept(name, filter)) {
                    continue;
                }
                String type = prop.getString("type");
                String description = prop.getString("description");
                Object defaultValue = prop.get("defaultValue");

                sb.append(String.format("    %s", name));
                if (type != null) {
                    sb.append(String.format(" (%s)", type));
                }
                if (defaultValue != null) {
                    sb.append(String.format(" = %s", defaultValue));
                }
                sb.append("\n");
                if (description != null) {
                    sb.append(String.format("        %s%n", description));
                }
            }
        }

        return sb.toString();
    }

    @Override
    protected JsonObject doCallJson(Map<String, Object> options) {
        String filter = optionString(options, FILTER);

        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();

        List<JsonObject> properties = getProperties();
        if (properties != null) {
            for (JsonObject prop : properties) {
                String name = prop.getString("name");
                if (!accept(name, filter)) {
                    continue;
                }
                arr.add(prop);
            }
        }

        root.put("properties", arr);
        return root;
    }

    private List<JsonObject> loadAndMergeMetadata() {
        Map<String, JsonObject> merged = new LinkedHashMap<>();

        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = getClass().getClassLoader();
            }
            Enumeration<URL> resources = cl.getResources(METADATA_RESOURCE);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try {
                    parseMetadataResource(url, merged);
                } catch (Exception e) {
                    LOG.debug("Failed to parse Spring Boot configuration metadata from: {}", url, e);
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to scan for Spring Boot configuration metadata resources", e);
        }

        return new ArrayList<>(merged.values());
    }

    @SuppressWarnings("unchecked")
    private void parseMetadataResource(URL url, Map<String, JsonObject> merged) throws Exception {
        String json;
        try (InputStream is = url.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            json = reader.lines().collect(Collectors.joining("\n"));
        }

        Object parsed = Jsoner.deserialize(json);
        if (!(parsed instanceof JsonObject root)) {
            return;
        }

        Object propertiesObj = root.get("properties");
        if (!(propertiesObj instanceof JsonArray propertiesArr)) {
            return;
        }

        for (Object item : propertiesArr) {
            if (!(item instanceof JsonObject property)) {
                continue;
            }
            String name = property.getString("name");
            if (name == null || name.isEmpty()) {
                continue;
            }

            JsonObject entry = new JsonObject();
            entry.put("name", name);
            putIfNotNull(entry, "type", property.getString("type"));
            putIfNotNull(entry, "description", property.getString("description"));
            putIfNotNull(entry, "sourceType", property.getString("sourceType"));
            if (property.containsKey("defaultValue")) {
                entry.put("defaultValue", property.get("defaultValue"));
            }
            if (Boolean.TRUE.equals(property.get("deprecated"))) {
                entry.put("deprecated", true);
                Object deprecation = property.get("deprecation");
                if (deprecation instanceof JsonObject deprecationObj) {
                    JsonObject depInfo = new JsonObject();
                    putIfNotNull(depInfo, "replacement", deprecationObj.getString("replacement"));
                    putIfNotNull(depInfo, "since", deprecationObj.getString("since"));
                    if (!depInfo.isEmpty()) {
                        entry.put("deprecation", depInfo);
                    }
                }
            }

            // merge: last-wins for duplicate property names across JARs
            merged.put(name, entry);
        }
    }

    private static void putIfNotNull(JsonObject obj, String key, String value) {
        if (value != null) {
            obj.put(key, value);
        }
    }

    private static boolean accept(String name, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return PatternHelper.matchPattern(name, filter);
    }
}
