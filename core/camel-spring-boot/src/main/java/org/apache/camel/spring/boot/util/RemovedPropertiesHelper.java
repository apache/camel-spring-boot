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

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.DataObjectPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * Reports the configuration properties under a prefix that is no longer read, or that the configuration class bound to
 * their prefix has no option for.
 * <p/>
 * Spring Boot ignores a property below the prefix of a <tt>@ConfigurationProperties</tt> class when the class has no
 * field for it. A property whose prefix or option was removed or renamed in an earlier release therefore has no
 * effect, and nothing is logged about it.
 */
public final class RemovedPropertiesHelper {

    private static final Logger LOG = LoggerFactory.getLogger(RemovedPropertiesHelper.class);

    private static final UnboundElementsSourceFilter UNKNOWN_PROPERTIES_SOURCES = new UnboundElementsSourceFilter();

    private RemovedPropertiesHelper() {
    }

    /**
     * Logs a WARN for each property below the given prefix, or named as the prefix, that a property source of the
     * environment holds, naming the property to use instead. A property set in several property sources is logged
     * once.
     *
     * @param environment
     *                    the environment of the application
     * @param prefix
     *                    the prefix that is no longer read, such as <tt>camel.springboot</tt>, or the name of a single
     *                    property that is no longer read, such as <tt>camel.health.components-enabled</tt>
     * @param reason
     *                    why the properties below the prefix have no effect, such as <tt>the camel.springboot. prefix
     *                    was removed in Camel 4.13</tt>
     * @param stillRead
     *                    the properties below the prefix that are still read, which are not logged, by the part of
     *                    their name below the prefix in uniform form (lower case, without dashes and dots), such as
     *                    <tt>lenientconfigurationbinding</tt>
     * @param replacement
     *                    gives the property to use instead from the part of the name below the prefix in uniform form,
     *                    such as <tt>mainruncontroller</tt>, or <tt>null</tt> when there is none
     */
    public static void warnRemovedProperties(
            Environment environment, String prefix, String reason, Set<String> stillRead,
            Function<String, String> replacement) {
        warnProperties(environment, prefix, (source, relative) -> stillRead.contains(uniformName(relative))
                ? null : because(reason, replacement.apply(uniformName(relative))));
    }

    /**
     * Logs a WARN for each property below a prefix that was renamed, naming the option of the same name below the new
     * prefix. A property set in several property sources is logged once.
     *
     * @param environment
     *                    the environment of the application
     * @param prefix
     *                    the prefix that is no longer read, such as <tt>camel.component.file.cluster.service</tt>
     * @param newPrefix
     *                    the prefix the options were renamed to, such as <tt>camel.cluster.file</tt>
     * @param since
     *                    the Camel version that renamed the prefix, such as <tt>3.10</tt>
     * @param type
     *                    the configuration class bound to the new prefix, whose properties are the options to use
     *                    instead, or <tt>null</tt> when the options kept their name
     */
    public static void warnRenamedProperties(
            Environment environment, String prefix, String newPrefix, String since, Class<?> type) {
        Map<String, String> options = type != null ? dashedPropertyNames(type) : null;
        String reason = "the " + prefix + ". prefix was renamed to " + newPrefix + ". in Camel " + since;
        warnProperties(environment, prefix, (source, relative) -> {
            if (relative.isEmpty()) {
                return because(reason, newPrefix);
            }
            String option = options != null
                    ? options.get(relative.getElement(0, Form.UNIFORM))
                    : DataObjectPropertyName.toDashedForm(relative.getElement(0, Form.ORIGINAL));
            if (option == null) {
                return because(reason, null);
            }
            String answer = newPrefix + "." + option;
            if (relative.getNumberOfElements() > 1) {
                // the keys of a map or the elements of a list, kept as they are
                String rest = relative.subName(1).toString();
                answer += rest.startsWith("[") ? rest : "." + rest;
            }
            return because(reason, answer);
        });
    }

    /**
     * Logs a WARN for each property below the given prefix that no option of the configuration class bound to the
     * prefix binds, such as an option that Camel moved to another prefix, an option of Camel Main standalone, or a
     * typo. Spring Boot ignores such a property. A property set in several property sources is logged once.
     * <p/>
     * An option that was moved is logged from every property source, with the option to use instead. Any other
     * property is logged only from the property sources that Spring Boot checks for a configuration class that does
     * not ignore unknown fields, and also not from the environment variables under another name: the name of an
     * environment variable cannot tell a dash from a dot, such as <tt>CAMEL_MAIN_SHUTDOWN_TIMEOUT</tt> for
     * <tt>camel.main.shutdown-timeout</tt>.
     *
     * @param environment
     *                      the environment of the application
     * @param prefix
     *                      the prefix bound to the class, such as <tt>camel.main</tt>
     * @param type
     *                      the configuration class bound to the prefix
     * @param readElsewhere
     *                      the properties below the prefix that are read outside the class, by the part of their name
     *                      below the prefix in uniform form (lower case, without dashes and dots), such as
     *                      <tt>typeconversion</tt>
     * @param moved
     *                      gives the property to use instead of an option that was moved, from the part of its name
     *                      below the prefix in uniform form, such as <tt>camel.trace.enabled</tt> for
     *                      <tt>backlogtracing</tt>, or <tt>null</tt> when it is not such an option
     */
    public static void warnUnboundProperties(
            Environment environment, String prefix, Class<?> type, Set<String> readElsewhere,
            Function<String, String> moved) {
        Set<String> options = new HashSet<>(readElsewhere);
        for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(type)) {
            if (!"class".equals(pd.getName())) {
                options.add(pd.getName().toLowerCase(Locale.ROOT));
            }
        }
        String reason = "Camel on Spring Boot has no " + prefix + " option of this name";
        warnProperties(environment, prefix, (source, relative) -> {
            if (relative.isEmpty()) {
                return null;
            }
            String name = uniformName(relative);
            // a map option, such as camel.main.global-options[key], is bound by its first element
            if (options.contains(relative.getElement(0, Form.UNIFORM)) || options.contains(name)) {
                return null;
            }
            String replacement = moved.apply(name);
            if (replacement != null) {
                return reason + ". Use " + replacement + " instead.";
            }
            return checksUnknownProperties(source) ? reason + "." : null;
        });
    }

    /**
     * The writable properties of the given class, by their name in uniform form (lower case, without dashes), with
     * their name in the dashed form of the Spring Boot configuration keys, such as <tt>run-controller</tt>.
     */
    public static Map<String, String> dashedPropertyNames(Class<?> type) {
        Map<String, String> answer = new HashMap<>();
        for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(type)) {
            if (pd.getWriteMethod() != null) {
                answer.put(pd.getName().toLowerCase(Locale.ROOT), DataObjectPropertyName.toDashedForm(pd.getName()));
            }
        }
        return answer;
    }

    /**
     * Whether unknown properties are reported from the given property source: the property sources that Spring Boot
     * checks for a configuration class that does not ignore unknown fields, except the environment variables under
     * another name.
     */
    private static boolean checksUnknownProperties(ConfigurationPropertySource source) {
        return !(source.getUnderlyingSource() instanceof SystemEnvironmentPropertySource)
                && UNKNOWN_PROPERTIES_SOURCES.apply(source);
    }

    /**
     * Why a property has no effect, with the property to use instead.
     */
    private static String because(String reason, String replacement) {
        if (replacement != null) {
            return reason + ". Use " + replacement + " instead.";
        }
        return reason + ", and there is no option of the same name to use instead.";
    }

    /**
     * Logs a WARN for each property below the given prefix, or named as the prefix, that a property source of the
     * environment holds.
     *
     * @param message
     *                gives, from the property source and the part of the name below the prefix, why the property has
     *                no effect, such as <tt>the camel.springboot. prefix was removed in Camel 4.13. Use camel.main.name
     *                instead.</tt>, or <tt>null</tt> when the property is read
     */
    private static void warnProperties(
            Environment environment, String prefix,
            BiFunction<ConfigurationPropertySource, ConfigurationPropertyName, String> message) {
        if (!(environment instanceof ConfigurableEnvironment)) {
            return;
        }
        ConfigurationPropertyName prefixName = ConfigurationPropertyName.of(prefix);
        Set<ConfigurationPropertyName> reported = new HashSet<>();
        for (ConfigurationPropertySource source : ConfigurationPropertySources.get(environment)) {
            if (!(source instanceof IterableConfigurationPropertySource iterable)) {
                continue;
            }
            List<ConfigurationPropertyName> names = iterable.stream()
                    .filter(name -> prefixName.equals(name) || prefixName.isAncestorOf(name)).toList();
            for (ConfigurationPropertyName name : names) {
                ConfigurationPropertyName relative = name.subName(prefixName.getNumberOfElements());
                String why = message.apply(source, relative);
                if (why == null || !reported.add(name)) {
                    continue;
                }
                ConfigurationProperty property = source.getConfigurationProperty(name);
                Origin origin = property != null ? property.getOrigin() : null;
                String key = name.toString();
                String location = origin != null ? origin.toString() : "unknown location";
                if (origin instanceof PropertySourceOrigin pso) {
                    key = pso.getPropertyName();
                    location = pso.getOrigin() != null
                            ? pso.getOrigin().toString() : "property source " + pso.getPropertySource().getName();
                }
                LOG.warn("Property {} ({}) has no effect, as {}", key, location, why);
            }
        }
    }

    private static String uniformName(ConfigurationPropertyName name) {
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < name.getNumberOfElements(); i++) {
            answer.append(name.getElement(i, Form.UNIFORM));
        }
        return answer.toString();
    }
}
