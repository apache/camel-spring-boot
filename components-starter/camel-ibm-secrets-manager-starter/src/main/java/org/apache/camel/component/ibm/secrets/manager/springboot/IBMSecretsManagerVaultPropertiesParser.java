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
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.AbstractEarlyResolutionPropertiesParser;
import org.apache.camel.util.ObjectHelper;
import org.springframework.core.env.ConfigurableEnvironment;

public class IBMSecretsManagerVaultPropertiesParser extends AbstractEarlyResolutionPropertiesParser {

    @Override
    protected String getEarlyResolutionProperty() {
        return "camel.component.ibm-secrets-manager.early-resolve-properties";
    }

    @Override
    protected String getOverridePropertySourceName() {
        return "overridden-ibm-secrets-manager-properties";
    }

    @Override
    protected PropertiesFunction createPropertiesFunction(ConfigurableEnvironment environment) {
        SecretsManager client;
        String token = environment.getProperty("camel.vault.ibm.token");
        String serviceUrl = environment.getProperty("camel.vault.ibm.serviceUrl");
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
        return new IBMSecretsManagerPropertiesFunction(client);
    }
}
