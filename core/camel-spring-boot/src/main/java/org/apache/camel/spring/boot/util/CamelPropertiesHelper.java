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
package org.apache.camel.spring.boot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.PropertyBindingException;
import org.apache.camel.spi.BeanIntrospection;
import org.apache.camel.spi.PropertyConfigurer;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.BoundConfigurationProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.ApplicationContext;

/**
 * To help configuring Camel properties that have been defined in Spring Boot configuration files.
 */
public final class CamelPropertiesHelper {

    /**
     * Property to turn off failing fast when an explicitly configured Spring Boot option cannot be set on the Camel
     * component, data format or language it belongs to. When lenient, such an option is logged at WARN level and
     * ignored, which is close to the behaviour before Camel 4.23, where it was dropped without any log line.
     */
    public static final String LENIENT_CONFIGURATION_BINDING = "camel.springboot.lenient-configuration-binding";

    private static final Logger LOG = LoggerFactory.getLogger(CamelPropertiesHelper.class);

    /**
     * Options that belong to the Spring Boot auto configuration layer itself, and therefore are not options on the
     * Camel component, data format or language being configured.
     */
    private static final Set<String> AUTO_CONFIGURATION_OPTIONS = Set.of("enabled", "customizer");

    /**
     * Cache of class → default-instance properties. Avoids constructing a new default instance on every customizer
     * call. The sentinel value {@link #NO_DEFAULTS} means the class has no accessible no-arg constructor and
     * programmatic-assignment detection is unavailable for it.
     */
    private static final ConcurrentHashMap<Class<?>, Map<String, Object>> DEFAULTS_CACHE = new ConcurrentHashMap<>();

    /** Sentinel placed in {@link #DEFAULTS_CACHE} when instantiation failed. */
    private static final Map<String, Object> NO_DEFAULTS = Map.of();

    private CamelPropertiesHelper() {
    }

    /**
     * Copies the options from a generated Spring Boot configuration class onto the Camel component, data format or
     * language it configures.
     * <p/>
     * An option is copied when it was set by the application, either via Spring Boot property binding (application
     * properties / environment) as recorded in {@link BoundConfigurationProperties}, or programmatically in Java
     * directly onto the generated configuration class (detected by comparing the current value against the value a
     * freshly-constructed default instance of that class would carry). Options that only carry the catalog default the
     * generator wrote as a field initializer are <em>not</em> copied, so they cannot overwrite a value the target
     * already holds — for example one set programmatically on a user-supplied bean. The options that belong to the
     * auto configuration layer itself (<tt>enabled</tt> and <tt>customizer</tt>) are never copied, as they are not
     * options on the target bean.
     * <p/>
     * <strong>Known gap:</strong> a programmatic assignment whose value happens to equal the class field initializer
     * (e.g. {@code cfg.setSkipRequestHeaders(false)} when the initializer is already {@code false}) is indistinguishable
     * from a catalog default and will not be copied. This matters only when the target holds a different value and the
     * intent was to reset it.
     * <p/>
     * A configured option that cannot be set on the target fails fast with an {@link IllegalArgumentException},
     * instead of being dropped without a trace. Set {@link #LENIENT_CONFIGURATION_BINDING} to <tt>true</tt> to log it
     * at WARN and continue, instead of failing.
     *
     * @param camelContext
     *                           the CamelContext
     * @param applicationContext
     *                           the Spring application context, used to tell a configured option from a catalog
     *                           default
     * @param propertyPrefix
     *                           the configuration prefix of the source, such as <tt>camel.component.http</tt>
     * @param source
     *                           the Spring Boot configuration class
     * @param target
     *                           the Camel component, data format or language to configure
     */
    public static void copyConfigurationProperties(CamelContext camelContext, ApplicationContext applicationContext,
            String propertyPrefix, Object source, Object target) {
        ObjectHelper.notNull(camelContext, "camel context");
        ObjectHelper.notNull(source, "source");
        ObjectHelper.notNull(target, "target");

        Map<String, Object> properties = getNonNullProperties(camelContext, source);
        properties.keySet().removeIf(key -> AUTO_CONFIGURATION_OPTIONS.contains(key.toLowerCase(Locale.US)));
        // only the options the application configured are copied, the others merely carry their catalog default
        Set<ConfigurationPropertyName> bound = boundProperties(applicationContext);
        if (bound != null && propertyPrefix != null && !propertyPrefix.isEmpty()) {
            // Also keep properties that were assigned programmatically in Java (not via Spring Boot
            // property binding), detected by comparing against a fresh default instance of the
            // configuration class (cached per class to avoid repeated instantiation).
            // When the class has no accessible no-arg constructor the detection is skipped and only
            // bound options are copied (safe fallback: same behaviour as before this fix).
            Map<String, Object> defaults = defaultProperties(camelContext, source);
            if (defaults != null) {
                properties.keySet().removeIf(key -> {
                    String optionKey = optionKey(propertyPrefix, key);
                    if (isConfigured(bound, optionKey)) {
                        return false; // keep — explicitly configured via Spring Boot
                    }
                    if (isDifferentFromDefault(key, properties, defaults)) {
                        LOG.debug("Copying programmatically-assigned option [{}] onto [{}]", optionKey,
                                ObjectHelper.classCanonicalName(target));
                        return false; // keep — assigned in Java, differs from class default
                    }
                    LOG.debug("Skipping option [{}] — not configured and equals the class default", optionKey);
                    return true; // drop — only carries the catalog default
                });
            } else {
                properties.keySet().removeIf(key -> !isConfigured(bound, optionKey(propertyPrefix, key)));
            }
        }

        // the options that could be set are removed from the map, so what is left could not be set
        doSetCamelProperties(camelContext, target, properties, false, false);
        if (properties.isEmpty()) {
            return;
        }

        boolean lenient = isLenientBinding(applicationContext);
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String option = optionKey(propertyPrefix, entry.getKey());
            if (lenient) {
                LOG.warn("Cannot configure option [{}] with value [{}] on [{}]. This option is ignored.", option,
                        entry.getValue(), ObjectHelper.classCanonicalName(target));
            } else {
                failed.add(option + " = " + entry.getValue());
            }
        }
        if (!failed.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot configure " + failed + " on [" + ObjectHelper.classCanonicalName(target)
                                               + "], which has no suitable setter method for it, and no bean with that id exists in the Spring"
                                               + " Boot registry. The option may be listed in the documentation of the starter, as that is"
                                               + " generated from the Camel catalog rather than from this class, in which case it has never"
                                               + " taken effect. Correct or remove the option, or set "
                                               + LENIENT_CONFIGURATION_BINDING + "=true to keep ignoring it.");
        }
    }

    private static String optionKey(String propertyPrefix, String name) {
        String dashed = StringHelper.camelCaseToDash(name).toLowerCase(Locale.US);
        return propertyPrefix != null && !propertyPrefix.isEmpty() ? propertyPrefix + "." + dashed : dashed;
    }

    /**
     * The properties Spring Boot bound onto the configuration classes of the application, or <tt>null</tt> when that
     * cannot be determined, in which case every option is copied as before Camel 4.23.
     */
    private static Set<ConfigurationPropertyName> boundProperties(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return null;
        }
        try {
            BoundConfigurationProperties bound = BoundConfigurationProperties.get(applicationContext);
            if (bound == null) {
                return null;
            }
            // Copy inside the try so a failure in getAll() is also caught
            return Set.copyOf(bound.getAll().keySet());
        } catch (Exception e) {
            LOG.warn("Cannot determine which options were configured due to: {}. Copying all options.",
                    e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gets the default properties of a freshly-constructed instance of the same class as the given source, cached per
     * class. Returns {@code null} when the class has no accessible no-arg constructor, meaning programmatic-assignment
     * detection is unavailable; the caller falls back to copying only bound options (same as before this fix).
     */
    private static Map<String, Object> defaultProperties(CamelContext camelContext, Object source) {
        Map<String, Object> cached = DEFAULTS_CACHE.get(source.getClass());
        if (cached != null) {
            return cached == NO_DEFAULTS ? null : cached;
        }
        try {
            Object defaults = source.getClass().getDeclaredConstructor().newInstance();
            Map<String, Object> result = getNonNullProperties(camelContext, defaults);
            DEFAULTS_CACHE.put(source.getClass(), result);
            return result;
        } catch (Exception e) {
            LOG.debug("Cannot instantiate default of {} to detect programmatic assignments: {}",
                    ObjectHelper.classCanonicalName(source), e.getMessage());
            DEFAULTS_CACHE.put(source.getClass(), NO_DEFAULTS);
            return null;
        }
    }

    /**
     * Whether the value of the given key in {@code properties} differs from the value in {@code defaults}. When the
     * current value is not equal to the default, it was assigned programmatically in Java and must be copied to the
     * target even if it was not bound by Spring Boot.
     */
    private static boolean isDifferentFromDefault(String key, Map<String, Object> properties,
            Map<String, Object> defaults) {
        return !Objects.equals(properties.get(key), defaults.get(key));
    }

    /**
     * Whether the application configured the given option, as opposed to the option only carrying the default value
     * the generator took from the Camel catalog. Spring Boot records every property it bound, including the entries
     * of a map or list option, so the option is configured when that property or any property below it was bound.
     */
    private static boolean isConfigured(Set<ConfigurationPropertyName> bound, String optionKey) {
        ConfigurationPropertyName option = ConfigurationPropertyName.ofIfValid(optionKey);
        if (option == null) {
            // not a name Spring Boot could have bound, so it cannot be told apart from a default
            return true;
        }
        for (ConfigurationPropertyName name : bound) {
            if (option.equals(name) || option.isAncestorOf(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLenientBinding(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return false;
        }
        try {
            return applicationContext.getEnvironment().getProperty(LENIENT_CONFIGURATION_BINDING, Boolean.class,
                    Boolean.FALSE);
        } catch (Exception e) {
            LOG.warn("Cannot resolve {} due to: {}. Using strict configuration binding.",
                    LENIENT_CONFIGURATION_BINDING, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Copies all non-null options from the source onto the target, ignoring any option that cannot be set.
     *
     * @see #copyConfigurationProperties(CamelContext, ApplicationContext, String, Object, Object) for copying a
     *      generated Spring Boot configuration class onto the Camel bean it configures
     */
    @SuppressWarnings({ "unchecked" })
    public static void copyProperties(CamelContext camelContext, Object source, Object target) {
        ObjectHelper.notNull(camelContext, "camel context");
        ObjectHelper.notNull(source, "source");
        ObjectHelper.notNull(target, "target");

        CamelPropertiesHelper.setCamelProperties(camelContext, target,
                source instanceof Map ? (Map) source : getNonNullProperties(camelContext, source), false);
    }

    /**
     * Sets the properties on the target bean.
     * <p/>
     * This method uses {@link PropertyBindingSupport} and therefore offers its capabilities such as:
     * <ul>
     * <li>property placeholders - Keys and values using Camels property placeholder will be resolved</li>
     * <li>nested - Properties can be nested using the dot syntax (OGNL and builder pattern using with as prefix), eg
     * foo.bar=123</li>
     * <li>map</li> - Properties can lookup in Map's using map syntax, eg foo[bar] where foo is the name of the property
     * that is a Map instance, and bar is the name of the key.</li>
     * <li>list</li> - Properties can refer or add to in List's using list syntax, eg foo[0] where foo is the name of
     * the property that is a List instance, and 0 is the index. To refer to the last element, then use last as
     * key.</li>
     * </ul>
     * This implementation sets the properties using the following algorithm in the given order:
     * <ul>
     * <li>reference by bean id - Values can refer to other beans in the registry by prefixing with with # or #bean: eg
     * #myBean or #bean:myBean</li>
     * <li>reference by type - Values can refer to singleton beans by their type in the registry by prefixing with
     * #type: syntax, eg #type:com.foo.MyClassType</li>
     * <li>autowire by type - Values can refer to singleton beans by auto wiring by setting the value to #autowired</li>
     * <li>reference new class - Values can refer to creating new beans by their class name by prefixing with #class, eg
     * #class:com.foo.MyClassType</li>
     * <li>value as lookup - The value is used as-is (eg like #value) to lookup in the Registry if there is a bean then
     * its set on the target</li>
     * </ul>
     * When an option has been set on the target bean, then its removed from the given properties map. If all the
     * options has been set, then the map will be empty. The implementation ignores case for the property keys.
     *
     * @param context
     *            the CamelContext
     * @param target
     *            the target bean
     * @param properties
     *            the properties
     * @param failIfNotSet
     *            whether to fail if an option either does not exists on the target bean or if the option cannot be due
     *            no suitable setter methods with the given type
     *
     * @return <tt>true</tt> if at least one option was configured
     *
     * @throws IllegalArgumentException
     *             is thrown if an option cannot be configured on the bean because there is no suitable setter method
     *             and failOnNoSet is true.
     */
    public static boolean setCamelProperties(CamelContext context, Object target, Map<String, Object> properties,
            boolean failIfNotSet) {
        return doSetCamelProperties(context, target, properties, failIfNotSet, true);
    }

    private static boolean doSetCamelProperties(CamelContext context, Object target, Map<String, Object> properties,
            boolean failIfNotSet, boolean warnIfNotSet) {
        ObjectHelper.notNull(context, "context");
        ObjectHelper.notNull(target, "target");
        ObjectHelper.notNull(properties, "properties");
        boolean rc = false;

        PropertyConfigurer configurer = null;
        if (target instanceof Component) {
            // the component needs to be initialized to have the configurer ready
            ServiceHelper.initService(target);
            configurer = ((Component) target).getComponentPropertyConfigurer();
        }

        Iterator<Map.Entry<String, Object>> it = properties.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            String name = entry.getKey();
            Object value = entry.getValue();
            String stringValue = value != null ? value.toString() : null;
            boolean hit = false;
            try {
                hit = PropertyBindingSupport.build().withConfigurer(configurer).withIgnoreCase(true).bind(context,
                        target, name, value);
            } catch (PropertyBindingException e) {
                // no we could not and this would be thrown if we attempted to set a value on a property which we cannot
                // do type conversion as
                // then maybe the value refers to a spring bean in the registry so try this
                if (stringValue != null) {
                    if (stringValue.startsWith("#")) {
                        stringValue = stringValue.substring(1);
                    }
                    // use #bean: to lookup
                    stringValue = "#bean:" + stringValue;
                    hit = PropertyBindingSupport.build().withIgnoreCase(true).bind(context, target, name, stringValue);
                }
            }

            if (hit) {
                // must remove as its a valid option and we could configure it
                it.remove();
                rc = true;
            } else if (failIfNotSet) {
                throw new IllegalArgumentException("Cannot configure option [" + name + "] with value [" + stringValue
                        + "] as the bean class [" + ObjectHelper.classCanonicalName(target)
                        + "] has no suitable setter method, or not possible to lookup a bean with the id ["
                        + stringValue + "] in Spring Boot registry");
            } else if (warnIfNotSet) {
                LOG.warn(
                        "Cannot configure option [{}] with value [{}] as the bean class [{}] has no suitable setter method,"
                        + " or not possible to lookup a bean with the id [{}] in Spring Boot registry."
                        + " This option is ignored.",
                        name, stringValue, ObjectHelper.classCanonicalName(target), stringValue);
            }
        }

        return rc;
    }

    /**
     * Gets all the non-null properties from the given object,
     *
     * @param camelContext
     *            the camel context
     * @param target
     *            the object
     *
     * @return the properties (non-null only)
     */
    public static Map<String, Object> getNonNullProperties(CamelContext camelContext, Object target) {
        Map<String, Object> properties = new HashMap<>();

        BeanIntrospection bi = PluginHelper.getBeanIntrospection(camelContext);
        bi.getProperties(target, properties, null, false);

        return properties;
    }

}
