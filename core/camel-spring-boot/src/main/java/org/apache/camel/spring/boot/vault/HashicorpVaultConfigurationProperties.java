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

@ConfigurationProperties(prefix = "camel.vault.hashicorp")
public class HashicorpVaultConfigurationProperties {

    /**
     * How to log in to hashicorp vault: token (a fixed token), approle (a role ID and a secret ID), kubernetes (the
     * service account token of the pod), jwt (a JWT, such as a workload identity token) or cert (the TLS client
     * certificate of the keyStore). Camel renews the token before it expires, and logs in again when the token can no
     * longer be renewed.
     */
    @Metadata(defaultValue = "token", enums = "token,approle,kubernetes,jwt,cert")
    private String authentication = "token";

    /**
     * The Hashicorp Vault Token for accessing the service
     */
    private String token;

    /**
     * The role ID of the AppRole, for the approle login
     */
    private String roleId;

    /**
     * The secret ID of the AppRole, for the approle login
     */
    private String secretId;

    /**
     * The hashicorp vault role to log in as, for the kubernetes and jwt logins. The jwt login can leave it out when the
     * login method has a default role.
     */
    private String role;

    /**
     * The JWT to log in with, for the jwt and kubernetes logins. For a JWT that is replaced over time, such as a
     * projected service account token, use jwtFile instead.
     */
    private String jwt;

    /**
     * The file to read the JWT from at every login, for the jwt and kubernetes logins. The kubernetes login reads
     * /var/run/secrets/kubernetes.io/serviceaccount/token when neither jwt nor jwtFile is set.
     */
    private String jwtFile;

    /**
     * The path the login method is mounted at in hashicorp vault (without auth/), when it is not the default path of
     * the login method: approle, kubernetes or jwt
     */
    private String authMountPath;

    /**
     * The file with the certificates to trust for the TLS connection to hashicorp vault, such as the CA certificate of
     * a private CA: a PKCS12 or JKS trust store, or PEM certificates. When not set, the JVM trusts the certificate as
     * for any other TLS connection.
     */
    private String trustStore;

    /**
     * The password of the trust store, when it is a PKCS12 or JKS trust store
     */
    private String trustStorePassword;

    /**
     * The file with the TLS client certificate and its private key, which Camel presents to hashicorp vault: for the
     * cert login, or for a Vault that requires client certificates. A PKCS12 or JKS key store, or PEM with the
     * certificate and the PKCS#8 private key.
     */
    private String keyStore;

    /**
     * The password of the key store and of its private key, when it is a PKCS12 or JKS key store
     */
    private String keyStorePassword;

    /**
     * The Hashicorp Vault Host for accessing the service
     */
    private String host;

    /**
     * The Hashicorp Vault port for accessing the service
     */
    private String port;

    /**
     * The Hashicorp Vault Scheme for accessing the service
     */
    private String scheme;

    /**
     * Determine if the Hashicorp Vault is deployed on Hashicorp Cloud or not
     */
    private boolean cloud;

    /**
     * If the Hashicorp Vault instance is deployed on Hashicorp Cloud, this field will determine the namespace
     */
    private String namespace;

    /**
     * Whether to automatically reload Camel upon secrets being updated in Hashicorp Vault
     */
    private boolean refreshEnabled;

    /**
     * The period (millis) between checking Hashicorp Vault for updated secrets
     */
    private long refreshPeriod = 60000;

    /**
     * Specify the secret names (or pattern) to check for updates. Multiple secrets can be separated by comma
     */
    private String secrets;

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public String getJwtFile() {
        return jwtFile;
    }

    public void setJwtFile(String jwtFile) {
        this.jwtFile = jwtFile;
    }

    public String getAuthMountPath() {
        return authMountPath;
    }

    public void setAuthMountPath(String authMountPath) {
        this.authMountPath = authMountPath;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public boolean isCloud() {
        return cloud;
    }

    public void setCloud(boolean cloud) {
        this.cloud = cloud;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
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
