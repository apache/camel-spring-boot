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
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.AbstractEarlyResolutionPropertiesParser;
import org.apache.camel.util.ObjectHelper;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;

public class SpringBootGoogleSecretManagerPropertiesParser extends AbstractEarlyResolutionPropertiesParser {

    @Override
    protected String getEarlyResolutionProperty() {
        return "camel.component.google-secret-manager.early-resolve-properties";
    }

    @Override
    protected String getOverridePropertySourceName() {
        return "overridden-camel-google-secret-manager-properties";
    }

    @Override
    protected PropertiesFunction createPropertiesFunction(ConfigurableEnvironment environment) {
        SecretManagerServiceClient client;
        String projectId = environment.getProperty("camel.vault.gcp.projectId");
        boolean useDefaultInstance
                = Boolean.parseBoolean(environment.getProperty("camel.vault.gcp.useDefaultInstance"));
        if (useDefaultInstance && ObjectHelper.isNotEmpty(projectId)) {
            try {
                SecretManagerServiceSettings settings = SecretManagerServiceSettings.newBuilder().build();
                client = SecretManagerServiceClient.create(settings);
            } catch (IOException e) {
                throw new RuntimeCamelException(e);
            }
        } else {
            throw new RuntimeCamelException(
                    "Using the GCP Secret Manager Properties Function in Spring Boot early resolver mode requires setting GCP project Id as application properties and use default instance option to true");
        }
        return new GoogleSecretManagerPropertiesFunction(client, projectId);
    }
}
