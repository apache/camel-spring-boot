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
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.camel.maven.packaging.AbstractGeneratorMojo;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.ComponentModel.ComponentOptionModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.DataFormatModel.DataFormatOptionModel;
import org.apache.camel.tooling.model.EipModel;
import org.apache.camel.tooling.model.EipModel.EipOptionModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.LanguageModel;
import org.apache.camel.tooling.model.LanguageModel.LanguageOptionModel;
import org.apache.camel.tooling.model.Strings;
import org.apache.camel.tooling.util.PackageHelper;
import org.apache.camel.tooling.util.srcgen.Annotation;
import org.apache.camel.tooling.util.srcgen.GenericType;
import org.apache.camel.tooling.util.srcgen.JavaClass;
import org.apache.camel.tooling.util.srcgen.Method;
import org.apache.camel.tooling.util.srcgen.Property;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import static org.apache.camel.tooling.util.Strings.camelCaseToDash;

/**
 * Generate Spring Boot auto configuration files for Camel components and data
 * formats.
 */
@Mojo(name = "prepare-spring-boot-auto-configuration", threadSafe = true,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class SpringBootAutoConfigurationMojo extends AbstractSpringBootGenerator {

    private static final String[] IGNORE_MODULES = {"camel-spring-boot-xml", "camel-spring-boot-engine"};

    /**
     * Useful to move configuration towards starters. Warning: the
     * spring.factories files sometimes are used also on the main artifacts.
     * Make sure it is not the case before enabling this property.
     */
    private static final boolean DELETE_FILES_ON_MAIN_ARTIFACTS = false;

    private static final Map<String, String> PRIMITIVEMAP;
    private static final Map<Type, Type> PRIMITIVE_CLASSES;

    static {
        PRIMITIVE_CLASSES = new HashMap<>();
        PRIMITIVE_CLASSES.put(boolean.class, Boolean.class);
        PRIMITIVE_CLASSES.put(char.class, Character.class);
        PRIMITIVE_CLASSES.put(long.class, Long.class);
        PRIMITIVE_CLASSES.put(int.class, Integer.class);
        PRIMITIVE_CLASSES.put(byte.class, Byte.class);
        PRIMITIVE_CLASSES.put(short.class, Short.class);
        PRIMITIVE_CLASSES.put(double.class, Double.class);
        PRIMITIVE_CLASSES.put(float.class, Float.class);
        PRIMITIVEMAP = new HashMap<>();
        PRIMITIVEMAP.put("boolean", "java.lang.Boolean");
        PRIMITIVEMAP.put("char", "java.lang.Character");
        PRIMITIVEMAP.put("long", "java.lang.Long");
        PRIMITIVEMAP.put("int", "java.lang.Integer");
        PRIMITIVEMAP.put("integer", "java.lang.Integer");
        PRIMITIVEMAP.put("byte", "java.lang.Byte");
        PRIMITIVEMAP.put("short", "java.lang.Short");
        PRIMITIVEMAP.put("double", "java.lang.Double");
        PRIMITIVEMAP.put("float", "java.lang.Float");
    }

    /**
     * The output directory for generated component schema file
     */
    @Parameter(defaultValue = "${project.build.directory}/classes")
    protected File classesDir;

    /**
     * The base directory
     */
    @Parameter(defaultValue = "${basedir}")
    protected File baseDir;

    DynamicClassLoader projectClassLoader;

    JarFile componentJar;

    @Override
    protected boolean isIgnore(String artifactId) {
        return Arrays.asList(IGNORE_MODULES).contains(artifactId);
    }

    protected void executeAll() throws MojoExecutionException, MojoFailureException, IOException {
        if ("camel-core-engine".equals(getMainDepArtifactId())) {
            executeAll(getMainDepGroupId(), "camel-core-engine");
            executeAll(getMainDepGroupId(), "camel-core-model");
            executeAll(getMainDepGroupId(), "camel-core-languages");
        } else {
            executeAll(getMainDepGroupId(), getMainDepArtifactId());
        }
    }

    private void executeAll(String groupId, String artifactId) throws MojoExecutionException, MojoFailureException, IOException {
        try (JarFile jar = getJarFile(groupId, artifactId)) {
            if (jar != null) {
                Map<String, Supplier<String>> files = getJSonFiles(jar);
                executeModels(jar, files);
                executeComponents(jar, files);
                executeDataFormats(jar, files);
                executeLanguages(jar, files);
            }
        }
    }

    private void executeModels(JarFile componentJar, Map<String, Supplier<String>> files) throws MojoExecutionException, MojoFailureException {
        // Resilience4j
        String json = loadModelJson(files, "resilience4jConfiguration");
        if (json != null) {
            EipModel model = JsonMapper.generateEipModel(json);

            int pos = model.getJavaType().lastIndexOf(".");
            String pkg = model.getJavaType().substring(0, pos) + ".springboot";

            // Generate properties, auto-configuration for camel-core-starter
            createEipModelConfigurationSource(pkg, model, "camel.resilience4j", true);
        }

        // Consul
        json = loadModelJson(files, "consulServiceDiscovery");
        if (json != null) {
            EipModel model = JsonMapper.generateEipModel(json);

            int pos = model.getJavaType().lastIndexOf(".");
            String pkg = model.getJavaType().substring(0, pos) + ".springboot";

            // Generate properties, auto-configuration for camel-core-starter
            createEipModelConfigurationSource(pkg, model, "camel.cloud.consul.service-discovery", true);
        }

        // DNS
        json = loadModelJson(files, "dnsServiceDiscovery");
        if (json != null) {
            EipModel model = JsonMapper.generateEipModel(json);

            int pos = model.getJavaType().lastIndexOf(".");
            String pkg = model.getJavaType().substring(0, pos) + ".springboot";

            // Generate properties, auto-configuration for camel-core-starter
            createEipModelConfigurationSource(pkg, model, "camel.cloud.dns.service-discovery", true);
        }

        // Etcd
        json = loadModelJson(files, "etcdServiceDiscovery");
        if (json != null) {
            EipModel model = JsonMapper.generateEipModel(json);

            int pos = model.getJavaType().lastIndexOf(".");
            String pkg = model.getJavaType().substring(0, pos) + ".springboot";

            // Generate properties, auto-configuration for camel-core-starter
            createEipModelConfigurationSource(pkg, model, "camel.cloud.etcd.service-discovery", true);
        }

        // Kubernetes
        json = loadModelJson(files, "kubernetesServiceDiscovery");
        if (json != null) {
            EipModel model = JsonMapper.generateEipModel(json);

            int pos = model.getJavaType().lastIndexOf(".");
            String pkg = model.getJavaType().substring(0, pos) + ".springboot";

            // Generate properties, auto-configuration happens in
            // camel-kubernetes-starter
            createEipModelConfigurationSource(pkg, model, "camel.cloud.kubernetes.service-discovery", true);
        }

        // Rest
        json = loadModelJson(files, "restConfiguration");
        if (json != null) {
            EipModel model = JsonMapper.generateEipModel(json);

            int pos = model.getJavaType().lastIndexOf(".");
            String pkg = model.getJavaType().substring(0, pos) + ".springboot";

            // Generate properties, auto-configuration for camel-core-starter
            createRestConfigurationSource(pkg, model, "camel.rest");
            createRestModuleAutoConfigurationSource(pkg, model);
        }
    }

    private void createEipModelConfigurationSource(String packageName, EipModel model, String propertiesPrefix, boolean generatedNestedConfig) throws MojoFailureException {
        final int pos = model.getJavaType().lastIndexOf(".");
        final String commonName = model.getJavaType().substring(pos + 1) + (generatedNestedConfig ? "Common" : "Properties");
        final String configName = model.getJavaType().substring(pos + 1) + (generatedNestedConfig ? "Properties" : null);

        // Common base class
        JavaClass commonClass = new JavaClass(getProjectClassLoader());
        commonClass.setPackage(packageName);
        commonClass.setName(commonName);

        String doc = "Generated by camel-package-maven-plugin - do not edit this file!";
        if (!Strings.isNullOrEmpty(model.getDescription())) {
            doc = model.getDescription() + "\n\n" + doc;
        }
        commonClass.getJavaDoc().setFullText(doc);

        for (EipOptionModel option : model.getOptions()) {
            String type = option.getJavaType();
            String name = option.getName();

            if ("id".equalsIgnoreCase(name) || "parent".equalsIgnoreCase(name) || "camelContext".equalsIgnoreCase(name)) {
                // Skip them as they should not be set via spring boot
                continue;
            }

            if ("java.util.List<org.apache.camel.model.PropertyDefinition>".equalsIgnoreCase(type)) {
                type = "java.util.Map<java.lang.String, java.lang.String>";
            }

            // generate inner class for non-primitive options
            Property prop = commonClass.addProperty(type, option.getName());
            if (!Strings.isNullOrEmpty(option.getDescription())) {
                prop.getField().getJavaDoc().setFullText(option.getDescription());
            }
            if (!isBlank(option.getDefaultValue())) {
                if ("java.lang.String".equals(type)) {
                    prop.getField().setStringInitializer(option.getDefaultValue().toString());
                } else if ("long".equals(type) || "java.lang.Long".equals(type)) {
                    // the value should be a Long number
                    String value = option.getDefaultValue() + "L";
                    prop.getField().setLiteralInitializer(value);
                } else if ("integer".equals(option.getType()) || "java.lang.Integer".equals(option.getJavaType())
                        || "boolean".equals(option.getType()) || "java.lang.Boolean".equals(option.getJavaType())) {
                    prop.getField().setLiteralInitializer(option.getDefaultValue().toString());
                } else if (!isBlank(option.getEnums())) {
                    String enumShortName = type.substring(type.lastIndexOf(".") + 1);
                    prop.getField().setLiteralInitializer(enumShortName + "." + option.getDefaultValue());
                    commonClass.addImport(model.getJavaType());
                }
            }
        }

        writeSourceIfChanged(commonClass, packageName.replaceAll("\\.", "\\/") + "/" + commonName + ".java", true);

        Class commonClazz = generateDummyClass(commonClass.getCanonicalName());

        // Config class
        if (generatedNestedConfig) {
            JavaClass configClass = new JavaClass(getProjectClassLoader());
            configClass.setPackage(packageName);
            configClass.setName(configName);
            configClass.extendSuperType(commonClass);
            configClass.addAnnotation(loadClass("org.springframework.boot.context.properties.ConfigurationProperties")).setStringValue("prefix", propertiesPrefix);
            configClass.addImport(Map.class);
            configClass.addImport(HashMap.class);
            configClass.removeImport(commonClass);

            configClass.addField().setName("enabled").setType(boolean.class).setPrivate().setLiteralInitializer("true").getJavaDoc().setFullText("Enable the component");
            configClass.addField().setName("configurations").setType(loadType("java.util.Map<java.lang.String, " + packageName + "." + commonName + ">")).setPrivate()
                    .setLiteralInitializer("new HashMap<>()").getJavaDoc().setFullText("Define additional configuration definitions");

            Method method;

            method = configClass.addMethod();
            method.setName("getConfigurations");
            method.setReturnType(loadType("java.util.Map<java.lang.String, " + packageName + "." + commonName + ">"));
            method.setPublic();
            method.setBody("return configurations;");

            method = configClass.addMethod();
            method.setName("isEnabled");
            method.setReturnType(boolean.class);
            method.setPublic();
            method.setBody("return enabled;");

            method = configClass.addMethod();
            method.setName("setEnabled");
            method.addParameter(boolean.class, "enabled");
            method.setPublic();
            method.setBody("this.enabled = enabled;");

            String fileName = packageName.replaceAll("\\.", "\\/") + "/" + configName + ".java";
            writeSourceIfChanged(configClass, fileName, true);
        }
    }

    private void createRestConfigurationSource(String packageName, EipModel model, String propertiesPrefix) throws MojoFailureException {
        final int pos = model.getJavaType().lastIndexOf(".");
        final String className = model.getJavaType().substring(pos + 1) + "Properties";

        generateDummyClass(packageName + "." + className);

        // Common base class
        JavaClass javaClass = new JavaClass(getProjectClassLoader());
        javaClass.setPackage(packageName);
        javaClass.setName(className);
        javaClass.addAnnotation("org.springframework.boot.context.properties.ConfigurationProperties").setStringValue("prefix", propertiesPrefix);

        String doc = "Generated by camel-package-maven-plugin - do not edit this file!";
        if (!Strings.isNullOrEmpty(model.getDescription())) {
            doc = model.getDescription() + "\n\n" + doc;
        }
        javaClass.getJavaDoc().setFullText(doc);

        for (EipOptionModel option : model.getOptions()) {
            String type = option.getJavaType();
            String name = option.getName();

            if ("id".equalsIgnoreCase(name) || "parent".equalsIgnoreCase(name) || "camelContext".equalsIgnoreCase(name)) {
                // Skip them as they should not be set via spring boot
                continue;
            }

            if ("java.util.List<org.apache.camel.model.PropertyDefinition>".equalsIgnoreCase(type)) {
                type = "java.util.Map<java.lang.String, java.lang.String>";
            } else if ("java.util.List<org.apache.camel.model.rest.RestPropertyDefinition>".equalsIgnoreCase(type)) {
                type = "java.util.Map<java.lang.String, java.lang.Object>";
            }

            // to avoid ugly names such as c-o-r-s
            if ("enableCORS".equalsIgnoreCase(name)) {
                name = "enableCors";
            }

            // generate inner class for non-primitive options
            Property prop = javaClass.addProperty(type, name);
            if (!Strings.isNullOrEmpty(option.getDescription())) {
                prop.getField().getJavaDoc().setFullText(option.getDescription());
            }
            if (!isBlank(option.getDefaultValue())) {
                if ("java.lang.String".equals(type)) {
                    prop.getField().setStringInitializer(option.getDefaultValue().toString());
                } else if ("long".equals(type) || "java.lang.Long".equals(type)) {
                    // the value should be a Long number
                    String value = option.getDefaultValue() + "L";
                    prop.getField().setLiteralInitializer(value);
                } else if ("integer".equals(option.getType()) || "java.lang.Integer".equals(option.getJavaType())
                        || "boolean".equals(option.getType()) || "java.lang.Boolean".equals(option.getJavaType())) {
                    prop.getField().setLiteralInitializer(option.getDefaultValue().toString());
                } else if (!isBlank(option.getEnums())) {
                    String enumShortName = type.substring(type.lastIndexOf(".") + 1);
                    prop.getField().setLiteralInitializer(enumShortName + "." + option.getDefaultValue());
                    javaClass.addImport(model.getJavaType());
                }
            }
        }

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + className + ".java";
        writeSourceIfChanged(javaClass, fileName, true);
    }

    private void createRestModuleAutoConfigurationSource(String packageName, EipModel model) throws MojoFailureException {
        final JavaClass javaClass = new JavaClass(getProjectClassLoader());
        final int pos = model.getJavaType().lastIndexOf(".");
        final String name = model.getJavaType().substring(pos + 1) + "AutoConfiguration";
        final String configType = model.getJavaType().substring(pos + 1) + "Properties";

        javaClass.setPackage(packageName);
        javaClass.setName(name);

        String doc = "Generated by camel-package-maven-plugin - do not edit this file!";
        javaClass.getJavaDoc().setFullText(doc);

        javaClass.addAnnotation(Configuration.class).setLiteralValue("proxyBeanMethods", "false");
        javaClass.addAnnotation(ConditionalOnBean.class).setStringValue("type", "org.apache.camel.spring.boot.CamelAutoConfiguration");
        javaClass.addAnnotation(ConditionalOnProperty.class).setStringValue("name", "camel.rest.enabled").setLiteralValue("matchIfMissing", "true");
        javaClass.addAnnotation(AutoConfigureAfter.class).setStringValue("name", "org.apache.camel.spring.boot.CamelAutoConfiguration");
        javaClass.addAnnotation(EnableConfigurationProperties.class).setLiteralValue("value", configType + ".class");

        javaClass.addImport("java.util.Map");
        javaClass.addImport("java.util.HashMap");
        javaClass.addImport("org.apache.camel.util.CollectionHelper");
        javaClass.addImport("org.apache.camel.support.IntrospectionSupport");
        javaClass.addImport("org.apache.camel.spring.boot.util.CamelPropertiesHelper");
        javaClass.addImport("org.apache.camel.CamelContext");
        javaClass.addImport("org.apache.camel.spi.RestConfiguration");

        javaClass.addField().setName("camelContext").setType(loadClass("org.apache.camel.CamelContext")).setPrivate().addAnnotation(Autowired.class);
        javaClass.addField().setName("config").setType(loadClass(packageName + "." + configType)).setPrivate().addAnnotation(Autowired.class);

        Method method;

        // Configuration
        method = javaClass.addMethod();
        method.setName("configure" + model.getShortJavaType());
        method.setPublic();
        method.addThrows(Exception.class);
        method.setReturnType(loadClass("org.apache.camel.spi.RestConfiguration"));
        method.addAnnotation(Lazy.class);
        method.addAnnotation(Bean.class).setLiteralValue("name", "\"rest-configuration\"");
        method.addAnnotation(ConditionalOnClass.class).setLiteralValue("value", "CamelContext.class");
        method.addAnnotation(ConditionalOnMissingBean.class);
        method.setBody("" + "Map<String, Object> properties = new HashMap<>();\n" + "IntrospectionSupport.getProperties(config, properties, null, false);\n"
                + "// These options is configured specially further below, so remove them first\n" + "properties.remove(\"enableCors\");\n"
                + "properties.remove(\"apiProperty\");\n" + "properties.remove(\"componentProperty\");\n" + "properties.remove(\"consumerProperty\");\n"
                + "properties.remove(\"dataFormatProperty\");\n" + "properties.remove(\"endpointProperty\");\n" + "properties.remove(\"corsHeaders\");\n" + "\n"
                + "RestConfiguration definition = new RestConfiguration();\n" + "CamelPropertiesHelper.setCamelProperties(camelContext, definition, properties, true);\n"
                + "\n" + "// Workaround for spring-boot properties name as It would appear\n" + "// as enable-c-o-r-s if left uppercase in Configuration\n"
                + "definition.setEnableCORS(config.getEnableCors());\n" + "\n" + "if (config.getApiProperty() != null) {\n"
                + "    definition.setApiProperties(new HashMap<>(CollectionHelper.flattenKeysInMap(config.getApiProperty(), \".\")));\n" + "}\n"
                + "if (config.getComponentProperty() != null) {\n"
                + "    definition.setComponentProperties(new HashMap<>(CollectionHelper.flattenKeysInMap(config.getComponentProperty(), \".\")));\n" + "}\n"
                + "if (config.getConsumerProperty() != null) {\n"
                + "    definition.setConsumerProperties(new HashMap<>(CollectionHelper.flattenKeysInMap(config.getConsumerProperty(), \".\")));\n" + "}\n"
                + "if (config.getDataFormatProperty() != null) {\n"
                + "    definition.setDataFormatProperties(new HashMap<>(CollectionHelper.flattenKeysInMap(config.getDataFormatProperty(), \".\")));\n" + "}\n"
                + "if (config.getEndpointProperty() != null) {\n"
                + "    definition.setEndpointProperties(new HashMap<>(CollectionHelper.flattenKeysInMap(config.getEndpointProperty(), \".\")));\n" + "}\n"
                + "if (config.getCorsHeaders() != null) {\n" + "    Map<String, Object> map = CollectionHelper.flattenKeysInMap(config.getCorsHeaders(), \".\");\n"
                + "    Map<String, String> target = new HashMap<>();\n" + "    map.forEach((k, v) -> target.put(k, v.toString()));\n"
                + "    definition.setCorsHeaders(target);\n" + "}\n" + "return definition;");

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, true);
        writeComponentSpringFactorySource(packageName, name);
    }

    private void executeComponents(JarFile componentJar, Map<String, Supplier<String>> jsonFiles) throws MojoFailureException {
        // find the component names
        List<String> componentNames = findComponentNames(componentJar);

        // create auto configuration for the components
        if (!componentNames.isEmpty()) {
            getLog().debug("Found " + componentNames.size() + " components");

            List<ComponentModel> allModels = new LinkedList<>();
            for (String componentName : componentNames) {
                String json = loadComponentJson(jsonFiles, componentName);
                if (json != null) {
                    ComponentModel model = JsonMapper.generateComponentModel(json);
                    allModels.add(model);
                }
            }

            // Group the models by implementing classes
            Map<String, List<ComponentModel>> grModels = allModels.stream().collect(Collectors.groupingBy(ComponentModel::getJavaType));
            for (String componentClass : grModels.keySet()) {
                List<ComponentModel> compModels = grModels.get(componentClass);
                ComponentModel model = compModels.get(0); // They should be
                // equivalent
                List<String> aliases = compModels.stream().map(ComponentModel::getScheme).sorted().collect(Collectors.toList());

                // use springboot as sub package name so the code is not in normal
                // package so the Spring Boot JARs can be optional at runtime
                int pos = model.getJavaType().lastIndexOf(".");
                String pkg = model.getJavaType().substring(0, pos) + ".springboot";

                String overrideComponentName = null;
                if (aliases.size() > 1) {
                    // determine component name when there are multiple ones
                    overrideComponentName = model.getArtifactId().replace("camel-", "");
                }

                beforeGenerateComponentSource(model);

                boolean complexOptions = model.getOptions().stream().anyMatch(this::isComplexType);
                createComponentConfigurationSource(pkg, model, overrideComponentName);
                createComponentAutoConfigurationSource(pkg, model, overrideComponentName, complexOptions);
                if (complexOptions) {
                    createComponentConverterSource(pkg, model);
                }
                createComponentSpringFactorySource(pkg, model);
            }
        }
    }

    private void beforeGenerateComponentSource(ComponentModel model) {
        if ("activemq".equals(model.getScheme()) || "amqp".equals(model.getScheme()) || "stomp".equals(model.getScheme())) {
            // we want brokerURL to be brokerUrl so its generated with a nice name for spring boot (and some other options too)
            model.getComponentOptions().stream()
                    .filter(o -> "brokerURL".equals(o.getName())).findFirst().ifPresent(o -> o.setName("brokerUrl"));
            model.getComponentOptions().stream()
                    .filter(o -> "useMessageIDAsCorrelationID".equals(o.getName())).findFirst().ifPresent(o -> o.setName("useMessageIdAsCorrelationId"));
            model.getComponentOptions().stream()
                    .filter(o -> "includeAllJMSXProperties".equals(o.getName())).findFirst().ifPresent(o -> o.setName("includeAllJmsxProperties"));
            model.getComponentOptions().stream()
                    .filter(o -> "includeSentJMSMessageID".equals(o.getName())).findFirst().ifPresent(o -> o.setName("includeSentJmsMessageId"));
        }
    }

    private void executeDataFormats(JarFile componentJar, Map<String, Supplier<String>> jsonFiles) throws MojoFailureException {
        // find the data format names
        List<String> dataFormatNames = findDataFormatNames(componentJar);

        // create auto configuration for the data formats
        if (!dataFormatNames.isEmpty()) {
            getLog().debug("Found " + dataFormatNames.size() + " dataformats");

            List<DataFormatModel> allModels = new LinkedList<>();
            for (String dataFormatName : dataFormatNames) {
                String json = loadDataFormatJson(jsonFiles, dataFormatName);
                if (json != null) {
                    DataFormatModel model = JsonMapper.generateDataFormatModel(json);
                    allModels.add(model);
                }
            }

            // Group the models by implementing classes
            Map<String, List<DataFormatModel>> grModels = allModels.stream().collect(Collectors.groupingBy(DataFormatModel::getJavaType));
            for (String dataFormatClass : grModels.keySet()) {
                List<DataFormatModel> dfModels = grModels.get(dataFormatClass);
                DataFormatModel model = dfModels.get(0); // They should be
                // equivalent
                List<String> aliases = dfModels.stream().map(DataFormatModel::getName).sorted().collect(Collectors.toList());

                // use springboot as sub package name so the code is not in
                // normal
                // package so the Spring Boot JARs can be optional at runtime
                int pos = model.getJavaType().lastIndexOf(".");
                String pkg = model.getJavaType().substring(0, pos) + ".springboot";

                String overrideDataFormatName = null;
                if (aliases.size() > 1) {
                    // determine component name when there are multiple ones
                    overrideDataFormatName = model.getArtifactId().replace("camel-", "");
                }


                boolean complexOptions = model.getOptions().stream().anyMatch(this::isComplexType);
                createDataFormatConfigurationSource(pkg, model, overrideDataFormatName);
                createDataFormatAutoConfigurationSource(pkg, model, overrideDataFormatName, complexOptions);
                if (complexOptions) {
                    createDataFormatConverterSource(pkg, model);
                }
                createDataFormatSpringFactorySource(pkg, model);
            }
        }
    }

    private void executeLanguages(JarFile componentJar, Map<String, Supplier<String>> jsonFiles) throws MojoFailureException {
        // find the language names
        List<String> languageNames = findLanguageNames(componentJar);

        // create auto configuration for the languages
        if (!languageNames.isEmpty()) {
            getLog().debug("Found " + languageNames.size() + " languages");

            List<LanguageModel> allModels = new LinkedList<>();
            for (String languageName : languageNames) {
                String json = loadLanguageJson(jsonFiles, languageName);
                if (json != null) {
                    LanguageModel model = JsonMapper.generateLanguageModel(json);
                    allModels.add(model);
                }
            }

            // Group the models by implementing classes
            Map<String, List<LanguageModel>> grModels = allModels.stream().collect(Collectors.groupingBy(LanguageModel::getJavaType));
            for (String languageClass : grModels.keySet()) {
                List<LanguageModel> dfModels = grModels.get(languageClass);
                LanguageModel model = dfModels.get(0); // They should be
                // equivalent
                List<String> aliases = dfModels.stream().map(LanguageModel::getName).sorted().collect(Collectors.toList());

                // use springboot as sub package name so the code is not in
                // normal
                // package so the Spring Boot JARs can be optional at runtime
                int pos = model.getJavaType().lastIndexOf(".");
                String pkg = model.getJavaType().substring(0, pos) + ".springboot";

                String overrideLanguageName = null;
                if (aliases.size() > 1) {
                    // determine language name when there are multiple ones
                    overrideLanguageName = model.getArtifactId().replace("camel-", "");
                }

                boolean complexOptions = model.getOptions().stream().anyMatch(this::isComplexType);
                createLanguageConfigurationSource(pkg, model, overrideLanguageName);
                createLanguageAutoConfigurationSource(pkg, model, overrideLanguageName, complexOptions);
                if (complexOptions) {
                    createLanguageConverterSource(pkg, model);
                }
                createLanguageSpringFactorySource(pkg, model);
            }
        }
    }

    private void createComponentConfigurationSource(String packageName, ComponentModel model, String overrideComponentName) throws MojoFailureException {
        int pos = model.getJavaType().lastIndexOf(".");
        String name = model.getJavaType().substring(pos + 1);
        name = name.replace("Component", "ComponentConfiguration");

        final JavaClass javaClass = new JavaClass(getProjectClassLoader());
        javaClass.setPackage(packageName);
        javaClass.setName(name);
        javaClass.extendSuperType("ComponentConfigurationPropertiesCommon");
        javaClass.addImport("org.apache.camel.spring.boot.ComponentConfigurationPropertiesCommon");

        // add bogus field for enabled so spring boot tooling can get the
        // javadoc as description in its metadata
        Property bogus = javaClass.addProperty("java.lang.Boolean", "enabled");
        String scheme = overrideComponentName != null ? overrideComponentName : model.getScheme();
        bogus.getField().getJavaDoc().setText("Whether to enable auto configuration of the " + scheme + " component. This is enabled by default.");
        bogus.removeAccessor();
        bogus.removeMutator();

        String doc = "Generated by camel-package-maven-plugin - do not edit this file!";
        if (!Strings.isNullOrEmpty(model.getDescription())) {
            doc = model.getDescription() + "\n\n" + doc;
        }
        javaClass.getJavaDoc().setText(doc);

        String prefix = "camel.component." + camelCaseToDash(overrideComponentName != null ? overrideComponentName : model.getScheme());
        // make sure prefix is in lower case
        prefix = prefix.toLowerCase(Locale.US);
        javaClass.addAnnotation("org.springframework.boot.context.properties.ConfigurationProperties").setStringValue("prefix", prefix);

        for (ComponentOptionModel option : model.getComponentOptions()) {

            if (skipComponentOption(model, option)) {
                // some component options should be skipped
                continue;
            }

            String type = option.getJavaType();

            // generate inner class for non-primitive options
            type = getSimpleJavaType(type);

            Property prop = javaClass.addProperty(type, option.getName());
            if (option.isDeprecated()) {
                prop.getField().addAnnotation(Deprecated.class);
                prop.getAccessor().addAnnotation(Deprecated.class);
                prop.getMutator().addAnnotation(Deprecated.class);
                // DeprecatedConfigurationProperty must be on getter when deprecated
                prop.getAccessor().addAnnotation(DeprecatedConfigurationProperty.class);
            }
            if (!Strings.isNullOrEmpty(option.getDescription())) {
                String desc = option.getDescription();
                boolean complex = isComplexTypeOrDuration(option) && isBlank(option.getEnums());
                if (complex) {
                    if (!desc.endsWith(".")) {
                        desc = desc + ".";
                    }
                    desc = desc + " The option is a " + option.getJavaType() + " type.";
                }
                prop.getField().getJavaDoc().setFullText(desc);
            }
            if (!isBlank(option.getDefaultValue())) {
                if ("java.lang.String".equals(option.getJavaType())) {
                    prop.getField().setStringInitializer(option.getDefaultValue().toString());
                } else if ("duration".equals(option.getType())) {
                    String value = convertDurationToMills(option.getDefaultValue().toString());
                    // duration is either long or int java type
                    if ("long".equals(option.getJavaType()) || "java.lang.Long".equals(option.getJavaType())) {
                        value = value + "L";
                    }
                    prop.getField().setLiteralInitializer(value);
                } else if ("long".equals(option.getJavaType()) || "java.lang.Long".equals(option.getJavaType())) {
                    // the value should be a Long number
                    String value = option.getDefaultValue() + "L";
                    prop.getField().setLiteralInitializer(value);
                } else if ("integer".equals(option.getType()) || "java.lang.Integer".equals(option.getJavaType())
                        || "boolean".equals(option.getType()) || "java.lang.Boolean".equals(option.getJavaType())) {
                    prop.getField().setLiteralInitializer(option.getDefaultValue().toString());
                } else if (!isBlank(option.getEnums())) {
                    String enumShortName = type.substring(type.lastIndexOf(".") + 1);
                    prop.getField().setLiteralInitializer(enumShortName + "." + option.getDefaultValue());
                    javaClass.addImport(model.getJavaType());
                }
            }
        }

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, true);
    }

    private String convertDurationToMills(String pattern) {
        String value;
        pattern = pattern.toLowerCase();
        if (pattern.contains("ms")) {
            pattern = pattern.replaceAll("ms", "");
        }
        try {
            Duration d = Duration.parse("PT" + pattern);
            value = String.valueOf(d.toMillis());
        } catch (java.time.format.DateTimeParseException e) {
            value = pattern;
        }
        return value;
    }

    private boolean isBlank(Object value) {
        if (value instanceof String) {
            return "".equals(value);
        } else if (value instanceof Collection) {
            return ((Collection) value).isEmpty();
        } else {
            return value == null;
        }
    }

    private boolean isComplexTypeOrDuration(ComponentOptionModel option) {
        // we can configure map/list/set types from spring-boot so do not regard them as complex
        if (option.getJavaType().startsWith("java.util.Map") || option.getJavaType().startsWith("java.util.List") || option.getJavaType().startsWith("java.util.Set")) {
            return false;
        }
        // all the object types are complex
        return "object".equals(option.getType()) || "duration".equals(option.getType());
    }

    private boolean isComplexType(ComponentOptionModel option) {
        // we can configure map/list/set types from spring-boot so do not regard them as complex
        if (option.getJavaType().startsWith("java.util.Map") || option.getJavaType().startsWith("java.util.List") || option.getJavaType().startsWith("java.util.Set")) {
            return false;
        }
        // enums are not complex
        if (option.getEnums() != null && !option.getEnums().isEmpty()) {
            return false;
        }
        // java.lang.Object are not complex
        if ("java.lang.Object".equals(option.getJavaType())) {
            return false;
        }
        // all the object types are complex
        return "object".equals(option.getType());
    }

    private boolean isComplexType(DataFormatOptionModel option) {
        // we can configure map/list/set types from spring-boot so do not regard them as complex
        if (option.getJavaType().startsWith("java.util.Map") || option.getJavaType().startsWith("java.util.List") || option.getJavaType().startsWith("java.util.Set")) {
            return false;
        }
        // enums are not complex
        if (option.getEnums() != null && !option.getEnums().isEmpty()) {
            return false;
        }
        // all the object types are complex
        return "object".equals(option.getType());
    }

    private boolean isComplexType(LanguageOptionModel option) {
        // we can configure map/list/set types from spring-boot so do not regard them as complex
        if (option.getJavaType().startsWith("java.util.Map") || option.getJavaType().startsWith("java.util.List") || option.getJavaType().startsWith("java.util.Set")) {
            return false;
        }
        // enums are not complex
        if (option.getEnums() != null && !option.getEnums().isEmpty()) {
            return false;
        }
        // all the object types are complex
        return "object".equals(option.getType());
    }

    private GenericType loadType(String type) throws MojoFailureException {
        try {
            return GenericType.parse(type, getProjectClassLoader());
        } catch (ClassNotFoundException e) {
            throw new MojoFailureException("Unable to load type", e);
        }
    }

    // try loading class, looking for inner classes if needed
    private Class<?> loadClass(String loadClassName) throws MojoFailureException {
        Class<?> optionClass;
        while (true) {
            try {
                optionClass = getProjectClassLoader().loadClass(loadClassName);
                break;
            } catch (ClassNotFoundException e) {
                int dotIndex = loadClassName.lastIndexOf('.');
                if (dotIndex == -1) {
                    throw new MojoFailureException(e.getMessage(), e);
                } else {
                    loadClassName = loadClassName.substring(0, dotIndex) + "$" + loadClassName.substring(dotIndex + 1);
                }
            }
        }
        return optionClass;
    }

    protected DynamicClassLoader getProjectClassLoader() {
        if (projectClassLoader == null) {
            final List<String> classpathElements;
            try {
                classpathElements = project.getTestClasspathElements();
            } catch (org.apache.maven.artifact.DependencyResolutionRequiredException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            projectClassLoader = DynamicClassLoader.createDynamicClassLoader(classpathElements);
        }
        return projectClassLoader;
    }

    private String getSimpleJavaType(String type) {
        // use wrapper types for primitive types so a null mean that the option
        // has not been configured
        String wrapper = PRIMITIVEMAP.get(type);
        if (wrapper != null) {
            type = wrapper;
        }
        return type;
    }

    // read java type from project, returns null if not found
    private JavaClass readJavaType(String type) {
        if (!type.startsWith("java.lang.") && (!type.contains("<") || !type.contains(">"))) {
            String sourceCode = "";
            try {
                Class<?> clazz = getProjectClassLoader().loadClass(type);
                URL url = clazz != null ? getProjectClassLoader().getResource(clazz.getName().replace('.', '/') + ".class") : null;
                Artifact mainDep = project.getArtifactMap().get(getMainDepGroupId() + ":" + getMainDepArtifactId());
                if (url == null || mainDep == null || !url.toString().contains(mainDep.getFile().toURI().toString())) {
                    return null;
                }
                JavaClass nestedType = new JavaClass(getProjectClassLoader()).setPackage(clazz.getPackage().getName()).setName(clazz.getSimpleName()).setEnum(clazz.isEnum())
                        .setClass(!clazz.isInterface()).setAbstract((clazz.getModifiers() & Modifier.ABSTRACT) != 0).setStatic((clazz.getModifiers() & Modifier.STATIC) != 0)
                        .extendSuperType(clazz.getGenericSuperclass() != null ? new GenericType(clazz.getGenericSuperclass()).toString() : null);

                List<java.lang.reflect.Method> publicMethods = Stream.of(clazz.getDeclaredMethods()).filter(m -> Modifier.isPublic(m.getModifiers())).collect(Collectors.toList());
                List<java.lang.reflect.Method> allSetters = publicMethods.stream().filter(m -> m.getReturnType() == void.class || m.getReturnType() == clazz)
                        .filter(m -> m.getParameterCount() == 1).filter(m -> m.getName().matches("set[A-Z][a-zA-Z0-9]*")).collect(Collectors.toList());
                List<java.lang.reflect.Method> allGetters = publicMethods.stream().filter(m -> m.getReturnType() != void.class).filter(m -> m.getParameterCount() == 0)
                        .filter(m -> m.getName().matches("(get|is)[A-Z][a-zA-Z0-9]*")).collect(Collectors.toList());
                allSetters.stream()
                        .sorted(Comparator.<java.lang.reflect.Method>comparingInt(m -> getSetterPosition(sourceCode, m))
                                .thenComparing(java.lang.reflect.Method::getName))
                        .map(m -> StringUtils.uncapitalize(m.getName().substring(3)))
                        .forEach(fn -> {
                            Class<?> ft;
                            Type wft;
                            boolean isBoolean;
                            java.lang.reflect.Field field = Stream.of(clazz.getDeclaredFields()).filter(f -> f.getName().equals(fn)).findAny().orElse(null);
                            List<java.lang.reflect.Method> setters = allSetters.stream().filter(m -> m.getName().equals("set" + StringUtils.capitalize(fn))).collect(Collectors.toList());
                            List<java.lang.reflect.Method> getters = allGetters.stream()
                                    .filter(m -> m.getName().equals("get" + StringUtils.capitalize(fn)) || m.getName().equals("is" + StringUtils.capitalize(fn))).collect(Collectors.toList());
                            java.lang.reflect.Method mutator;
                            java.lang.reflect.Method accessor;
                            if (setters.size() == 1) {
                                mutator = setters.get(0);
                                ft = mutator.getParameterTypes()[0];
                                wft = PRIMITIVE_CLASSES.getOrDefault(ft, ft);
                                isBoolean = ft == boolean.class || ft == Boolean.class;
                                accessor = allGetters.stream()
                                        .filter(m -> m.getName().equals("get" + StringUtils.capitalize(fn)) || isBoolean && m.getName().equals("is" + StringUtils.capitalize(fn)))
                                        .filter(m -> PRIMITIVE_CLASSES.getOrDefault(m.getReturnType(), m.getReturnType()) == wft).findAny().orElse(null);
                            } else if (field != null) {
                                ft = field.getType();
                                wft = PRIMITIVE_CLASSES.getOrDefault(ft, ft);
                                isBoolean = ft == boolean.class || ft == Boolean.class;
                                mutator = allSetters.stream().filter(m -> m.getName().equals("set" + StringUtils.capitalize(fn)))
                                        .filter(m -> PRIMITIVE_CLASSES.getOrDefault(m.getParameterTypes()[0], m.getParameterTypes()[0]) == wft).findAny().orElse(null);
                                accessor = allGetters.stream()
                                        .filter(m -> m.getName().equals("get" + StringUtils.capitalize(fn)) || isBoolean && m.getName().equals("is" + StringUtils.capitalize(fn)))
                                        .filter(m -> PRIMITIVE_CLASSES.getOrDefault(m.getReturnType(), m.getReturnType()) == wft).findAny().orElse(null);
                            } else {
                                if (getters.size() == 1) {
                                    ft = getters.get(0).getReturnType();
                                } else {
                                    throw new IllegalStateException("Unable to determine type for property " + fn);
                                }
                                wft = PRIMITIVE_CLASSES.getOrDefault(ft, ft);
                                mutator = setters.stream().filter(m -> PRIMITIVE_CLASSES.getOrDefault(m.getParameterTypes()[0], m.getParameterTypes()[0]) == wft).findAny().orElse(null);
                                accessor = getters.stream().filter(m -> PRIMITIVE_CLASSES.getOrDefault(m.getReturnType(), m.getReturnType()) == wft).findAny().orElse(null);
                            }
                            if (mutator == null) {
                                throw new IllegalStateException("Could not find mutator for property " + fn);
                            }
                            Property property = nestedType.addProperty(new GenericType(wft), fn);
                            property.getMutator().getJavaDoc().setText(getSetterJavaDoc(sourceCode, fn));
                            for (java.lang.annotation.Annotation ann : mutator.getAnnotations()) {
                                addAnnotation(ac -> property.getMutator().addAnnotation(ac), ann);
                            }
                            if (accessor != null) {
                                for (java.lang.annotation.Annotation ann : accessor.getAnnotations()) {
                                    addAnnotation(ac -> property.getAccessor().addAnnotation(ac), ann);
                                }
                            } else {
                                property.removeAccessor();
                            }
                            if (field != null) {
                                for (java.lang.annotation.Annotation ann : field.getAnnotations()) {
                                    addAnnotation(ac -> property.getField().addAnnotation(ac), ann);
                                }
                            } else {
                                property.removeField();
                            }
                        });
                return nestedType;
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private int getSetterPosition(String sourceCode, java.lang.reflect.Method m) {
        int i0 = sourceCode.indexOf("void " + m.getName() + "(");
        int i1 = sourceCode.indexOf(m.getDeclaringClass().getSimpleName() + " " + m.getName() + "(");
        int l = sourceCode.length();
        return Math.min(i0 > 0 ? i0 : l, i1 > 0 ? i1 : l);
    }

    private String getSetterJavaDoc(String sourceCode, String name) {
        int idx = sourceCode.indexOf("public void set" + StringUtils.capitalize(name) + "(");
        if (idx > 0) {
            sourceCode = sourceCode.substring(0, idx);
            idx = sourceCode.lastIndexOf("/**");
            if (idx > 0) {
                sourceCode = sourceCode.substring(idx + 3);
                idx = sourceCode.indexOf("*/");
                if (idx > 0) {
                    sourceCode = sourceCode.substring(0, idx);
                    List<String> lines = Stream.of(sourceCode.split("\n")).map(String::trim).map(s -> s.startsWith("*") ? s.substring(1) : s).map(String::trim)
                            .filter(s -> !s.isEmpty()).collect(Collectors.toList());
                    int lastLine = 0;
                    while (lastLine < lines.size()) {
                        if (lines.get(lastLine).startsWith("@")) {
                            break;
                        }
                        lastLine++;
                    }
                    sourceCode = lines.subList(0, lastLine).stream().map(s -> s.replaceAll("  ", " ")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(" "));
                    return sourceCode;
                }

            }
        }
        return null;
    }

    private void addAnnotation(Function<Class<? extends java.lang.annotation.Annotation>, Annotation> creator, java.lang.annotation.Annotation ann) {
        Class<? extends java.lang.annotation.Annotation> ac = ann.annotationType();
        Annotation a = creator.apply(ac);
        for (java.lang.reflect.Method m : ac.getMethods()) {
            if ("equals".equals(m.getName()) || "toString".equals(m.getName()) || "hashCode".equals(m.getName())) {
                continue;
            }
            String n = m.getName();
            try {
                Object v = m.invoke(ann);
                if (v != null) {
                    a.setLiteralValue(n, v.toString());
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to retrieve annotation value " + n + " on " + ac.getName());
            }
        }
    }

    // CHECKSTYLE:OFF
    private static boolean skipComponentOption(ComponentModel model, ComponentOptionModel option) {
        if ("netty-http".equals(model.getScheme())) {
            String name = option.getName();
            if (name.equals("textline") || name.equals("delimiter") || name.equals("autoAppendDelimiter") || name.equals("decoderMaxLineLength") || name.equals("encoding")
                    || name.equals("allowDefaultCodec") || name.equals("udpConnectionlessSending") || name.equals("networkInterface") || name.equals("clientMode")
                    || name.equals("reconnect") || name.equals("reconnectInterval") || name.equals("useByteBuf") || name.equals("udpByteArrayCodec") || name.equals("broadcast")) {
                return true;
            }
        }
        return false;
    }
    // CHECKSTYLE:ON

    private void createDataFormatConfigurationSource(String packageName, DataFormatModel model, String overrideDataFormatName) throws MojoFailureException {
        final JavaClass javaClass = new JavaClass(getProjectClassLoader());

        int pos = model.getJavaType().lastIndexOf(".");
        String name = model.getJavaType().substring(pos + 1);
        name = name.replace("DataFormat", "DataFormatConfiguration");
        javaClass.setPackage(packageName).setName(name);
        javaClass.extendSuperType("DataFormatConfigurationPropertiesCommon");
        javaClass.addImport("org.apache.camel.spring.boot.DataFormatConfigurationPropertiesCommon");

        // add bogus field for enabled so spring boot tooling can get the
        // javadoc as description in its metadata
        Property bogus = javaClass.addProperty("java.lang.Boolean", "enabled");
        bogus.getField().getJavaDoc().setText("Whether to enable auto configuration of the " + model.getName() + " data format. This is enabled by default.");
        bogus.removeAccessor();
        bogus.removeMutator();

        String doc = "Generated by camel-package-maven-plugin - do not edit this file!";
        if (!Strings.isNullOrEmpty(model.getDescription())) {
            doc = model.getDescription() + "\n\n" + doc;
        }
        javaClass.getJavaDoc().setFullText(doc);

        String prefix = "camel.dataformat." + camelCaseToDash(overrideDataFormatName != null ? overrideDataFormatName : model.getName());
        // make sure prefix is in lower case
        prefix = prefix.toLowerCase(Locale.US);
        javaClass.addAnnotation("org.springframework.boot.context.properties.ConfigurationProperties").setStringValue("prefix", prefix);

        for (DataFormatOptionModel option : model.getOptions()) {
            // skip option with name id in data format as we do not need that
            if ("id".equals(option.getName())) {
                continue;
            }
            Object defaultValue = option.getDefaultValue();
            String type = option.getJavaType();
            type = getSimpleJavaType(type);

            // special for bindy
            if ("org.apache.camel.model.dataformat.BindyType".equals(option.getJavaType())) {
                // force to use a string type
                type = "java.lang.String";
                defaultValue = null;
            } else if (option.getJavaType().contains("org.apache.camel.model.dataformat")) {
                // skip options that are from the model as they are not possible to configure anyway
                continue;
            }

            // spring-boot auto configuration does not support complex types
            // (unless they are enum, nested)
            // and if so then we should use a String type so spring-boot and its
            // tooling support that
            // as Camel will be able to convert the string value into a lookup
            // of the bean in the registry anyway
            // and therefore there is no problem, eg
            // camel.component.jdbc.data-source = myDataSource
            // where the type would have been javax.sql.DataSource
            boolean complex = isComplexType(option) && isBlank(option.getEnums());
            if (complex) {
                // force to use a string type
                type = "java.lang.String";
            }

            Property prop = javaClass.addProperty(type, option.getName());
            if (option.isDeprecated()) {
                prop.getField().addAnnotation(Deprecated.class);
                prop.getAccessor().addAnnotation(Deprecated.class);
                prop.getMutator().addAnnotation(Deprecated.class);
                // DeprecatedConfigurationProperty must be on getter when
                // deprecated
                prop.getAccessor().addAnnotation(DeprecatedConfigurationProperty.class);
            }
            if (!Strings.isNullOrEmpty(option.getDescription())) {
                String desc = option.getDescription();
                if (complex) {
                    if (!desc.endsWith(".")) {
                        desc = desc + ".";
                    }
                    desc = desc + " The option is a " + option.getJavaType() + " type.";
                }
                prop.getField().getJavaDoc().setFullText(desc);
            }
            if (!isBlank(defaultValue)) {
                if ("java.lang.String".equals(option.getJavaType())) {
                    prop.getField().setStringInitializer(defaultValue.toString());
                } else if ("long".equals(option.getJavaType()) || "java.lang.Long".equals(option.getJavaType())) {
                    // the value should be a Long number
                    String value = defaultValue + "L";
                    prop.getField().setLiteralInitializer(value);
                } else if ("integer".equals(option.getType()) || "java.lang.Integer".equals(option.getJavaType())
                        || "boolean".equals(option.getType()) || "java.lang.Boolean".equals(option.getJavaType())) {
                    prop.getField().setLiteralInitializer(defaultValue.toString());
                } else if (!isBlank(option.getEnums())) {
                    String enumShortName = type.substring(type.lastIndexOf(".") + 1);
                    prop.getField().setLiteralInitializer(enumShortName + "." + defaultValue);
                    javaClass.addImport(model.getJavaType());
                }
            }
        }

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, true);
    }

    private void createLanguageConfigurationSource(String packageName, LanguageModel model, String overrideLanguageName) throws MojoFailureException {
        final JavaClass javaClass = new JavaClass(getProjectClassLoader());

        int pos = model.getJavaType().lastIndexOf(".");
        String name = model.getJavaType().substring(pos + 1);
        name = name.replace("Language", "LanguageConfiguration");
        javaClass.setPackage(packageName).setName(name);
        javaClass.extendSuperType("LanguageConfigurationPropertiesCommon");
        javaClass.addImport("org.apache.camel.spring.boot.LanguageConfigurationPropertiesCommon");

        // add bogus field for enabled so spring boot tooling can get the
        // javadoc as description in its metadata
        Property bogus = javaClass.addProperty("java.lang.Boolean", "enabled");
        bogus.getField().getJavaDoc().setText("Whether to enable auto configuration of the " + model.getName() + " language. This is enabled by default.");
        bogus.removeAccessor();
        bogus.removeMutator();

        String doc = "Generated by camel-package-maven-plugin - do not edit this file!";
        if (!Strings.isNullOrEmpty(model.getDescription())) {
            doc = model.getDescription() + "\n\n" + doc;
        }
        javaClass.getJavaDoc().setFullText(doc);

        String prefix = "camel.language." + (camelCaseToDash(overrideLanguageName != null ? overrideLanguageName : model.getName()));
        // make sure prefix is in lower case
        prefix = prefix.toLowerCase(Locale.US);
        javaClass.addAnnotation("org.springframework.boot.context.properties.ConfigurationProperties").setStringValue("prefix", prefix);

        for (LanguageOptionModel option : model.getOptions()) {
            // skip option with name id, or expression in language as we do not
            // need that and skip resultType as they are not global options
            if ("id".equals(option.getName()) || "expression".equals(option.getName()) || "resultType".equals(option.getName())) {
                continue;
            }
            // CHECKSTYLE:OFF
            if ("bean".equals(model.getName())) {
                // and skip following as they are not global options
                if ("bean".equals(option.getName()) || "ref".equals(option.getName()) || "method".equals(option.getName()) || "beanType".equals(option.getName())) {
                    continue;
                }
            } else if ("tokenize".equals(model.getName())) {
                // and skip following as they are not global options
                if ("token".equals(option.getName()) || "endToken".equals(option.getName()) || "inheritNamespaceTagName".equals(option.getName())
                        || "headerName".equals(option.getName()) || "regex".equals(option.getName()) || "xml".equals(option.getName()) || "includeTokens".equals(option.getName())
                        || "group".equals(option.getName()) || "skipFirst".equals(option.getName())) {
                    continue;
                }
            } else if ("xtokenize".equals(model.getName())) {
                // and skip following as they are not global options
                if ("headerName".equals(option.getName()) || "group".equals(option.getName())) {
                    continue;
                }
            } else if ("xpath".equals(model.getName())) {
                // and skip following as they are not global options
                if ("headerName".equals(option.getName())) {
                    continue;
                }
            } else if ("xquery".equals(model.getName())) {
                // and skip following as they are not global options
                if ("headerName".equals(option.getName())) {
                    continue;
                }
            }
            // CHECKSTYLE:ON
            String type = option.getJavaType();
            type = getSimpleJavaType(type);

            // spring-boot auto configuration does not support complex types
            // (unless they are enum, nested)
            // and if so then we should use a String type so spring-boot and its
            // tooling support that
            // as Camel will be able to convert the string value into a lookup
            // of the bean in the registry anyway
            // and therefore there is no problem, eg
            // camel.component.jdbc.data-source = myDataSource
            // where the type would have been javax.sql.DataSource
            boolean complex = isComplexType(option) && isBlank(option.getEnums());
            if (complex) {
                // force to use a string type
                type = "java.lang.String";
            }

            Property prop = javaClass.addProperty(type, option.getName());
            if (option.isDeprecated()) {
                prop.getField().addAnnotation(Deprecated.class);
                prop.getAccessor().addAnnotation(Deprecated.class);
                prop.getMutator().addAnnotation(Deprecated.class);
                // DeprecatedConfigurationProperty must be on getter when
                // deprecated
                prop.getAccessor().addAnnotation(DeprecatedConfigurationProperty.class);
            }
            if (!Strings.isNullOrEmpty(option.getDescription())) {
                String desc = option.getDescription();
                if (complex) {
                    if (!desc.endsWith(".")) {
                        desc = desc + ".";
                    }
                    desc = desc + " The option is a " + option.getJavaType() + " type.";
                }
                prop.getField().getJavaDoc().setFullText(desc);
            }
            if (!isBlank(option.getDefaultValue())) {
                if ("java.lang.String".equals(option.getJavaType())) {
                    prop.getField().setStringInitializer(option.getDefaultValue().toString());
                } else if ("long".equals(option.getJavaType()) || "java.lang.Long".equals(option.getJavaType())) {
                    // the value should be a Long number
                    String value = option.getDefaultValue() + "L";
                    prop.getField().setLiteralInitializer(value);
                } else if ("integer".equals(option.getType()) || "java.lang.Integer".equals(option.getJavaType())
                        || "boolean".equals(option.getType()) || "java.lang.Boolean".equals(option.getJavaType())) {
                    prop.getField().setLiteralInitializer(option.getDefaultValue().toString());
                } else if (isBlank(option.getEnums())) {
                    String enumShortName = type.substring(type.lastIndexOf(".") + 1);
                    prop.getField().setLiteralInitializer(enumShortName + "." + option.getDefaultValue());
                    javaClass.addImport(model.getJavaType());
                }
            }
        }

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, true);
    }

    private Class<?> generateDummyClass(String clazzName) {
        return getProjectClassLoader().generateDummyClass(clazzName);
    }

    private void createComponentAutoConfigurationSource(String packageName, ComponentModel model, String overrideName, boolean complexOptions)
            throws MojoFailureException {

        final String name = model.getJavaType().substring(model.getJavaType().lastIndexOf(".") + 1).replace("Component", "ComponentAutoConfiguration");
        final String configurationName = name.replace("ComponentAutoConfiguration", "ComponentConfiguration");
        final String componentName = camelCaseToDash(overrideName != null ? overrideName : model.getScheme()).toLowerCase(Locale.US);
        final Class<?> configClass = generateDummyClass(packageName + "." + configurationName);
        JavaClass javaClass = new JavaClass(getProjectClassLoader());

        javaClass.setPackage(packageName);
        javaClass.setName(name);
        javaClass.getJavaDoc().setFullText("Generated by camel-package-maven-plugin - do not edit this file!");
        javaClass.addAnnotation(Configuration.class).setLiteralValue("proxyBeanMethods", "false");
        javaClass.addAnnotation(Conditional.class).setLiteralValue("ConditionalOnCamelContextAndAutoConfigurationBeans.class");
        javaClass.addAnnotation(EnableConfigurationProperties.class)
                .setLiteralValue("{ComponentConfigurationProperties.class," + configurationName + ".class}");

        javaClass.addAnnotation("org.apache.camel.spring.boot.util.ConditionalOnHierarchicalProperties")
                .setStringArrayValue(
                        "value",
                        new String[]{"camel.component", "camel.component." + componentName});

        if (complexOptions) {
            String cn = name.replace("ComponentAutoConfiguration", "ComponentConverter");
            javaClass.addAnnotation(AutoConfigureAfter.class).setLiteralValue("{CamelAutoConfiguration.class, " + cn + ".class}");
        } else {
            javaClass.addAnnotation(AutoConfigureAfter.class).setLiteralValue("CamelAutoConfiguration.class");
        }

        javaClass.addImport(ApplicationContext.class);
        javaClass.addImport("org.apache.camel.CamelContext");
        javaClass.addImport("org.apache.camel.Component");
        javaClass.addImport("org.apache.camel.spi.ComponentCustomizer");
        javaClass.addImport("org.apache.camel.spring.boot.CamelAutoConfiguration");
        javaClass.addImport("org.apache.camel.spring.boot.ComponentConfigurationProperties");
        javaClass.addImport("org.apache.camel.spring.boot.util.CamelPropertiesHelper");
        javaClass.addImport("org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans");
        javaClass.addImport("org.apache.camel.spring.boot.util.HierarchicalPropertiesEvaluator");
        javaClass.addImport(model.getJavaType());

        javaClass.addField().setPrivate().setName("applicationContext").setType(ApplicationContext.class).addAnnotation(Autowired.class);
        javaClass.addField().setPrivate().setFinal(true).setName("camelContext").setType(loadClass("org.apache.camel.CamelContext"));
        javaClass.addField().setPrivate().setName("configuration").setType(configClass).addAnnotation(Autowired.class);

        Method ctr = javaClass.addMethod().setConstructor(true).setPublic().setName(name).addParameter("org.apache.camel.CamelContext", "camelContext");
        ctr.setBody("this.camelContext = camelContext;\n");

        // add method for auto configure
        String body = createComponentBody(model.getShortJavaType(), componentName);
        String methodName = "configure" + model.getShortJavaType();

        Method method = javaClass.addMethod()
                .setName(methodName)
                .setPublic()
                .setBody(body)
                .setReturnType(loadType("org.apache.camel.spi.ComponentCustomizer"));
        method.addAnnotation(Lazy.class);
        method.addAnnotation(Bean.class);

        sortImports(javaClass);

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, false);
    }

    private void createComponentConverterSource(String packageName, ComponentModel model)
            throws MojoFailureException {

        final String name = model.getJavaType().substring(model.getJavaType().lastIndexOf(".") + 1).replace("Component", "ComponentConverter");

        // create converter class and write source
        JavaClass javaClass = new JavaClass(getProjectClassLoader());
        javaClass.setPackage(packageName);
        javaClass.setName(name);
        javaClass.getJavaDoc().setFullText("Generated by camel-package-maven-plugin - do not edit this file!");
        javaClass.addAnnotation(Configuration.class).setLiteralValue("proxyBeanMethods", "false");
        javaClass.addAnnotation("org.springframework.boot.context.properties.ConfigurationPropertiesBinding");
        javaClass.addAnnotation("org.springframework.stereotype.Component");
        javaClass.addImport("java.util.LinkedHashSet");
        javaClass.addImport("java.util.Set");
        javaClass.addImport("org.springframework.core.convert.TypeDescriptor");
        javaClass.addImport("org.springframework.core.convert.converter.GenericConverter");

        javaClass.implementInterface("GenericConverter");
        javaClass.addField().setPrivate().setName("applicationContext").setType(loadClass("org.springframework.context.ApplicationContext")).addAnnotation(Autowired.class);

        String body = createConverterPairBody(model);
        javaClass.addMethod().setName("getConvertibleTypes").setPublic().setReturnType("Set<ConvertiblePair>")
                .setBody(body);
        body = createConvertBody(model);
        javaClass.addMethod().setName("convert").setPublic().setReturnType("Object")
                .addParameter("Object", "source")
                .addParameter("TypeDescriptor", "sourceType")
                .addParameter("TypeDescriptor", "targetType")
                .setBody(body);
        sortImports(javaClass);

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, false);

        writeComponentSpringFactorySource(packageName, name);
    }

    private String createConvertBody(ComponentModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (source == null) {\n");
        sb.append("    return null;\n");
        sb.append("}\n");
        sb.append("String ref = source.toString();\n");
        sb.append("if (!ref.startsWith(\"#\")) {\n");
        sb.append("    return null;\n");
        sb.append("}\n");
        sb.append("ref = ref.startsWith(\"#bean:\") ? ref.substring(6) : ref.substring(1);\n");
        sb.append("switch (targetType.getName()) {\n");
        // we need complex types only which unique types only
        Stream<String> s = model.getComponentOptions().stream().filter(this::isComplexType).map(SpringBootAutoConfigurationMojo::getJavaType).distinct();
        s.forEach(type -> {
            sb.append("    case \"").append(type).append("\": return applicationContext.getBean(ref, ").append(type).append(".class);\n");
        });
        sb.append("}\n");
        sb.append("return null;\n");
        return sb.toString();
    }

    private String createConvertBody(DataFormatModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (source == null) {\n");
        sb.append("    return null;\n");
        sb.append("}\n");
        sb.append("String ref = source.toString();\n");
        sb.append("if (!ref.startsWith(\"#\")) {\n");
        sb.append("    return null;\n");
        sb.append("}\n");
        sb.append("ref = ref.startsWith(\"#bean:\") ? ref.substring(6) : ref.substring(1);\n");
        sb.append("switch (targetType.getName()) {\n");
        // we need complex types only which unique types only
        Stream<String> s = model.getOptions().stream().filter(this::isComplexType).map(SpringBootAutoConfigurationMojo::getJavaType).distinct();
        s.forEach(type -> {
            sb.append("    case \"").append(type).append("\": return applicationContext.getBean(ref, ").append(type).append(".class);\n");
        });
        sb.append("}\n");
        sb.append("return null;\n");
        return sb.toString();
    }

    private String createConvertBody(LanguageModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (source == null) {\n");
        sb.append("    return null;\n");
        sb.append("}\n");
        sb.append("String ref = source.toString();\n");
        sb.append("if (!ref.startsWith(\"#\")) {\n");
        sb.append("    return null;\n");
        sb.append("}\n");
        sb.append("ref = ref.startsWith(\"#bean:\") ? ref.substring(6) : ref.substring(1);\n");
        sb.append("switch (targetType.getName()) {\n");
        // we need complex types only which unique types only
        Stream<String> s = model.getOptions().stream().filter(this::isComplexType).map(SpringBootAutoConfigurationMojo::getJavaType).distinct();
        s.forEach(type -> {
            sb.append("    case \"").append(type).append("\": return applicationContext.getBean(ref, ").append(type).append(".class);\n");
        });
        sb.append("}\n");
        sb.append("return null;\n");
        return sb.toString();
    }

    private String createConverterPairBody(ComponentModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("Set<ConvertiblePair> answer = new LinkedHashSet<>();\n");
        // we need complex types only which unique types only
        Stream<String> s = model.getOptions().stream().filter(this::isComplexType).map(SpringBootAutoConfigurationMojo::getJavaType).distinct();
        s.forEach(type -> {
            sb.append("answer.add(new ConvertiblePair(String.class, ");
            sb.append(type);
            sb.append(".class));\n");
        });
        sb.append("return answer;\n");
        return sb.toString();
    }

    private String createConverterPairBody(DataFormatModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("Set<ConvertiblePair> answer = new LinkedHashSet<>();\n");
        // we need complex types only which unique types only
        Stream<String> s = model.getOptions().stream().filter(this::isComplexType).map(SpringBootAutoConfigurationMojo::getJavaType).distinct();
        s.forEach(type -> {
            sb.append("answer.add(new ConvertiblePair(String.class, ");
            sb.append(type);
            sb.append(".class));\n");
        });
        sb.append("return answer;\n");
        return sb.toString();
    }

    private String createConverterPairBody(LanguageModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("Set<ConvertiblePair> answer = new LinkedHashSet<>();\n");
        // we need complex types only which unique types only
        Stream<String> s = model.getOptions().stream().filter(this::isComplexType).map(SpringBootAutoConfigurationMojo::getJavaType).distinct();
        s.forEach(type -> {
            sb.append("answer.add(new ConvertiblePair(String.class, ");
            sb.append(type);
            sb.append(".class));\n");
        });
        sb.append("return answer;\n");
        return sb.toString();
    }

    private void createDataFormatAutoConfigurationSource(String packageName, DataFormatModel model, String overrideName, boolean complexOptions)
            throws MojoFailureException {

        final String name = model.getJavaType().substring(model.getJavaType().lastIndexOf(".") + 1).replace("DataFormat", "DataFormatAutoConfiguration");
        final String configurationName = name.replace("DataFormatAutoConfiguration", "DataFormatConfiguration");
        final String dataformatName = camelCaseToDash(overrideName != null ? overrideName : model.getName()).toLowerCase(Locale.US);
        final Class<?> configClass = generateDummyClass(packageName + "." + configurationName);
        final JavaClass javaClass = new JavaClass(getProjectClassLoader());

        javaClass.setPackage(packageName);
        javaClass.setName(name);
        javaClass.getJavaDoc().setFullText("Generated by camel-package-maven-plugin - do not edit this file!");
        javaClass.addAnnotation(Configuration.class).setLiteralValue("proxyBeanMethods", "false");
        javaClass.addAnnotation(AutoConfigureAfter.class).setLiteralValue("CamelAutoConfiguration.class");
        javaClass.addAnnotation(Conditional.class).setLiteralValue("ConditionalOnCamelContextAndAutoConfigurationBeans.class");
        javaClass.addAnnotation(EnableConfigurationProperties.class)
                .setLiteralValue("{DataFormatConfigurationProperties.class," + configurationName + ".class}");

        javaClass.addAnnotation("org.apache.camel.spring.boot.util.ConditionalOnHierarchicalProperties")
                .setStringArrayValue(
                        "value",
                        new String[]{"camel.dataformat", "camel.dataformat." + dataformatName});

        javaClass.addImport(ApplicationContext.class);
        javaClass.addImport("org.springframework.boot.convert.ApplicationConversionService");
        javaClass.addImport("org.apache.camel.CamelContext");
        javaClass.addImport("org.apache.camel.spring.boot.CamelAutoConfiguration");
        javaClass.addImport("org.apache.camel.spring.boot.DataFormatConfigurationProperties");
        javaClass.addImport("org.apache.camel.spring.boot.util.CamelPropertiesHelper");
        javaClass.addImport("org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans");
        javaClass.addImport("org.apache.camel.spring.boot.util.HierarchicalPropertiesEvaluator");
        javaClass.addImport("org.apache.camel.spi.DataFormat");
        javaClass.addImport("org.apache.camel.spi.DataFormatCustomizer");
        javaClass.addImport(model.getJavaType());

        javaClass.addField().setPrivate().setName("applicationContext").setType(ApplicationContext.class).addAnnotation(Autowired.class);
        javaClass.addField().setPrivate().setFinal(true).setName("camelContext").setType(loadClass("org.apache.camel.CamelContext"));
        javaClass.addField().setPrivate().setName("configuration").setType(configClass).addAnnotation(Autowired.class);

        Method ctr = javaClass.addMethod().setConstructor(true).setPublic().setName(name).addParameter("org.apache.camel.CamelContext", "camelContext");
        ctr.setBody("this.camelContext = camelContext;\n");

        String body = createDataFormatBody(model.getShortJavaType(), dataformatName);
        String methodName = "configure" + model.getShortJavaType() + "Factory";

        Method method = javaClass.addMethod()
                .setName(methodName)
                .setPublic()
                .setBody(body)
                .setReturnType(loadType("org.apache.camel.spi.DataFormatCustomizer"));

        method.addAnnotation(Lazy.class);
        method.addAnnotation(Bean.class);

        sortImports(javaClass);

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, false);
    }

    private void createDataFormatConverterSource(String packageName, DataFormatModel model)
            throws MojoFailureException {

        final String name = model.getJavaType().substring(model.getJavaType().lastIndexOf(".") + 1).replace("DataFormat", "DataFormatConverter");

        // create converter class and write source
        JavaClass javaClass = new JavaClass(getProjectClassLoader());
        javaClass.setPackage(packageName);
        javaClass.setName(name);
        javaClass.getJavaDoc().setFullText("Generated by camel-package-maven-plugin - do not edit this file!");
        javaClass.addImport("java.util.LinkedHashSet");
        javaClass.addImport("java.util.Set");
        javaClass.addImport("org.apache.camel.CamelContext");
        javaClass.addImport("org.springframework.core.convert.TypeDescriptor");
        javaClass.addImport("org.springframework.core.convert.converter.GenericConverter");

        javaClass.implementInterface("org.springframework.core.convert.converter.GenericConverter");
        javaClass.addField().setPrivate().setFinal(true).setName("camelContext").setType(loadClass("org.apache.camel.CamelContext"));
        javaClass.addMethod().setConstructor(true).setPublic().setPublic().setName(name).addParameter("org.apache.camel.CamelContext", "camelContext")
                .setBody("this.camelContext = camelContext;\n");

        String body = createConverterPairBody(model);
        javaClass.addMethod().setName("getConvertibleTypes").setPublic().setReturnType("Set<ConvertiblePair>")
                .setBody(body);
        body = createConvertBody(model);
        javaClass.addMethod().setName("convert").setPublic().setReturnType("Object")
                .addParameter("Object", "source")
                .addParameter("TypeDescriptor", "sourceType")
                .addParameter("TypeDescriptor", "targetType")
                .setBody(body);
        sortImports(javaClass);

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, false);
    }

    private void createLanguageAutoConfigurationSource(String packageName, LanguageModel model, String overrideName, boolean complexOptions)
            throws MojoFailureException {

        final String name = model.getJavaType().substring(model.getJavaType().lastIndexOf(".") + 1).replace("Language", "LanguageAutoConfiguration");
        final String configurationName = name.replace("LanguageAutoConfiguration", "LanguageConfiguration");
        final String languageName = camelCaseToDash(overrideName != null ? overrideName : model.getName()).toLowerCase(Locale.US);
        final Class<?> configClass = generateDummyClass(packageName + "." + configurationName);
        final JavaClass javaClass = new JavaClass(getProjectClassLoader());

        javaClass.setPackage(packageName);
        javaClass.setName(name);
        javaClass.getJavaDoc().setFullText("Generated by camel-package-maven-plugin - do not edit this file!");
        javaClass.addAnnotation(Configuration.class).setLiteralValue("proxyBeanMethods", "false");
        javaClass.addAnnotation(AutoConfigureAfter.class).setLiteralValue("CamelAutoConfiguration.class");
        javaClass.addAnnotation(Conditional.class).setLiteralValue("ConditionalOnCamelContextAndAutoConfigurationBeans.class");
        javaClass.addAnnotation(EnableConfigurationProperties.class)
                .setLiteralValue("{LanguageConfigurationProperties.class," + configurationName + ".class}");

        javaClass.addAnnotation("org.apache.camel.spring.boot.util.ConditionalOnHierarchicalProperties")
                .setStringArrayValue(
                        "value",
                        new String[]{"camel.language", "camel.language." + languageName});

        javaClass.addImport(ApplicationContext.class);
        javaClass.addImport("org.springframework.boot.convert.ApplicationConversionService");
        javaClass.addImport("org.apache.camel.CamelContext");
        javaClass.addImport("org.apache.camel.spring.boot.CamelAutoConfiguration");
        javaClass.addImport("org.apache.camel.spring.boot.LanguageConfigurationProperties");
        javaClass.addImport("org.apache.camel.spring.boot.util.CamelPropertiesHelper");
        javaClass.addImport("org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans");
        javaClass.addImport("org.apache.camel.spring.boot.util.HierarchicalPropertiesEvaluator");
        javaClass.addImport("org.apache.camel.spi.Language");
        javaClass.addImport("org.apache.camel.spi.LanguageCustomizer");
        javaClass.addImport(model.getJavaType());

        javaClass.addField().setPrivate().setName("applicationContext").setType(ApplicationContext.class).addAnnotation(Autowired.class);
        javaClass.addField().setPrivate().setFinal(true).setName("camelContext").setType(loadClass("org.apache.camel.CamelContext"));
        javaClass.addField().setPrivate().setName("configuration").setType(configClass).addAnnotation(Autowired.class);

        Method ctr = javaClass.addMethod().setConstructor(true).setPublic().setName(name).addParameter("org.apache.camel.CamelContext", "camelContext");
        ctr.setBody("this.camelContext = camelContext;\n");

        String body = createLanguageBody(model.getShortJavaType(), languageName);
        String methodName = "configure" + model.getShortJavaType();

        Method method = javaClass.addMethod()
                .setName(methodName)
                .setPublic()
                .setBody(body)
                .setReturnType("org.apache.camel.spi.LanguageCustomizer");

        method.addAnnotation(Lazy.class);
        method.addAnnotation(Bean.class);

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, false);
    }

    private void createComponentSpringFactorySource(String packageName, ComponentModel model) throws MojoFailureException {
        int pos = model.getJavaType().lastIndexOf(".");
        String name = model.getJavaType().substring(pos + 1);
        name = name.replace("Component", "ComponentAutoConfiguration");

        writeComponentSpringFactorySource(packageName, name);
    }

    private void createLanguageConverterSource(String packageName, LanguageModel model)
            throws MojoFailureException {

        final String name = model.getJavaType().substring(model.getJavaType().lastIndexOf(".") + 1).replace("Language", "LanguageConverter");

        // create converter class and write source
        JavaClass javaClass = new JavaClass(getProjectClassLoader());
        javaClass.setPackage(packageName);
        javaClass.setName(name);
        javaClass.getJavaDoc().setFullText("Generated by camel-package-maven-plugin - do not edit this file!");
        javaClass.addImport("java.util.LinkedHashSet");
        javaClass.addImport("java.util.Set");
        javaClass.addImport("org.apache.camel.CamelContext");
        javaClass.addImport("org.springframework.core.convert.TypeDescriptor");
        javaClass.addImport("org.springframework.core.convert.converter.GenericConverter");

        javaClass.implementInterface("org.springframework.core.convert.converter.GenericConverter");
        javaClass.addField().setPrivate().setFinal(true).setName("camelContext").setType(loadClass("org.apache.camel.CamelContext"));
        javaClass.addMethod().setConstructor(true).setPublic().setName(name).addParameter("org.apache.camel.CamelContext", "camelContext")
                .setBody("this.camelContext = camelContext;\n");

        String body = createConverterPairBody(model);
        javaClass.addMethod().setName("getConvertibleTypes").setPublic().setReturnType("Set<ConvertiblePair>")
                .setBody(body);
        body = createConvertBody(model);
        javaClass.addMethod().setName("convert").setPublic().setReturnType("Object")
                .addParameter("Object", "source")
                .addParameter("TypeDescriptor", "sourceType")
                .addParameter("TypeDescriptor", "targetType")
                .setBody(body);
        sortImports(javaClass);

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, false);
    }

    private void createDataFormatSpringFactorySource(String packageName, DataFormatModel model) throws MojoFailureException {
        int pos = model.getJavaType().lastIndexOf(".");
        String name = model.getJavaType().substring(pos + 1);
        name = name.replace("DataFormat", "DataFormatAutoConfiguration");

        writeComponentSpringFactorySource(packageName, name);
    }

    private void createLanguageSpringFactorySource(String packageName, LanguageModel model) throws MojoFailureException {
        int pos = model.getJavaType().lastIndexOf(".");
        String name = model.getJavaType().substring(pos + 1);
        name = name.replace("Language", "LanguageAutoConfiguration");

        writeComponentSpringFactorySource(packageName, name);
    }

    private static String createComponentBody(String shortJavaType, String name) {
        return new StringBuilder()
                .append("return new ComponentCustomizer() {\n")
                .append("    @Override\n")
                .append("    public void configure(String name, Component target) {\n")
                .append("        CamelPropertiesHelper.copyProperties(camelContext, configuration, target);\n")
                .append("    }\n")
                .append("    @Override\n")
                .append("    public boolean isEnabled(String name, Component target) {\n")
                .append("        return HierarchicalPropertiesEvaluator.evaluate(\n")
                .append("                applicationContext,\n")
                .append("                \"camel.component.customizer\",\n")
                .append("                \"camel.component.").append(name).append(".customizer\")\n")
                .append("            && target instanceof ").append(shortJavaType).append(";\n")
                .append("    }\n")
                .append("};\n")
                .toString();
    }

    private static String createDataFormatBody(String shortJavaType, String name) {
        return new StringBuilder()
                .append("return new DataFormatCustomizer() {\n")
                .append("    @Override\n")
                .append("    public void configure(String name, DataFormat target) {\n")
                .append("        CamelPropertiesHelper.copyProperties(camelContext, configuration, target);\n")
                .append("    }\n")
                .append("    @Override\n")
                .append("    public boolean isEnabled(String name, DataFormat target) {\n")
                .append("        return HierarchicalPropertiesEvaluator.evaluate(\n")
                .append("                applicationContext,\n")
                .append("                \"camel.dataformat.customizer\",\n")
                .append("                \"camel.dataformat.").append(name).append(".customizer\")\n")
                .append("            && target instanceof ").append(shortJavaType).append(";\n")
                .append("    }\n")
                .append("};\n")
                .toString();
    }

    private static String createLanguageBody(String shortJavaType, String name) {
        return new StringBuilder()
                .append("return new LanguageCustomizer() {\n")
                .append("    @Override\n")
                .append("    public void configure(String name, Language target) {\n")
                .append("        CamelPropertiesHelper.copyProperties(camelContext, configuration, target);\n")
                .append("    }\n")
                .append("    @Override\n")
                .append("    public boolean isEnabled(String name, Language target) {\n")
                .append("        return HierarchicalPropertiesEvaluator.evaluate(\n")
                .append("                applicationContext,\n")
                .append("                \"camel.language.customizer\",\n")
                .append("                \"camel.language.").append(name).append(".customizer\")\n")
                .append("            && target instanceof ").append(shortJavaType).append(";\n")
                .append("    }\n")
                .append("};\n")
                .toString();
    }

    private static void sortImports(JavaClass importer) {
        // sort imports
        // do nothing, as imports are sorted automatically when displayed
    }

    private static String getJavaType(ComponentOptionModel option) {
        // type may contain generics so remove those
        String type = option.getJavaType();
        if (type.indexOf('<') != -1) {
            type = type.substring(0, type.indexOf('<'));
        }
        type = type.replace('$', '.');
        return type;
    }

    private static String getJavaType(DataFormatOptionModel option) {
        // type may contain generics so remove those
        String type = option.getJavaType();
        if (type.indexOf('<') != -1) {
            type = type.substring(0, type.indexOf('<'));
        }
        type = type.replace('$', '.');
        return type;
    }

    private static String getJavaType(LanguageOptionModel option) {
        // type may contain generics so remove those
        String type = option.getJavaType();
        if (type.indexOf('<') != -1) {
            type = type.substring(0, type.indexOf('<'));
        }
        type = type.replace('$', '.');
        return type;
    }

    private void findComponentNames(Set<String> componentNames) {
        componentJar.stream()
                .filter(je -> !je.isDirectory())
                .map(ZipEntry::getName)
                .filter(s -> s.startsWith("META-INF/services/org/apache/camel/component/"))
                .map(s -> s.substring("META-INF/services/org/apache/camel/component/".length()))
                .filter(s -> !s.startsWith(".") && !s.contains("/"))
                .forEach(componentNames::add);
    }

    private List<String> findDataFormatNames() {
        return componentJar.stream()
                .filter(je -> !je.isDirectory())
                .map(ZipEntry::getName)
                .filter(s -> s.startsWith("META-INF/services/org/apache/camel/dataformat/"))
                .map(s -> s.substring("META-INF/services/org/apache/camel/dataformat/".length()))
                .filter(s -> !s.startsWith(".") && !s.contains("/"))
                .collect(Collectors.toList());
    }

    private List<String> findLanguageNames() {
        return componentJar.stream()
                .filter(je -> !je.isDirectory())
                .map(ZipEntry::getName)
                .filter(s -> s.startsWith("META-INF/services/org/apache/camel/language/"))
                .map(s -> s.substring("META-INF/services/org/apache/camel/language/".length()))
                .filter(s -> !s.startsWith(".") && !s.contains("/"))
                .collect(Collectors.toList());
    }

    private void writeSourceIfChanged(JavaClass source, String fileName, boolean innerClassesLast) throws MojoFailureException {
        writeSourceIfChanged(source.printClass(innerClassesLast), fileName);
    }

    private void writeSourceIfChanged(String source, String fileName) throws MojoFailureException {

        File target = new File(new File(baseDir, "src/main/java"), fileName);

        deleteFileOnMainArtifact(target);

        try {
            String header;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("license-header-java.txt")) {
                header = PackageHelper.loadText(is);
            }
            String code = header + "\n" + source;
            if (getLog().isDebugEnabled()) {
                getLog().debug("Source code generated:\n" + code);
            }

            AbstractGeneratorMojo.updateResource(null, target.toPath(), code);
        } catch (Exception e) {
            throw new MojoFailureException("IOError with file " + target, e);
        }
    }

    private void writeComponentSpringFactorySource(String packageName, String name) throws MojoFailureException {
        StringBuilder sb = new StringBuilder();
        sb.append("org.springframework.boot.autoconfigure.EnableAutoConfiguration=\\\n");

        String lineToAdd = packageName + "." + name + "\n";
        sb.append(lineToAdd);

        String fileName = "META-INF/spring.factories";
        File target = new File(new File(baseDir, "src/main/resources"), fileName);

        deleteFileOnMainArtifact(target);

        if (target.exists()) {
            try {
                // is the auto configuration already in the file
                boolean found = false;
                List<String> lines = FileUtils.readLines(target, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.contains(name)) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    getLog().debug("No changes to existing file: " + target);
                } else {
                    // find last non empty line, so we can add our new line
                    // after that
                    int lastLine = 0;
                    for (int i = lines.size() - 1; i >= 0; i--) {
                        String line = lines.get(i);
                        if (!line.trim().isEmpty()) {
                            // adjust existing line so its being continued
                            line = line + ",\\";
                            lines.set(i, line);
                            lastLine = i;
                            break;
                        }
                    }
                    lines.add(lastLine + 1, lineToAdd);

                    StringBuilder code = new StringBuilder();
                    for (String line : lines) {
                        code.append(line).append("\n");
                    }

                    // update
                    FileUtils.write(target, code.toString(), StandardCharsets.UTF_8, false);
                    getLog().info("Updated existing file: " + target);
                }
            } catch (Exception e) {
                throw new MojoFailureException("IOError with file " + target, e);
            }
        } else {
            // create new file
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("license-header.txt");
                String header = PackageHelper.loadText(is);
                String code = sb.toString();
                // add empty new line after header
                code = header + "\n" + code;
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Source code generated:\n" + code);
                }

                FileUtils.write(target, code, StandardCharsets.UTF_8);
                getLog().info("Created file: " + target);
            } catch (Exception e) {
                throw new MojoFailureException("IOError with file " + target, e);
            }
        }
    }

    private String getStarterArtifactId() {
        if ("camel-core-engine".equals(project.getArtifactId())) {
            return "camel-core";
        } else {
            return project.getArtifactId();
        }
    }

    private void deleteFileOnMainArtifact(File starterFile) {
        if (!DELETE_FILES_ON_MAIN_ARTIFACTS) {
            return;
        }

        String relativePath = baseDir.toPath().relativize(starterFile.toPath()).toString();
        File mainArtifactFile = new File(baseDir, relativePath);
        if (mainArtifactFile.exists()) {
            boolean deleted = mainArtifactFile.delete();
            if (!deleted) {
                throw new IllegalStateException("Cannot delete file " + mainArtifactFile);
            }
        }
    }

}
