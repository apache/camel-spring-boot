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
package org.apache.camel.spring.boot.k;

import org.apache.camel.component.kubernetes.properties.ConfigMapPropertiesFunction;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Properties;

/**
 * Inject Camel K's properties to Spring Boot Environment
 */
public class ApplicationEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        configurePropertySources(environment, application);
    }

    private void configurePropertySources(ConfigurableEnvironment environment, SpringApplication application) {

        Properties sysProperties = new Properties();

        // explicit disable looking up configmap and secret using the KubernetesClient
        sysProperties.put(ConfigMapPropertiesFunction.CLIENT_ENABLED, "false");

        environment.getPropertySources().addLast(
                new PropertiesPropertySource("camel-k-sys", sysProperties));
        environment.getPropertySources().addLast(
                new PropertiesPropertySource("camel-k-app", loadApplicationProperties()));
        environment.getPropertySources().addLast(
                new PropertiesPropertySource("camel-k-usr-configmap", loadConfigMapUserProperties()));
        environment.getPropertySources().addLast(
                new PropertiesPropertySource("camel-k-usr-secrets", loadSecretsProperties()));
        environment.getPropertySources().addLast(
                new PropertiesPropertySource("camel-k-servicebindings", loadServiceBindingsProperties()));

    }

    // ******************************************
    //
    // Helpers
    //
    // ******************************************

    private static Properties loadApplicationProperties() {
        final String conf = System.getProperty(ApplicationConstants.PROPERTY_CAMEL_K_CONF,
                System.getenv(ApplicationConstants.ENV_CAMEL_K_CONF));

        final Properties properties = new Properties();

        if (ObjectHelper.isEmpty(conf)) {
            return properties;
        }


        try {
            Path confPath = Paths.get(conf);
            if (Files.exists(confPath) && !Files.isDirectory(confPath)) {
                try (Reader reader = Files.newBufferedReader(confPath)) {
                    properties.load(reader);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return properties;
    }

    private static Properties loadConfigMapUserProperties() {
        return loadUserProperties(
                ApplicationConstants.PROPERTY_CAMEL_K_MOUNT_PATH_CONFIGMAPS,
                ApplicationConstants.ENV_CAMEL_K_MOUNT_PATH_CONFIGMAPS,
                ApplicationConstants.PATH_CONFIGMAPS);
    }

    private static Properties loadSecretsProperties() {
        return loadUserProperties(
                ApplicationConstants.PROPERTY_CAMEL_K_MOUNT_PATH_SECRETS,
                ApplicationConstants.ENV_CAMEL_K_MOUNT_PATH_SECRETS,
                ApplicationConstants.PATH_SECRETS);
    }

    private static Properties loadServiceBindingsProperties() {
        return loadUserProperties(
                ApplicationConstants.PROPERTY_CAMEL_K_MOUNT_PATH_SERVICEBINDINGS,
                ApplicationConstants.ENV_CAMEL_K_MOUNT_PATH_SERVICEBINDINGS,
                ApplicationConstants.PATH_SERVICEBINDINGS);
    }

    private static Properties loadUserProperties(String property, String env, String subpath) {
        String path = System.getProperty(property, System.getenv(env));

        if (path == null) {
            String conf = System.getProperty(
                    ApplicationConstants.PROPERTY_CAMEL_K_CONF_D,
                    System.getenv(ApplicationConstants.ENV_CAMEL_K_CONF_D));

            if (conf != null) {
                if (!conf.endsWith("/")) {
                    conf = conf + "/";
                }

                path = conf + subpath;
            }
        }

        final Properties properties = new Properties();

        if (ObjectHelper.isEmpty(path)) {
            return properties;
        }

        final Path root = Paths.get(path);

        if (Files.exists(root)) {
            FileVisitor<Path> visitor = propertiesCollector(properties);
            try {
                Files.walkFileTree(root, visitor);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return properties;
    }

    private static FileVisitor<Path> propertiesCollector(Properties properties) {
        return new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Objects.requireNonNull(file);
                Objects.requireNonNull(attrs);

                if (Files.isDirectory(file) || Files.isSymbolicLink(file)) {
                    return FileVisitResult.CONTINUE;
                }

                if (file.toFile().getAbsolutePath().endsWith(".properties")) {
                    try (Reader reader = Files.newBufferedReader(file)) {
                        Properties p = new Properties();
                        p.load(reader);
                        p.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
                    }
                } else {
                    try {
                        properties.put(
                                file.getFileName().toString(),
                                Files.readString(file, StandardCharsets.UTF_8));
                    } catch (MalformedInputException mie) {
                        // Just skip if it is not a UTF-8 encoded file (ie a binary)
                        LOGGER.info("Cannot transform {} into UTF-8 text, skipping.", file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        };
    }
}
