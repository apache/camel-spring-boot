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
package org.apache.camel.itest.springboot.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArchetypeConfig {

    private static final String DEFAULT_ARTIFACT_ID = "test-project";
    private static final String DEFAULT_GROUP_ID = "com.example";
    private static final String DEFAULT_VERSION = "1.0-SNAPSHOT";
    private static final String DEFAULT_PACKAGE = "com.example";
    private static final String DEFAULT_MAIN_CLASS_NAME = "MySpringBootApplication";

    final String artifactId;
    final String groupId;
    final String version;
    final String packageName;
    final String mainClassName;
    private final List<String> dependencies;
    private final List<String> properties;
    private final boolean registerClusterServices;
    private final boolean webRequired;
    private final Map<String, String> sourceFiles;

    private ArchetypeConfig(Builder builder) {
        this.artifactId = builder.artifactId;
        this.groupId = builder.groupId;
        this.version = builder.version;
        this.packageName = builder.packageName;
        this.mainClassName = builder.mainClassName;
        this.dependencies = List.copyOf(builder.dependencies);
        this.properties = List.copyOf(builder.properties);
        this.registerClusterServices = builder.registerClusterServices;
        this.webRequired = builder.webRequired;
        this.sourceFiles = Map.copyOf(builder.sourceFiles);
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public String getMainClassFqn() {
        return packageName + "." + mainClassName;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public List<String> getProperties() {
        return properties;
    }

    public boolean isRegisterClusterServices() {
        return registerClusterServices;
    }

    public boolean isWebRequired() {
        return webRequired;
    }

    public Map<String, String> getSourceFiles() {
        return sourceFiles;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String artifactId = DEFAULT_ARTIFACT_ID;
        private String groupId = DEFAULT_GROUP_ID;
        private String version = DEFAULT_VERSION;
        private String packageName = DEFAULT_PACKAGE;
        private String mainClassName = DEFAULT_MAIN_CLASS_NAME;
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> properties = new ArrayList<>();
        private boolean registerClusterServices;
        private boolean webRequired;
        private final Map<String, String> sourceFiles = new LinkedHashMap<>();

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder mainClassName(String mainClassName) {
            this.mainClassName = mainClassName;
            return this;
        }

        public Builder dependency(String gav) {
            this.dependencies.add(gav);
            return this;
        }

        public Builder property(String entry) {
            this.properties.add(entry);
            return this;
        }

        public Builder registerClusterServices(boolean register) {
            this.registerClusterServices = register;
            return this;
        }

        public Builder webRequired(boolean webRequired) {
            this.webRequired = webRequired;
            return this;
        }

        /**
         * Adds a Java source file to the generated project before compilation.
         *
         * @param fqcn   fully qualified class name (e.g. {@code "com.example.TestConfig"})
         * @param source the Java source code
         */
        public Builder sourceFile(String fqcn, String source) {
            this.sourceFiles.put(fqcn, source);
            return this;
        }

        public ArchetypeConfig build() {
            return new ArchetypeConfig(this);
        }
    }
}
