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

@ConfigurationProperties(prefix = "camel.vault.cyberark")
public class CyberArkVaultConfigurationProperties {

    /**
     * The CyberArk Conjur instance URL
     */
    private String url;

    /**
     * The CyberArk Conjur account name
     */
    private String account;

    /**
     * The username for authentication
     */
    private String username;

    /**
     * The password for authentication
     */
    private String password;

    /**
     * The API key for authentication
     */
    private String apiKey;

    /**
     * Pre-authenticated token to use
     */
    private String authToken;

    /**
     * Whether to verify SSL certificates
     */
    private boolean verifySsl = true;

    /**
     * Path to the SSL certificate for verification
     */
    private String certificatePath;

    /**
     * Specify the secret names (or pattern) to check for updates. Multiple secrets can be separated by comma.
     */
    private String secrets;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public boolean isVerifySsl() {
        return verifySsl;
    }

    public void setVerifySsl(boolean verifySsl) {
        this.verifySsl = verifySsl;
    }

    public String getCertificatePath() {
        return certificatePath;
    }

    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }

    public String getSecrets() {
        return secrets;
    }

    public void setSecrets(String secrets) {
        this.secrets = secrets;
    }
}
