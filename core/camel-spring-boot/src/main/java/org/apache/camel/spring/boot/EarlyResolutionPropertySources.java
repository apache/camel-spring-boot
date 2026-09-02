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

import java.util.Properties;

import org.slf4j.Logger;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Helpers for early property resolution listeners that inject a flat {@link Properties} override via
 * {@code PropertySources.addFirst}.
 * <p/>
 * Spring iterates property sources in precedence order (highest first). When the same key appears in multiple
 * sources, callers must retain the first resolved value so the override map reflects Spring's normal precedence
 * after it is added with {@code addFirst}.
 */
public final class EarlyResolutionPropertySources {

    private EarlyResolutionPropertySources() {
    }

    /**
     * Returns the string value from a property source entry, including {@link OriginTrackedValue} wrappers.
     */
    public static String asString(Object value) {
        if (value instanceof OriginTrackedValue originTrackedValue
                && originTrackedValue.getValue() instanceof String stringValue) {
            return stringValue;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return null;
    }

    /**
     * Stores a resolved value only when the key is not already present, preserving highest-precedence values
     * collected while iterating property sources.
     */
    public static void putIfAbsent(Properties props, Object key, String resolvedValue) {
        props.putIfAbsent(key.toString(), resolvedValue);
    }

    /**
     * Returns whether a property source with higher precedence defines the same key.
     * <p/>
     * Only {@link MapPropertySource} instances are inspected, matching the scanning loop used by the early-resolution
     * parsers. Higher-precedence non-map sources (for example command-line arguments via
     * {@code SimpleCommandLinePropertySource}) are not detected by this helper.
     */
    public static boolean hasHigherPrecedenceProperty(
            Iterable<PropertySource<?>> propertySources, PropertySource<?> currentPropertySource, Object key) {
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource == currentPropertySource) {
                return false;
            }
            if (propertySource instanceof MapPropertySource && propertySource.containsProperty(key.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs and returns {@code true} when early resolution should be skipped because a higher-precedence source
     * already defines the key.
     */
    public static boolean shouldSkipBecauseHigherPrecedenceDefines(
            Iterable<PropertySource<?>> propertySources, PropertySource<?> currentPropertySource, Object key,
            Logger log) {
        if (hasHigherPrecedenceProperty(propertySources, currentPropertySource, key)) {
            log.debug(
                    "Skipping early resolution for property {} from property source {} because a "
                            + "higher-precedence property source already defines it",
                    key, currentPropertySource.getName());
            return true;
        }
        return false;
    }
}
