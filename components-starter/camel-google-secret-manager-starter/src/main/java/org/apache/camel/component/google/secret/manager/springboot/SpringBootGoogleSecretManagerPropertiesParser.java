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
package org.apache.camel.component.google.secret.manager.springboot;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.google.secret.manager.GoogleSecretManagerPropertiesFunction;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.util.Properties;

public class SpringBootGoogleSecretManagerPropertiesParser implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootGoogleSecretManagerPropertiesParser.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        SecretManagerServiceClient client;
        ConfigurableEnvironment environment = event.getEnvironment();
        String projectId;
        if (Boolean.parseBoolean(environment.getProperty("camel.component.google-secret-manager.early-resolve-properties"))) {
            projectId = environment.getProperty("camel.vault.gcp.projectId");
            boolean useDefaultInstance = Boolean.parseBoolean(environment.getProperty("camel.vault.gcp.useDefaultInstance"));
            if (useDefaultInstance && ObjectHelper.isNotEmpty(projectId)) {
                SecretManagerServiceSettings settings = null;
                try {
                    settings = SecretManagerServiceSettings.newBuilder().build();
                    client = SecretManagerServiceClient.create(settings);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeCamelException(
                        "Using the GCP Secret Manager Properties Function in Spring Boot early resolver mode requires setting GCP project Id as application properties and use default instance option to true");
            }
            GoogleSecretManagerPropertiesFunction secretsManagerPropertiesFunction = new GoogleSecretManagerPropertiesFunction(client, projectId);
            final Properties props = new Properties();
            for (PropertySource mutablePropertySources : event.getEnvironment().getPropertySources()) {
                if (mutablePropertySources instanceof MapPropertySource mapPropertySource) {
                    mapPropertySource.getSource().forEach((key, value) -> {
                        String stringValue = null;
                        if ((value instanceof OriginTrackedValue originTrackedValue &&
                                originTrackedValue.getValue() instanceof String v)) {
                            stringValue = v;
                        } else if (value instanceof String v) {
                            stringValue = v;
                        }
                        if (stringValue != null &&
                                stringValue.startsWith("{{gcp:") &&
                                stringValue.endsWith("}}")) {
                            LOG.debug("decrypting and overriding property {}", key);
                            try {
                                String element = secretsManagerPropertiesFunction.apply(stringValue
                                        .replace("{{gcp:", "")
                                        .replace("}}", ""));
                                props.put(key, element);
                            } catch (Exception e) {
                                // Log and do nothing
                                LOG.debug("failed to parse property {}. This exception is ignored.", key, e);
                            }
                        }
                    });
                }
            }
            environment.getPropertySources().addFirst(new PropertiesPropertySource("overridden-camel-google-secret-manager-properties", props));
        }
    }
}
