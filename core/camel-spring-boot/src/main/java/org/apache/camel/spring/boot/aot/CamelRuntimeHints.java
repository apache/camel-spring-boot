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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.apache.camel.spring.boot.aot.RuntimeHintsHelper.registerClassHierarchy;

/**
 * {@code CamelRuntimeHints} provide the basic hints for the native compilation of a Camel application.
 */
public final class CamelRuntimeHints implements RuntimeHintsRegistrar {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CamelRuntimeHints.class);

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Give access to the catalog
        hints.resources().registerPattern("org/apache/camel/main/*.properties");
        // Register all the camel services
        registerCamelServices(hints, classLoader);
        // Register collections
        hints.reflection().registerType(java.util.List.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
            MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(java.util.Collection.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
            MemberCategory.INVOKE_PUBLIC_METHODS);
    }

    /**
     * Register all the Camel services that could be found in the given classloader.
     *
     * @param hints       the hints contributed so far for the deployment unit
     * @param classLoader the ClassLoader to load classpath resources with,
     *                    or {@code null} for using the thread context class loader
     *                    at the time of actual resource access
     */
    private static void registerCamelServices(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("META-INF/services/org/apache/camel/*");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        try {
            for (Resource resource : resolver.getResources("classpath*:META-INF/services/org/apache/camel/**")) {
                String filename = resource.getFilename();
                if (filename == null || filename.isBlank() || filename.endsWith(".properties")) {
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(new StringReader(resource.getContentAsString(StandardCharsets.UTF_8)))) {
                    String line = reader.readLine();
                    String prefixClass = "class=";
                    while (line != null) {
                        if (line.startsWith("#") || line.isBlank()) {
                            line = reader.readLine();
                            continue;
                        }
                        String className = line.trim();
                        if (line.startsWith(prefixClass)) {
                            className = line.substring(prefixClass.length());
                        }
                        LOG.debug("Found the class {} to register", className);
                        registerClassHierarchy(hints, classLoader, className, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                                MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS,
                                MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.INTROSPECT_PUBLIC_METHODS);
                        line = reader.readLine();
                    }
                }
            }
        } catch (IOException e) {
            LOG.debug("Could not load the Camel services: {}", e.getMessage());
        }
    }
}
