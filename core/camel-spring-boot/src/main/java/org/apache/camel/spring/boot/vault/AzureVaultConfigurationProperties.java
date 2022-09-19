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

import org.apache.camel.spi.Metadata;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.vault.azure")
public class AzureVaultConfigurationProperties {

    /**
     * The Vault Name
     */
    private String vaultName;

    /**
     * The Client Id
     */
    private String clientId;

    /**
     * The Client secret
     */
    private String clientSecret;

    /**
     * The tenant Id
     */
    private String tenantId;
    
    /**
     * Whether to automatically reload Camel upon secrets being updated in Azure.
     */
    private boolean refreshEnabled;
    
    /**
     * The period (millis) between checking Azure for updated secrets.
     */
    private long refreshPeriod = 30000;
    
    /**
     * Specify the secret names (or pattern) to check for updates. Multiple secrets can be separated by comma.
     */
    private String secrets;
    
    /**
     * The Eventhubs connection String for Key Vault Secret events notifications
     */
    private String eventhubConnectionString;

    /**
     * The Eventhubs Blob Access Key for CheckpointStore purpose
     */
    private String blobAccessKey;

    /**
     * The Eventhubs Blob Account Name for CheckpointStore purpose
     */
    private String blobAccountName;

    /**
     * The Eventhubs Blob Container Name for CheckpointStore purpose
     */
    private String blobContainerName;

    public String getVaultName() {
        return vaultName;
    }

    public void setVaultName(String vaultName) {
        this.vaultName = vaultName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getEventhubConnectionString() {
        return eventhubConnectionString;
    }

    public void setEventhubConnectionString(String eventhubConnectionString) {
        this.eventhubConnectionString = eventhubConnectionString;
    }

    public String getBlobAccessKey() {
        return blobAccessKey;
    }

    public void setBlobAccessKey(String blobAccessKey) {
        this.blobAccessKey = blobAccessKey;
    }

    public String getBlobAccountName() {
        return blobAccountName;
    }

    public void setBlobAccountName(String blobAccountName) {
        this.blobAccountName = blobAccountName;
    }

    public String getBlobContainerName() {
        return blobContainerName;
    }

    public void setBlobContainerName(String blobContainerName) {
        this.blobContainerName = blobContainerName;
    }
}
