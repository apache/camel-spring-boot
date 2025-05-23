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
package org.apache.camel.component.ibm.secrets.manager.springboot;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.secrets_manager_sdk.secrets_manager.v2.SecretsManager;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.ibm.secrets.manager.IBMSecretsManagerPropertiesFunction;
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

import java.util.Properties;

public class IBMSecretsManagerVaultPropertiesParser implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(IBMSecretsManagerVaultPropertiesParser.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        SecretsManager client;
        ConfigurableEnvironment environment = event.getEnvironment();
        String token;
        String serviceUrl;
        if (Boolean.parseBoolean(environment.getProperty("camel.component.ibm-secrets-manager.early-resolve-properties"))) {
            token = environment.getProperty("camel.vault.ibm.token");
            serviceUrl = environment.getProperty("camel.vault.ibm.serviceUrl");
            if (ObjectHelper.isNotEmpty(token) && ObjectHelper.isNotEmpty(serviceUrl)) {
                IamAuthenticator iamAuthenticator = new IamAuthenticator.Builder()
                        .apikey(token)
                        .build();
                client = new SecretsManager("Camel Secrets Manager Service for Properties", iamAuthenticator);
                client.setServiceUrl(serviceUrl);
            } else {
                throw new RuntimeCamelException(
                        "Using the IBM Secrets Manager Properties Function requires setting IBM Credentials and service url as application properties or environment variables");
            }
            IBMSecretsManagerPropertiesFunction secretsManagerPropertiesFunction = new IBMSecretsManagerPropertiesFunction(client);
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
                                stringValue.startsWith("{{ibm:") &&
                                stringValue.endsWith("}}")) {
                            LOG.debug("decrypting and overriding property {}", key);
                            try {
                                String element = secretsManagerPropertiesFunction.apply(stringValue
                                        .replace("{{ibm:", "")
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
            environment.getPropertySources().addFirst(new PropertiesPropertySource("overridden-ibm-secrets-manager-properties", props));
        }
    }
}