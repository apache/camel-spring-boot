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
package org.apache.camel.spring.boot.vault;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.vault.aws")
public class AwsVaultConfigurationProperties {

    /**
     * The AWS access key
     */
    private String accessKey;

    /**
     * The AWS secret key
     */
    private String secretKey;

    /**
     * The AWS region
     */
    private String region;

    /**
     * Define if we want to use the AWS Default Credentials Provider or not
     */
    private boolean defaultCredentialsProvider;

    /**
     * Define if we want to use the AWS Profile Credentials Provider or not
     */
    private boolean profileCredentialsProvider;

    /**
     * Define the profile name in case we are using profile credentials provider
     */
    private String profileName;

    /**
     * Define if we want to refresh the secrets on update
     */
    private boolean refreshEnabled;

    /**
     * Define the refresh period
     */
    private long refreshPeriod = 30000;

    /**
     * Define the secrets to look at
     */
    private String secrets;
    
    /**
     * Whether to use AWS SQS for secrets updates notification, this will require setting up Eventbridge/Cloudtrail/SQS
     * communication
     */
    private boolean useSqsNotification;

    /**
     * In case of usage of SQS notification this field will specified the Queue URL to use
     */
    private String sqsQueueUrl;

    /**
     * Set the need for overriding the endpoint. This option needs to be used in combination with the
     * uriEndpointOverride option
     */
    private boolean overrideEndpoint;

    /**
     * Set the overriding uri endpoint. This option needs to be used in combination with overrideEndpoint option
     */
    private String uriEndpointOverride;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isDefaultCredentialsProvider() {
        return defaultCredentialsProvider;
    }

    public void setDefaultCredentialsProvider(boolean defaultCredentialsProvider) {
        this.defaultCredentialsProvider = defaultCredentialsProvider;
    }

    public boolean isProfileCredentialsProvider() {
        return profileCredentialsProvider;
    }

    public void setProfileCredentialsProvider(boolean profileCredentialsProvider) {
        this.profileCredentialsProvider = profileCredentialsProvider;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    public void setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
    }

    public long getRefreshPeriod() {
        return refreshPeriod;
    }

    public void setRefreshPeriod(long refreshPeriod) {
        this.refreshPeriod = refreshPeriod;
    }

    public String getSecrets() {
        return secrets;
    }

    public void setSecrets(String secrets) {
        this.secrets = secrets;
    }
    
   public boolean isUseSqsNotification() {
        return useSqsNotification;
    }

    public void setUseSqsNotification(boolean useSqsNotification) {
        this.useSqsNotification = useSqsNotification;
    }

    public String getSqsQueueUrl() {
        return sqsQueueUrl;
    }

    public void setSqsQueueUrl(String sqsQueueUrl) {
        this.sqsQueueUrl = sqsQueueUrl;
    }

    public boolean isOverrideEndpoint() {
        return overrideEndpoint;
    }

    public void setOverrideEndpoint(boolean overrideEndpoint) {
        this.overrideEndpoint = overrideEndpoint;
    }

    public String getUriEndpointOverride() {
        return uriEndpointOverride;
    }

    public void setUriEndpointOverride(String uriEndpointOverride) {
        this.uriEndpointOverride = uriEndpointOverride;
    }
}
