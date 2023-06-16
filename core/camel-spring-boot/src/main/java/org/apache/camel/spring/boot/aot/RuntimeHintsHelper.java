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
package org.apache.camel.spring.boot.aot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.util.ClassUtils;

public final class RuntimeHintsHelper {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RuntimeHintsHelper.class);

    private RuntimeHintsHelper() {
    }


    /**
     * Register the given class and all its parent classes by applying the given member categories.
     *
     * @param hints            the hints contributed so far for the deployment unit
     * @param classLoader      the ClassLoader to load classpath resources with,
     *                         or {@code null} for using the thread context class loader
     *                         at the time of actual resource access
     * @param className        the name of the class to register
     * @param memberCategories the member categories to apply
     */
    public static void registerClassHierarchy(RuntimeHints hints, ClassLoader classLoader, String className,
                                              MemberCategory... memberCategories) {
        try {
            registerClassHierarchy(hints, ClassUtils.forName(className, classLoader), memberCategories);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            LOG.debug("The class {} cannot be found", className);
        }
    }

    /**
     * Register the given class and all its parent classes by applying the given member categories.
     *
     * @param hints            the hints contributed so far for the deployment unit
     * @param clazz            the class to register
     * @param memberCategories the member categories to apply
     */
    public static void registerClassHierarchy(RuntimeHints hints, Class<?> clazz, MemberCategory... memberCategories) {
        if (clazz.isInterface() || clazz.isArray()) {
            return;
        }
        while (clazz != Object.class) {
            hints.reflection().registerType(clazz, memberCategories);
            clazz = clazz.getSuperclass();
        }
    }
}
