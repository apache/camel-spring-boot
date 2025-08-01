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
package org.apache.camel.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties component is used for using property placeholders in endpoint uris.
 */
@ConfigurationProperties(prefix = "camel.component.properties")
public class PropertiesComponentConfiguration {

    /**
     * A list of locations to load properties. You can use comma to separate multiple locations. This option will
     * override any default locations and only use the locations from this option.
     */
    private String location;
    /**
     * Encoding to use when loading properties file from the file system or classpath. If no encoding has been set, then
     * the properties files is loaded using ISO-8859-1 encoding (latin-1) as documented by
     * java.util.Properties#load(java.io.InputStream)
     */
    private String encoding;
    /**
     * To use a custom PropertiesParser. The option is a org.apache.camel.component.properties.PropertiesParser type.
     */
    private String propertiesParser;
    /**
     * If false, the component does not attempt to find a default for the key by looking after the colon separator.
     */
    private Boolean defaultFallbackEnabled = true;
    /**
     * Whether to silently ignore if a location cannot be located, such as a properties file not found.
     */
    private Boolean ignoreMissingLocation = false;
    /**
     * Whether to support nested property placeholders. A nested placeholder, means that a placeholder, has also a
     * placeholder, that should be resolved (recursively).
     */
    private Boolean nestedPlaceholder = false;
    /**
     * Sets initial properties which will be used before any locations are resolved. The option is a
     * java.util.Properties type.
     */
    private String initialProperties;
    /**
     * Sets a special list of override properties that take precedence and will use first, if a property exist. The
     * option is a java.util.Properties type.
     */
    private String overrideProperties;
    /**
     * Sets the JVM system property mode (0 = never, 1 = fallback, 2 = override). The default mode (override) is to use
     * system properties if present, and override any existing properties. OS environment variable mode is checked
     * before JVM system property mode
     */
    private Integer systemPropertiesMode = 2;
    /**
     * Sets the OS environment variables mode (0 = never, 1 = fallback, 2 = override). The default mode (override) is to
     * use OS environment variables if present, and override any existing properties. OS environment variable mode is
     * checked before JVM system property mode
     */
    private Integer environmentVariableMode = 2;
    /**
     * Whether to automatically discovery instances of PropertiesSource from registry and service factory.
     */
    private Boolean autoDiscoverPropertiesSources = true;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getPropertiesParser() {
        return propertiesParser;
    }

    public void setPropertiesParser(String propertiesParser) {
        this.propertiesParser = propertiesParser;
    }

    public Boolean getDefaultFallbackEnabled() {
        return defaultFallbackEnabled;
    }

    public void setDefaultFallbackEnabled(Boolean defaultFallbackEnabled) {
        this.defaultFallbackEnabled = defaultFallbackEnabled;
    }

    public Boolean getIgnoreMissingLocation() {
        return ignoreMissingLocation;
    }

    public void setIgnoreMissingLocation(Boolean ignoreMissingLocation) {
        this.ignoreMissingLocation = ignoreMissingLocation;
    }

    public Boolean getNestedPlaceholder() {
        return nestedPlaceholder;
    }

    public void setNestedPlaceholder(Boolean nestedPlaceholder) {
        this.nestedPlaceholder = nestedPlaceholder;
    }

    public String getInitialProperties() {
        return initialProperties;
    }

    public void setInitialProperties(String initialProperties) {
        this.initialProperties = initialProperties;
    }

    public String getOverrideProperties() {
        return overrideProperties;
    }

    public void setOverrideProperties(String overrideProperties) {
        this.overrideProperties = overrideProperties;
    }

    public Integer getSystemPropertiesMode() {
        return systemPropertiesMode;
    }

    public void setSystemPropertiesMode(Integer systemPropertiesMode) {
        this.systemPropertiesMode = systemPropertiesMode;
    }

    public Integer getEnvironmentVariableMode() {
        return environmentVariableMode;
    }

    public void setEnvironmentVariableMode(Integer environmentVariableMode) {
        this.environmentVariableMode = environmentVariableMode;
    }

    public Boolean getAutoDiscoverPropertiesSources() {
        return autoDiscoverPropertiesSources;
    }

    public void setAutoDiscoverPropertiesSources(Boolean autoDiscoverPropertiesSources) {
        this.autoDiscoverPropertiesSources = autoDiscoverPropertiesSources;
    }

}