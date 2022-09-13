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

@ConfigurationProperties(prefix = "camel.vault.gcp")
public class GcpVaultConfigurationProperties {

    /**
     * The Service Account Key location
     */
    private String serviceAccountKey;

    /**
     * The GCP Project ID
     */
    private String projectId;

    /**
     * Define if we want to use the GCP Client Default Instance or not
     */
    private boolean useDefaultInstance;
    
    /**
     * Define the Google Pubsub subscription Name to be used when checking for updates
     */
    private String subscriptionName;
    
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

    public String getServiceAccountKey() {
        return serviceAccountKey;
    }

    public void setServiceAccountKey(String serviceAccountKey) {
        this.serviceAccountKey = serviceAccountKey;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public boolean isUseDefaultInstance() {
        return useDefaultInstance;
    }

    public void setUseDefaultInstance(boolean useDefaultInstance) {
        this.useDefaultInstance = useDefaultInstance;
    }

	public String getSubscriptionName() {
		return subscriptionName;
	}

	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
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
}
