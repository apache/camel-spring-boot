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

import java.lang.reflect.Field;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ClassUtils;

/**
 * Resolves the bean references that Camel configuration options of a complex (object) type are configured with in
 * <tt>application.properties</tt>, such as
 * <tt>camel.component.netty-http.ssl-context-parameters = #bean:mySslContextParameters</tt>.
 * <p/>
 * This is used by the generated <tt>*ComponentConverter</tt>, <tt>*DataFormatConverter</tt> and
 * <tt>*LanguageConverter</tt> classes in the Camel Spring Boot starters. The following syntaxes are supported:
 * <ul>
 * <li><tt>#bean:myBean</tt> - lookup the bean by its id</li>
 * <li><tt>#myBean</tt> - lookup the bean by its id</li>
 * <li><tt>myBean</tt> - lookup the bean by its id</li>
 * <li><tt>#autowired</tt> - lookup the single bean of the option type</li>
 * <li><tt>#type:com.foo.MyType</tt> - lookup the single bean of the given type</li>
 * </ul>
 * A configured value that cannot be resolved is reported by throwing an {@link IllegalArgumentException}, so a typo in
 * a configuration file is not silently turned into a <tt>null</tt> value.
 * <p/>
 * The generated converters are registered with {@code @ConfigurationPropertiesBinding} and therefore take part in
 * every {@code @ConfigurationProperties} binding in the application, not only in Camel's own. A binding whose target
 * is not a Camel configuration class keeps the behaviour it had before Camel 4.23 - a value that is not a bean
 * reference converts to <tt>null</tt> - so that adding a starter to the classpath cannot make an unrelated
 * application property fail to bind. See {@link #isCamelConfigurationTarget(TypeDescriptor)}.
 */
public final class BeanReferenceHelper {

    private static final String BEAN_PREFIX = "#bean:";
    private static final String TYPE_PREFIX = "#type:";
    private static final String CLASS_PREFIX = "#class:";
    private static final String AUTOWIRED = "#autowired";

    private static final String CAMEL_PACKAGE = "org.apache.camel";
    private static final String CAMEL_PROPERTY_PREFIX = "camel.";

    private BeanReferenceHelper() {
    }

    /**
     * Resolves the given configured value as a bean from the Spring application context.
     *
     * @param  applicationContext
     *                                  the Spring application context
     * @param  source
     *                                  the configured value, such as <tt>#bean:myBean</tt>
     * @param  targetType
     *                                  the type the option expects
     * @param  propertyPrefix
     *                                  the configuration prefix the option belongs to, such as
     *                                  <tt>camel.component.netty-http</tt>, used to make the error message actionable
     *
     * @return                          the resolved bean, or <tt>null</tt> if no value was configured
     *
     * @throws IllegalArgumentException
     *                                  if a Camel option was configured with a value that cannot be resolved to a bean
     *                                  of the target type
     */
    public static Object resolveBeanReference(ApplicationContext applicationContext, Object source,
            TypeDescriptor targetType, String propertyPrefix) {
        if (source == null) {
            return null;
        }
        String value = source.toString().trim();
        if (value.isEmpty()) {
            return null;
        }
        Class<?> type = targetType != null ? targetType.getObjectType() : Object.class;
        if (!isCamelConfigurationTarget(targetType)) {
            // this binding belongs to somebody else, so only resolve what has always been resolved here and
            // leave the rest alone rather than imposing Camel's bean reference syntax on it
            if (!value.startsWith("#")) {
                return null;
            }
        }
        if (applicationContext == null) {
            throw new IllegalArgumentException(
                    message(value, type, propertyPrefix, "there is no Spring application context available"));
        }
        if (value.startsWith(CLASS_PREFIX)) {
            throw new IllegalArgumentException(message(value, type, propertyPrefix,
                    "the #class: syntax is not supported here, declare the bean in the Spring application context instead"));
        }
        try {
            if (AUTOWIRED.equalsIgnoreCase(value)) {
                return applicationContext.getBean(type);
            }
            if (value.startsWith(TYPE_PREFIX)) {
                String fqn = value.substring(TYPE_PREFIX.length()).trim();
                Object bean = applicationContext.getBean(ClassUtils.forName(fqn, applicationContext.getClassLoader()));
                if (!type.isInstance(bean)) {
                    throw new IllegalArgumentException(message(value, type, propertyPrefix,
                            "the bean found by type is a [" + bean.getClass().getName()
                                                                     + "] which is not assignable to the option type"));
                }
                return bean;
            }
            String id = value;
            if (id.startsWith(BEAN_PREFIX)) {
                id = id.substring(BEAN_PREFIX.length()).trim();
            } else if (id.startsWith("#")) {
                id = id.substring(1).trim();
            }
            if (id.isEmpty()) {
                throw new IllegalArgumentException(message(value, type, propertyPrefix, "the bean id is empty"));
            }
            return applicationContext.getBean(id, type);
        } catch (BeansException | ClassNotFoundException | LinkageError e) {
            throw new IllegalArgumentException(message(value, type, propertyPrefix, e.getMessage()), e);
        }
    }

    /**
     * Whether the binding this conversion takes part in targets a Camel configuration class.
     * <p/>
     * Spring Boot's binder builds the target {@link TypeDescriptor} from the setter's {@link MethodParameter} (or from
     * the {@link Field} for field access), so the class being bound is reachable through
     * {@link TypeDescriptor#getSource()}. A class counts as Camel's own when it lives under
     * <tt>org.apache.camel</tt>, or when it is annotated with {@link ConfigurationProperties} for a prefix starting
     * with <tt>camel.</tt>.
     * <p/>
     * When the source does not identify a class this returns <tt>true</tt>, so that Camel's own binding is never
     * weakened by a shape of the binder this does not recognise.
     * <p/>
     * Note that this cannot be done in {@code ConditionalGenericConverter.matches}: Spring's
     * {@code GenericConversionService} caches the converter it picked per source/target {@code TypeDescriptor} pair,
     * and {@code TypeDescriptor.equals} ignores the source, so {@code matches} is consulted once for the first class
     * bound and the answer is then reused for every other class with a field of the same type.
     */
    static boolean isCamelConfigurationTarget(TypeDescriptor targetType) {
        Class<?> owner = boundClass(targetType);
        if (owner == null) {
            return true;
        }
        if (owner.getName().startsWith(CAMEL_PACKAGE)) {
            return true;
        }
        ConfigurationProperties annotation
                = AnnotatedElementUtils.findMergedAnnotation(owner, ConfigurationProperties.class);
        if (annotation != null) {
            String prefix = !annotation.prefix().isEmpty() ? annotation.prefix() : annotation.value();
            return prefix.startsWith(CAMEL_PROPERTY_PREFIX);
        }
        return false;
    }

    private static Class<?> boundClass(TypeDescriptor targetType) {
        Object source = targetType != null ? targetType.getSource() : null;
        if (source instanceof MethodParameter methodParameter) {
            Class<?> containing = methodParameter.getContainingClass();
            return containing != null ? containing : methodParameter.getDeclaringClass();
        }
        if (source instanceof Field field) {
            return field.getDeclaringClass();
        }
        return null;
    }

    private static String message(String value, Class<?> type, String propertyPrefix, String reason) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("Cannot resolve value [").append(value).append("] as a bean of type [").append(type.getName())
                .append("]");
        if (propertyPrefix != null && !propertyPrefix.isEmpty()) {
            sb.append(" while configuring ").append(propertyPrefix).append(".*");
        }
        sb.append(". Use #bean:myBeanId (or #myBeanId, or myBeanId) to refer to a bean in the Spring application"
                  + " context, or #autowired or #type:com.foo.MyType to refer to it by type. Reason: ").append(reason);
        return sb.toString();
    }

}
