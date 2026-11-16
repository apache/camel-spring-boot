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
     * service account token of the pod), jwt (a JWT, such as a workload identity token), cert (the TLS client
     * certificate of the keyStore), aws-iam (the AWS credentials of the application), azure-msi (the Azure managed
     * identity) or gcp-gce (the service account of the Google Compute Engine instance). Camel renews the token before
     * it expires, and logs in again when the token can no longer be renewed.
     */
    @Metadata(defaultValue = "token", enums = "token,approle,kubernetes,jwt,cert,aws-iam,azure-msi,gcp-gce")
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
     * The hashicorp vault role to log in as, for the kubernetes, jwt, cert, aws-iam, azure-msi and gcp-gce logins. The
     * jwt login can leave it out when the login method has a default role, the aws-iam login to log in as the role
     * named as the IAM principal, and the cert login to let Vault pick the certificate role that trusts the
     * certificate.
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
     * the login method: approle, kubernetes, jwt, cert, aws (for aws-iam), azure (for azure-msi) or gcp (for gcp-gce)
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
     * certificate and its unencrypted private key, in PKCS#8 or, for an RSA key, in PKCS#1.
     */
    private String keyStore;

    /**
     * The password of the key store and of its private key, when it is a PKCS12 or JKS key store
     */
    private String keyStorePassword;

    /**
     * The value of the X-Vault-AWS-IAM-Server-ID header of the aws-iam login, when the AWS login of hashicorp vault
     * requires one (iam_server_id_header_value)
     */
    private String awsIamServerId;

    /**
     * The AWS region the aws-iam login signs its request for, which must be the region of awsStsEndpoint. When not set:
     * us-east-1 with the global endpoint, else the region of the AWS SDK default region chain (such as the AWS_REGION
     * environment variable).
     */
    private String awsRegion;

    /**
     * The AWS STS endpoint of the aws-iam login, which the AWS login of hashicorp vault must accept (its sts_endpoint).
     * When not set, the global endpoint https://sts.amazonaws.com/.
     */
    private String awsStsEndpoint;

    /**
     * The resource (audience) of the managed identity token of the azure-msi login, which must be the resource of the
     * Azure login of hashicorp vault
     */
    private String azureResource = "https://management.azure.com/";

    /**
     * The client ID of a user-assigned managed identity, for the azure-msi login. When not set, the system-assigned
     * managed identity.
     */
    private String azureClientId;

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

    public String getAwsIamServerId() {
        return awsIamServerId;
    }

    public void setAwsIamServerId(String awsIamServerId) {
        this.awsIamServerId = awsIamServerId;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getAwsStsEndpoint() {
        return awsStsEndpoint;
    }

    public void setAwsStsEndpoint(String awsStsEndpoint) {
        this.awsStsEndpoint = awsStsEndpoint;
    }

    public String getAzureResource() {
        return azureResource;
    }

    public void setAzureResource(String azureResource) {
        this.azureResource = azureResource;
    }

    public String getAzureClientId() {
        return azureClientId;
    }

    public void setAzureClientId(String azureClientId) {
        this.azureClientId = azureClientId;
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
