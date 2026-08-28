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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.PropertiesFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Base class for the early-resolution listeners used by the Camel vault and secrets starters.
 * <p/>
 * These listeners run on {@link ApplicationEnvironmentPreparedEvent}, before the application context exists, so
 * they cannot use dependency injection or conditional auto-configuration. This class owns the parts that are the
 * same for every component: reading the guard property, walking the property sources, matching placeholders,
 * resolving them and registering the resolved values. Subclasses contribute only the component-specific client
 * construction and naming.
 * <p/>
 * Early resolution deliberately handles only property values that consist entirely of a single placeholder.
 * Values with an embedded placeholder, such as {@code jdbc://{{aws:host}}/db}, are left for Camel's normal
 * property parser.
 */
public abstract class AbstractEarlyResolutionPropertiesParser
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    /**
     * Lets an operator tolerate resolution failures instead of aborting startup.
     */
    public static final String IGNORE_RESOLUTION_FAILURES = "camel.vault.ignore-resolution-failures";

    private static final String SUFFIX = "}}";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEarlyResolutionPropertiesParser.class);

    /**
     * The property that enables early resolution for this component, for example
     * {@code camel.component.aws-secrets-manager.early-resolve-properties}.
     */
    protected abstract String getEarlyResolutionProperty();

    /**
     * The name of the property source holding the resolved values, for example
     * {@code overridden-camel-aws-secrets-manager-properties}. These names are observable through
     * {@code /actuator/env}, so they must not change.
     */
    protected abstract String getOverridePropertySourceName();

    /**
     * Builds the component client and its resolver. Called only after the guard property is enabled, so that
     * client construction and its configuration validation never run for applications that did not opt in.
     */
    protected abstract PropertiesFunction createPropertiesFunction(ConfigurableEnvironment environment);

    /**
     * Wording used in diagnostics to name where a value was being resolved from.
     */
    protected String getSourceDescription() {
        return "the vault";
    }

    @Override
    public final void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        if (!Boolean.parseBoolean(environment.getProperty(getEarlyResolutionProperty()))) {
            return;
        }

        // an unresolved placeholder would otherwise stay in the property value and become the effective
        // secret, so resolution failures abort startup unless the operator opts back into the old behaviour
        final boolean ignoreResolutionFailures
                = Boolean.parseBoolean(environment.getProperty(IGNORE_RESOLUTION_FAILURES));

        PropertiesFunction propertiesFunction = createPropertiesFunction(environment);
        LOG.debug("Early resolving properties using the {} function", propertiesFunction.getName());

        final String prefix = "{{" + propertiesFunction.getName() + ":";
        final Properties props = new Properties();
        final List<String> failedKeys = new ArrayList<>();
        final List<RuntimeCamelException> failures = new ArrayList<>();

        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof MapPropertySource mapPropertySource) {
                mapPropertySource.getSource().forEach((key, value) -> {
                    String stringValue = asString(value);
                    if (stringValue != null && stringValue.startsWith(prefix) && stringValue.endsWith(SUFFIX)) {
                        String remainder = stringValue.substring(prefix.length(),
                                stringValue.length() - SUFFIX.length());
                        LOG.debug("decrypting and overriding property {}", key);
                        try {
                            props.put(key, propertiesFunction.apply(remainder));
                        } catch (Exception e) {
                            if (ignoreResolutionFailures) {
                                LOG.warn("Failed to resolve property {} from {}; the placeholder is left "
                                         + "unresolved because {} is enabled",
                                        key, getSourceDescription(), IGNORE_RESOLUTION_FAILURES, e);
                            } else {
                                failedKeys.add(key);
                                failures.add(new RuntimeCamelException(
                                        "Failed to resolve property " + key + " from " + getSourceDescription()
                                                              + ".",
                                        e));
                            }
                        }
                    }
                });
            }
        }

        if (!failures.isEmpty()) {
            RuntimeCamelException aggregated = new RuntimeCamelException(
                    "Failed to resolve " + failures.size() + " property placeholder(s) from "
                                          + getSourceDescription() + ": " + String.join(", ", failedKeys)
                                          + ". Startup is aborted so that the unresolved placeholders cannot "
                                          + "become the effective values; set " + IGNORE_RESOLUTION_FAILURES
                                          + "=true to continue anyway.");
            failures.forEach(aggregated::addSuppressed);
            throw aggregated;
        }

        environment.getPropertySources()
                .addFirst(new PropertiesPropertySource(getOverridePropertySourceName(), props));
    }

    private static String asString(Object value) {
        if (value instanceof OriginTrackedValue originTrackedValue
                && originTrackedValue.getValue() instanceof String v) {
            return v;
        }
        if (value instanceof String v) {
            return v;
        }
        return null;
    }
}
