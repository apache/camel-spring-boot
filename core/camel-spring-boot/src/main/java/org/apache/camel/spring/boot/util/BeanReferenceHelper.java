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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
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
 */
public final class BeanReferenceHelper {

    private static final String BEAN_PREFIX = "#bean:";
    private static final String TYPE_PREFIX = "#type:";
    private static final String CLASS_PREFIX = "#class:";
    private static final String AUTOWIRED = "#autowired";

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
     *                                  if a value was configured but cannot be resolved to a bean of the target type
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
                return applicationContext.getBean(ClassUtils.forName(fqn, applicationContext.getClassLoader()));
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
