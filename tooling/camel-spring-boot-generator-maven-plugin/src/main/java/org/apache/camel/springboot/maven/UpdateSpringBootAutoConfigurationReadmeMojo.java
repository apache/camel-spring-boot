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

import static org.apache.camel.tooling.util.PackageHelper.loadText;
import static org.apache.camel.tooling.util.PackageHelper.writeText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.camel.maven.packaging.MvelHelper;
import org.apache.camel.springboot.maven.model.SpringBootAutoConfigureOptionModel;
import org.apache.camel.springboot.maven.model.SpringBootModel;
import org.apache.camel.tooling.util.Strings;
import org.apache.camel.util.json.DeserializationException;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.build.BuildContext;
import org.mvel2.templates.TemplateRuntime;

import javax.inject.Inject;

/**
 * For all the Camel components that has Spring Boot starter JAR, their documentation .adoc files in their component
 * directory is updated to include spring boot auto configuration options.
 */
@Mojo(name = "update-spring-boot-auto-configuration-readme", threadSafe = true)
public class UpdateSpringBootAutoConfigurationReadmeMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The project build directory
     *
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected File buildDir;

    /**
     * build context to check changed files and mark them for refresh (used for m2e compatibility)
     */
    @Inject
    private BuildContext buildContext;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            executeStarter(buildDir.getParentFile());
        } catch (Exception e) {
            throw new MojoFailureException("Error processing spring-configuration-metadata.json", e);
        }
    }

    // Copies json file to src/main/docs or, if there is none, writes a no-options file to src/main/docs
    // Is there a better way and place to do this?
    private void executeStarter(File starter) throws Exception {
        String name = starter.getName();

        // There's almost certainly a better test!
        if ("components-starter".equals(name) || "core".equals(name)) {
            return;
        }

        // remove camel- prefix and -starter suffix
        if (name.startsWith("camel-")) {
            name = name.substring(6);
        }
        if (name.endsWith("-starter")) {
            name = name.substring(0, name.length() - 8);
        }
        String componentName = name;
        getLog().debug("Camel component: " + componentName);
        Path starterPath = starter.toPath();
        Path docFolder = starterPath.resolve("src/main/docs/");
        Files.createDirectories(docFolder);
        Path jsonFileCopy = docFolder.resolve(componentName + ".json");

        File jsonFile = new File(buildDir, "classes/META-INF/spring-configuration-metadata.json");

        if (jsonFile.exists()) {
            getLog().debug("Copying generated Spring Boot auto-configuration file to: " + jsonFileCopy);
            byte[] contents = Files.readAllBytes(jsonFile.toPath());
            Files.write(jsonFileCopy, contents);
        }
    }

}
