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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate-starter", threadSafe = true,
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class StarterGeneratorMojo extends AbstractMojo {

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected ArtifactFactory artifactFactory;

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * The source pom template file.
     */
    @Parameter(defaultValue = "${basedir}/template-starter-pom.xml")
    protected File sourcePom;

    @Parameter(defaultValue = "${basedir}/../../components-starter")
    protected File startersDir;

    @Parameter(property = "action", required = true)
    protected String action;

    @Parameter(property = "name", required = true)
    protected String name;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!"create".equals(action) && !"delete".equals(action))
            throw new MojoFailureException("Unknown action: " + action);
        if (name == null || name.isEmpty())
            throw new MojoFailureException("Starter name must be specified as the parameter");
        if (name.startsWith("camel-"))
            name = name.substring("camel-".length());
        if (name.endsWith("-starter"))
            name = name.substring(0, name.length() - "-starter".length());
        try {
            doExecute();
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to " + action + " starter " + name, e);
        }
    }

    protected void doExecute() throws Exception {
        switch (action) {
            case "create":
                createStarter();
                break;
            case "delete":
                deleteStarter();
                break;
        }
    }

    private void deleteStarter() throws MojoFailureException, IOException {
        getLog().info("Deleting starter for " + name);
        File directory = new File(startersDir, "camel-" + name + "-starter");
        if (!directory.exists()) {
            throw new MojoFailureException("Starter does not exist: " + name);
        }
        FileUtils.deleteDirectory(directory);
        Path parent = new File(startersDir, "pom.xml").toPath();
        List<String> lines = Files.readAllLines(parent);
        int modulesStart = -1, modulesEnd = -1;
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            if (s.contains("<modules>"))
                modulesStart = i + 1;
            else if (s.contains("</modules>"))
                modulesEnd = i;
        }
        lines = concat(lines.subList(0, modulesStart).stream(),
                       lines.subList(modulesStart, modulesEnd).stream()
                            .filter(s -> !s.contains("<module>camel-" + name + "-starter</module>")),
                       lines.subList(modulesEnd, lines.size()).stream())
                 .collect(Collectors.toList());
        Files.write(parent, lines);
    }

    private void createStarter() throws MojoFailureException, IOException {
        getLog().info("Creating starter for " + name);
        File directory = new File(startersDir, "camel-" + name + "-starter");
        if (directory.exists()) {
            if (directory.isDirectory()) {
                throw new MojoFailureException("Directory already exists: " + directory);
            } else {
                throw new MojoFailureException("Can not create directory as a file already exists: " + directory);
            }
        }
        if (!directory.mkdirs()) {
            throw new MojoFailureException("Unable to create directory: " + directory);
        }
        Files.write(new File(directory, "pom.xml").toPath(),
                Files.lines(sourcePom.toPath())
                        .map(s -> s.replaceAll("%NAME%", name))
                        .collect(Collectors.toList()));
        Path parent = new File(startersDir, "pom.xml").toPath();
        List<String> lines = Files.readAllLines(parent);
        int modulesStart = -1, modulesEnd = -1;
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            if (s.contains("<modules>"))
                modulesStart = i + 1;
            else if (s.contains("</modules>"))
                modulesEnd = i;
        }
        lines = concat(lines.subList(0, modulesStart).stream(),
                       Stream.concat(lines.subList(modulesStart, modulesEnd).stream(),
                                     Stream.of("    <module>camel-" + name + "-starter</module>"))
                             .sorted().distinct(),
                       lines.subList(modulesEnd, lines.size()).stream())
                           .collect(Collectors.toList());
        Files.write(parent, lines);
    }

    private static <T> Stream<T> concat(Stream<T> s1, Stream<T> s2, Stream<T> s3) {
        return Stream.concat(s1, Stream.concat(s2, s3));
    }

}
