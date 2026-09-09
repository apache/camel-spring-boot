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
import java.util.Set;
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
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
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

    private CamelPropertiesHelper() {
    }

    /**
     * Copies the options from a generated Spring Boot configuration class onto the Camel component, data format or
     * language it configures.
     * <p/>
     * The options that belong to the auto configuration layer itself (<tt>enabled</tt> and <tt>customizer</tt>) are
     * removed first, as they are not options on the target bean.
     * <p/>
     * An option that cannot be set on the target and that the application configured explicitly fails fast with an
     * {@link IllegalArgumentException}, instead of being dropped without a trace. An option that cannot be set and
     * that only carries its catalog default is logged at DEBUG, as there is nothing the application can do about it
     * and the target keeps its own default. Set {@link #LENIENT_CONFIGURATION_BINDING} to <tt>true</tt> to log an
     * explicitly configured option at WARN and continue, instead of failing.
     *
     * @param camelContext
     *                           the CamelContext
     * @param applicationContext
     *                           the Spring application context, used to tell an explicitly configured option from a
     *                           catalog default
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

        // the options that could be set are removed from the map, so what is left could not be set
        doSetCamelProperties(camelContext, target, properties, false, false);
        if (properties.isEmpty()) {
            return;
        }

        boolean lenient = isLenientBinding(applicationContext);
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (isExplicitlyConfigured(applicationContext, propertyPrefix, name)) {
                if (lenient) {
                    LOG.warn("Cannot configure option [{}] with value [{}] on [{}]. This option is ignored.",
                            optionKey(propertyPrefix, name), value, ObjectHelper.classCanonicalName(target));
                } else {
                    failed.add(optionKey(propertyPrefix, name) + " = " + value);
                }
            } else {
                // only the catalog default was carried, so the target keeps its own default
                LOG.debug("Cannot configure option [{}] with default value [{}] on [{}]. This option is ignored.",
                        optionKey(propertyPrefix, name), value, ObjectHelper.classCanonicalName(target));
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
        String dashed = StringHelper.camelCaseToDash(name);
        return propertyPrefix != null && !propertyPrefix.isEmpty() ? propertyPrefix + "." + dashed : dashed;
    }

    /**
     * Whether the application configured the given option itself, as opposed to the option only carrying the default
     * value the generator took from the Camel catalog.
     */
    private static boolean isExplicitlyConfigured(ApplicationContext applicationContext, String propertyPrefix,
            String name) {
        if (applicationContext == null || propertyPrefix == null || propertyPrefix.isEmpty()) {
            return false;
        }
        try {
            ConfigurationPropertyName key
                    = ConfigurationPropertyName.of(optionKey(propertyPrefix, name).toLowerCase(Locale.US));
            for (ConfigurationPropertySource source : ConfigurationPropertySources
                    .get(applicationContext.getEnvironment())) {
                if (source.getConfigurationProperty(key) != null) {
                    return true;
                }
            }
        } catch (Exception e) {
            // returning false downgrades a hard error to an ignored option, so this must not stay quiet
            LOG.warn("Cannot determine whether {} was configured due to: {}. Treating it as not configured.",
                    optionKey(propertyPrefix, name), e.getMessage(), e);
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
