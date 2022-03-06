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

import org.apache.camel.impl.engine.DefaultPackageScanResourceResolver;
import org.apache.camel.util.IOHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * An implementation of the {@code org.apache.camel.spi.PackageScanResourceResolver} that is able to
 * scan spring-boot fat jars to find resources contained also in nested jars.
 */
public class FatJarPackageScanResourceResolver extends DefaultPackageScanResourceResolver {

    private static final String SPRING_BOOT_CLASSIC_LIB_ROOT = "lib/";
    private static final String SPRING_BOOT_BOOT_INF_LIB_ROOT = "BOOT-INF/lib/";
    private static final String SPRING_BOOT_BOOT_INF_CLASSES_ROOT = "BOOT-INF/classes/";
    private static final String SPRING_BOOT_WEB_INF_LIB_ROOT = "WEB-INF/lib/";
    private static final String SPRING_BOOT_WEB_INF_CLASSES_ROOT = "WEB-INF/classes/";

    @Override
    protected List<String> doLoadImplementationsInJar(String packageName, InputStream stream, String urlPath) {
        return doLoadImplementationsInJar(packageName,  stream, urlPath, true, true);
    }

    protected List<String> doLoadImplementationsInJar(String packageName, InputStream stream, String urlPath,
                                                      boolean inspectNestedJars, boolean closeStream) {
        List<String> entries = new ArrayList<>();

        JarInputStream jarStream = null;
        try {
            jarStream = new JarInputStream(stream);

            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                String name = entry.getName().trim();
                if (inspectNestedJars && !entry.isDirectory() && isSpringBootNestedJar(name)) {
                    String nestedUrl = urlPath + "!/" + name;
                    log.trace("Inspecting nested jar: {}", nestedUrl);
                    List<String> nestedEntries = doLoadImplementationsInJar(packageName, jarStream, nestedUrl, false, false);
                    entries.addAll(nestedEntries);
                } else if (!entry.isDirectory() && !name.endsWith(".class")) {
                    name = cleanupSpringBootClassName(name);
                    // name is FQN so it must start with package name
                    if (name.startsWith(packageName)) {
                        entries.add(name);
                    }
                }
            }
        } catch (IOException ioe) {
            log.warn("Cannot search jar file '" + urlPath + " due to an IOException: " + ioe.getMessage() + ". This exception is ignored.", ioe);
        } finally {
            if (closeStream) {
                // stream is left open when scanning nested jars, otherwise the fat jar stream gets closed
                IOHelper.close(jarStream, urlPath, log);
            }
        }

        return entries;
    }

    private boolean isSpringBootNestedJar(String name) {
        // Supporting both versions of the packaging model
        return name.endsWith(".jar") && (name.startsWith(SPRING_BOOT_CLASSIC_LIB_ROOT) || name.startsWith(SPRING_BOOT_BOOT_INF_LIB_ROOT) || name.startsWith(SPRING_BOOT_WEB_INF_LIB_ROOT));
    }

    private String cleanupSpringBootClassName(String name) {
        // Classes inside BOOT-INF/classes will be loaded by the new classloader as if they were in the root
        if (name.startsWith(SPRING_BOOT_BOOT_INF_CLASSES_ROOT)) {
            name = name.substring(SPRING_BOOT_BOOT_INF_CLASSES_ROOT.length());
        }
        if (name.startsWith(SPRING_BOOT_WEB_INF_CLASSES_ROOT)) {
            name = name.substring(SPRING_BOOT_WEB_INF_CLASSES_ROOT.length());
        }
        return name;
    }


}
