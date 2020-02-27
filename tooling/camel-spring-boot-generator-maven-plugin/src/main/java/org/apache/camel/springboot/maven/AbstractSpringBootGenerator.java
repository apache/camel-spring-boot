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
package org.apache.camel.springboot.maven;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.apache.camel.tooling.util.PackageHelper;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractSpringBootGenerator extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                                                        threads it generated failed.
     * @throws MojoFailureException   something bad happened...
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project.getArtifactId().equals("components-starter")) {
            return;
        }
        // Do not generate code for ignored module
        if (isIgnore(getMainDepArtifactId())) {
            getLog().info("Skipping module contained in the ignore list");
            return;
        }

        try {
            executeAll();
        } catch (IOException e) {
            throw new MojoExecutionException("Error executing mojo", e);
        }
    }

    protected boolean isIgnore(String artifactId) {
        return false;
    }

    protected String getMainDepArtifactId() {
        return project.getArtifactId().substring(0, project.getArtifactId().length() - "-starter".length());
    }

    protected String getMainDepGroupId() {
        if ("camel-spring-boot-starter".equals(project.getArtifactId())) {
            return "org.apache.camel.springboot";
        } else if (project.getArtifactId().startsWith("camel-spring-cloud")) {
            // any of the camel-spring-cloud are from camel-spring-boot
            return "org.apache.camel.springboot";
        } else {
            // others are from camel
            return "org.apache.camel";
        }
    }

    protected Artifact getMainDep() {
        return project.getArtifactMap().get(getMainDepGroupId() + ":" + getMainDepArtifactId());
    }

    protected String getMainDepVersion() {
        return getMainDep().getVersion();
    }

    protected abstract void executeAll() throws MojoExecutionException, MojoFailureException, IOException;

    protected JarFile getJarFile() throws IOException {
        return getJarFile(getMainDepGroupId(), getMainDepArtifactId());
    }

    protected JarFile getJarFile(String groupId, String artifactId) throws IOException {
        return new JarFile(project.getArtifactMap().get(groupId + ":" + artifactId).getFile());
    }

    protected Map<String, Supplier<String>> getJSonFiles(JarFile componentJar) {
        Artifact mainDep = getMainDep();
        Map<String, Supplier<String>> files;
        files = componentJar.stream()
                .filter(je -> je.getName().endsWith(".json"))
                .collect(Collectors.toMap(je -> "jar:" + mainDep.getFile().toURI().toString() + "!" + je.getName(),
                        je -> cache(() -> loadJson(componentJar, je))));
        return files;
    }

    protected List<String> findComponentNames(JarFile componentJar) {
        return findNames(componentJar, "META-INF/services/org/apache/camel/component/");
    }

    protected List<String> findDataFormatNames(JarFile componentJar) {
        return findNames(componentJar, "META-INF/services/org/apache/camel/dataformat/");
    }

    protected List<String> findLanguageNames(JarFile componentJar) {
        return findNames(componentJar, "META-INF/services/org/apache/camel/language/");
    }

    protected List<String> findNames(JarFile componentJar, String dir) {
        return componentJar.stream()
                .filter(je -> !je.isDirectory())
                .map(ZipEntry::getName)
                .filter(s -> s.startsWith(dir))
                .map(s -> s.substring(dir.length()))
                .filter(s -> !s.startsWith(".") && !s.contains("/"))
                .collect(Collectors.toList());
    }

    protected static String loadJson(JarFile jar, JarEntry je) {
        try (InputStream is = jar.getInputStream(je)) {
            return PackageHelper.loadText(is);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    protected static <T> Supplier<T> cache(Supplier<T> supplier) {
        return new Supplier<T>() {
            T value;

            @Override
            public T get() {
                if (value == null) {
                    value = supplier.get();
                }
                return value;
            }
        };
    }

    protected String loadModelJson(Map<String, Supplier<String>> jsonFiles, String modelName) {
        return loadJsonOfType(jsonFiles, modelName + ".json", "model");
    }

    protected String loadComponentJson(Map<String, Supplier<String>> jsonFiles, String componentName) {
        return loadJsonOfType(jsonFiles, componentName + ".json", "component");
    }

    protected String loadDataFormatJson(Map<String, Supplier<String>> jsonFiles, String dataFormatName) {
        return loadJsonOfType(jsonFiles, dataFormatName + ".json", "dataformat");
    }

    protected String loadLanguageJson(Map<String, Supplier<String>> jsonFiles, String languageName) {
        return loadJsonOfType(jsonFiles, languageName + ".json", "language");
    }

    protected String loadOtherJson(Map<String, Supplier<String>> jsonFiles, String otherName) {
        return loadJsonOfType(jsonFiles, otherName + ".json", "other");
    }

    protected String loadJsonOfType(Map<String, Supplier<String>> jsonFiles, String modelName, String type) {
        for (Map.Entry<String, Supplier<String>> entry : jsonFiles.entrySet()) {
            if (entry.getKey().endsWith("/" + modelName)
                    || entry.getKey().endsWith("!" + modelName)) {
                String json = entry.getValue().get();
                if (json.contains("\"kind\": \"" + type + "\"")) {
                    return json;
                }
            }
        }
        return null;
    }

    protected void writeIfChanged(String content, File file) throws IOException {
        boolean write = true;
        file = file.getCanonicalFile();
        if (file.exists()) {
            try (FileReader fr = new FileReader(file)) {
                String oldContent = IOUtils.toString(fr);
                if (!content.equals(oldContent)) {
                    getLog().debug("Writing new file " + file.getAbsolutePath());
                    fr.close();
                } else {
                    getLog().debug("File " + file.getAbsolutePath() + " has been left unchanged");
                    write = false;
                }
            }
        } else {
            // Create the structure
            File parent = file.getParentFile();
            parent.mkdirs();
        }

        if (write) {
            try (FileWriter fw = new FileWriter(file)) {
                IOUtils.write(content, fw);
            }
        }
    }

}
