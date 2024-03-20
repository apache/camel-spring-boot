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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.JavaClass;
import org.jboss.forge.roaster.model.Method;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocCapableSource;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Mojo(name = "generate-config", threadSafe = true)
public class SpringBootConfigGeneratorMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * The maven session.
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    @Parameter
    private String sourceClassFile;

    @Parameter
    private String template;

    @Parameter
    private String outputFile;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            List<String> props = new ArrayList<>();
            getLog().info("Loading config from " + sourceClassFile);
            String classFile = Files.readString(Paths.get(sourceClassFile));
            JavaClass<JavaClassSource> inputClass = (JavaClass) Roaster.parse(classFile);
            List<Option> options = new ArrayList<>();
            for (Method<JavaClassSource, ?> mth : inputClass.getMethods()) {
                if (mth.getName().startsWith("set")) {
                    String name = mth.getName().substring(3);
                    String fieldName = name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1);
                    String propName = camelCaseToDash(name);
                    String javaType = mth.getParameters().get(0).getType().getQualifiedName();
                    String defaultValue = null;
                    Field<JavaClassSource> field = inputClass.getField(fieldName);
                    if (field != null && field.getLiteralInitializer() != null) {
                        defaultValue = field.getLiteralInitializer();
                    }
                    String description = "";
                    int idx =  classFile.indexOf("public void " + mth.getName());
                    if (idx > 0) {
                        String str = classFile.substring(0, idx);
                        idx = str.lastIndexOf("/**");
                        if (idx > 0) {
                            str = str.substring(idx);
                            description = str.trim().replace("\n    ", "\n        ");
                        }
                    }
                    options.add(new Option(fieldName, propName, javaType, defaultValue, description));
                }
            }

            Map<String, Object> context = new HashMap<>();
            context.put("options", options);
            String output = velocity(template, context);
            Path path = Paths.get(outputFile);
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                w.write(output);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to parse/generated config", e);
        }
    }

    protected String velocity(String templatePath, Map<String, Object> ctx) {
        Properties props = new Properties();
        props.setProperty("resource.loaders", "file");
        props.setProperty("resource.loader.file.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        props.setProperty("resource.loader.file.path", "");
        RuntimeInstance velocity = new RuntimeInstance();
        velocity.init(props);

        VelocityContext context = new VelocityContext();
        ctx.forEach(context::put);

        Template template = velocity.getTemplate(templatePath);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    public static String camelCaseToDash(String camelCaseName) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCaseName.length(); i++) {
            char c = camelCaseName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append("-");
                result.append(Character.toLowerCase(c));
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }


    public static class Option {

        private final String fieldName;
        private final String propName;
        private final String javaType;
        private final String defaultValue;
        private final String description;

        public Option(String fieldName, String propName, String javaType, String defaultValue, String description) {
            this.fieldName = fieldName;
            this.propName = propName;
            this.javaType = javaType;
            this.defaultValue = defaultValue;
            this.description = description;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getGetterName() {
            return (javaType.equals("boolean") ? "is" : "get")
                    + fieldName.substring(0, 1).toUpperCase(Locale.ROOT)
                    + fieldName.substring(1);
        }

        public String getSetterName() {
            return "set"
                    + fieldName.substring(0, 1).toUpperCase(Locale.ROOT)
                    + fieldName.substring(1);
        }

        public String getPropName() {
            return propName;
        }

        public String getJavaType() {
            return javaType;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getDescription() {
            return description;
        }

    }
}
