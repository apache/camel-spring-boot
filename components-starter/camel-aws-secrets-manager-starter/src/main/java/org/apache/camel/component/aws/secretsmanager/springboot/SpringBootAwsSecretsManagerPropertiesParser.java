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
package org.apache.camel.component.aws.secretsmanager.springboot;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerPropertiesFunction;
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.AbstractEarlyResolutionPropertiesParser;
import org.apache.camel.util.ObjectHelper;
import org.springframework.core.env.ConfigurableEnvironment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

public class SpringBootAwsSecretsManagerPropertiesParser extends AbstractEarlyResolutionPropertiesParser {

    @Override
    protected String getEarlyResolutionProperty() {
        return "camel.component.aws-secrets-manager.early-resolve-properties";
    }

    @Override
    protected String getOverridePropertySourceName() {
        return "overridden-camel-aws-secrets-manager-properties";
    }

    @Override
    protected PropertiesFunction createPropertiesFunction(ConfigurableEnvironment environment) {
        SecretsManagerClient client;
        String accessKey = environment.getProperty("camel.vault.aws.accessKey");
        String secretKey = environment.getProperty("camel.vault.aws.secretKey");
        String region = environment.getProperty("camel.vault.aws.region");
        boolean useDefaultCredentialsProvider
                = Boolean.parseBoolean(environment.getProperty("camel.vault.aws.defaultCredentialsProvider"));
        boolean useProfileCredentialsProvider
                = Boolean.parseBoolean(environment.getProperty("camel.vault.aws.profileCredentialsProvider"));
        String profileName = environment.getProperty("camel.vault.aws.profileName");
        if (ObjectHelper.isNotEmpty(accessKey) && ObjectHelper.isNotEmpty(secretKey)
                && ObjectHelper.isNotEmpty(region)) {
            SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder();
            AwsBasicCredentials cred = AwsBasicCredentials.create(accessKey, secretKey);
            clientBuilder = clientBuilder.credentialsProvider(StaticCredentialsProvider.create(cred));
            clientBuilder.region(Region.of(region));
            client = clientBuilder.build();
        } else if (useDefaultCredentialsProvider && ObjectHelper.isNotEmpty(region)) {
            SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder();
            clientBuilder.region(Region.of(region));
            client = clientBuilder.build();
        } else if (useProfileCredentialsProvider && ObjectHelper.isNotEmpty(profileName)) {
            SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder();
            clientBuilder.credentialsProvider(ProfileCredentialsProvider.create(profileName));
            clientBuilder.region(Region.of(region));
            client = clientBuilder.build();
        } else {
            throw new RuntimeCamelException(
                    "Using the AWS Secrets Manager Properties Function requires setting AWS credentials as application properties or environment variables");
        }
        return new SecretsManagerPropertiesFunction(client);
    }
}
