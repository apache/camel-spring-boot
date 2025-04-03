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

@ConfigurationProperties(prefix = "camel.vault.ibm")
public class IBMVaultConfigurationProperties {

    /**
     * The IBM Secrets Manager Token
     */
    private String token;

    /**
     * The IBM Secrets Manager Service URL
     */
    private String serviceUrl;

    /**
     * Specify the topic name for consuming notification on IBM Event Stream
     */
    private String eventStreamTopic;

    /**
     * Specify the Bootstrap servers for consuming notification on IBM Event Stream. Multiple servers can be separated
     * by comma.
     */
    private String eventStreamBootstrapServers;

    /**
     * Specify the username to access IBM Event Stream
     */
    private String eventStreamUsername;

    /**
     * Specify the password to access IBM Event Stream
     */
    private String eventStreamPassword;

    /**
     * Specify the Consumer Group ID to access IBM Event Stream
     */
    private String eventStreamGroupId;

    /**
     * Specify the Consumer Poll Timeout while consuming from IBM Event Stream Topic
     */
    private long eventStreamConsumerPollTimeout = 3000;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getEventStreamTopic() {
        return eventStreamTopic;
    }

    public void setEventStreamTopic(String eventStreamTopic) {
        this.eventStreamTopic = eventStreamTopic;
    }

    public String getEventStreamBootstrapServers() {
        return eventStreamBootstrapServers;
    }

    public void setEventStreamBootstrapServers(String eventStreamBootstrapServers) {
        this.eventStreamBootstrapServers = eventStreamBootstrapServers;
    }

    public String getEventStreamUsername() {
        return eventStreamUsername;
    }

    public void setEventStreamUsername(String eventStreamUsername) {
        this.eventStreamUsername = eventStreamUsername;
    }

    public String getEventStreamPassword() {
        return eventStreamPassword;
    }

    public void setEventStreamPassword(String eventStreamPassword) {
        this.eventStreamPassword = eventStreamPassword;
    }

    public String getEventStreamGroupId() {
        return eventStreamGroupId;
    }

    public void setEventStreamGroupId(String eventStreamGroupId) {
        this.eventStreamGroupId = eventStreamGroupId;
    }

    public long getEventStreamConsumerPollTimeout() {
        return eventStreamConsumerPollTimeout;
    }

    public void setEventStreamConsumerPollTimeout(long eventStreamConsumerPollTimeout) {
        this.eventStreamConsumerPollTimeout = eventStreamConsumerPollTimeout;
    }
}
