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
package org.apache.camel.spring.boot.security;

import org.apache.camel.support.jsse.CipherSuitesParameters;
import org.apache.camel.support.jsse.FilterParameters;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.SSLContextClientParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.SSLContextServerParameters;
import org.apache.camel.support.jsse.SecureRandomParameters;
import org.apache.camel.support.jsse.SecureSocketProtocolsParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.ssl")
public class CamelSSLConfigurationProperties {

    // These option must be copied from camel-core SSLContextParameters to
    // allow spring boot to include them in its spring-configuration-metadata.json file

    /**
     * Global Camel security configuration.
     */
    private SSLContextParameters config = new SSLContextParameters();

    /**
     * The optional key manager configuration for creating the KeyManager used in constructing an SSLContext.
     */
    private KeyManagersParameters keyManagers;

    /**
     * The optional trust manager configuration for creating the TrustManager used in constructing an SSLContext.
     */
    private TrustManagersParameters trustManagers;

    /**
     * The optional secure random configuration options to use for constructing the SecureRandom used in the creation of
     * an SSLContext.
     */
    private SecureRandomParameters secureRandom;

    /**
     * The optional configuration options to be applied purely to the client side settings of the SSLContext. Settings
     * specified here override any duplicate settings provided at the overall level by this class. These parameters
     * apply to SSLSocketFactory and SSLEngine produced by the SSLContext produced from this class as well as to the
     * SSLContext itself.
     */
    private SSLContextClientParameters clientParameters;

    /**
     * The optional configuration options to be applied purely to the server side settings of the SSLContext. Settings
     * specified here override any duplicate settings provided at the overall level by this class. These parameters
     * apply to SSLServerSocketFactory and SSLEngine produced by the SSLContext produced from this class as well as to
     * the SSLContext itself.
     */
    private SSLContextServerParameters serverParameters;

    /**
     * The optional provider identifier for the JSSE implementation to use when constructing an SSLContext.
     */
    private String provider;

    /**
     * The optional protocol for the secure sockets created by the SSLContext represented by this instance's
     * configuration. See Appendix A in the Java Secure Socket Extension Reference Guide for information about standard
     * protocol names.
     */
    private String secureSocketProtocol;

    /**
     * An optional certificate alias to use. This is useful when the keystore has multiple certificates.
     */
    private String certAlias;

    /**
     * The optional explicitly configured cipher suites for this configuration.
     */
    private CipherSuitesParameters cipherSuites;

    /**
     * The optional cipher suite filter configuration for this configuration.
     */
    private FilterParameters cipherSuitesFilter;

    /**
     * The optional explicitly configured secure socket protocol names for this configuration.
     */
    private SecureSocketProtocolsParameters secureSocketProtocols;

    /**
     * The option secure socket protocol name filter configuration for this configuration.
     */
    private FilterParameters secureSocketProtocolsFilter;

    /**
     * The optional SSLSessionContext timeout time for javax.net.ssl.SSLSession in seconds.
     */
    private String sessionTimeout;

    public SSLContextParameters getConfig() {
        return config;
    }

    public void setConfig(SSLContextParameters config) {
        this.config = config;
    }

    public KeyManagersParameters getKeyManagers() {
        return this.keyManagers;
    }

    public void setKeyManagers(KeyManagersParameters keyManagers) {
        this.keyManagers = keyManagers;
    }

    public TrustManagersParameters getTrustManagers() {
        return this.trustManagers;
    }

    public void setTrustManagers(TrustManagersParameters trustManagers) {
        this.trustManagers = trustManagers;
    }

    public SecureRandomParameters getSecureRandom() {
        return this.secureRandom;
    }

    public void setSecureRandom(SecureRandomParameters secureRandom) {
        this.secureRandom = secureRandom;
    }

    public SSLContextClientParameters getClientParameters() {
        return this.clientParameters;
    }

    public void setClientParameters(SSLContextClientParameters clientParameters) {
        this.clientParameters = clientParameters;
    }

    public SSLContextServerParameters getServerParameters() {
        return this.serverParameters;
    }

    public void setServerParameters(SSLContextServerParameters serverParameters) {
        this.serverParameters = serverParameters;
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSecureSocketProtocol() {
        return this.secureSocketProtocol;
    }

    public void setSecureSocketProtocol(String secureSocketProtocol) {
        this.secureSocketProtocol = secureSocketProtocol;
    }

    public String getCertAlias() {
        return this.certAlias;
    }

    public void setCertAlias(String certAlias) {
        this.certAlias = certAlias;
    }

    public CipherSuitesParameters getCipherSuites() {
        return this.cipherSuites;
    }

    public void setCipherSuites(CipherSuitesParameters cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public FilterParameters getCipherSuitesFilter() {
        return this.cipherSuitesFilter;
    }

    public void setCipherSuitesFilter(FilterParameters cipherSuitesFilter) {
        this.cipherSuitesFilter = cipherSuitesFilter;
    }

    public SecureSocketProtocolsParameters getSecureSocketProtocols() {
        return this.secureSocketProtocols;
    }

    public void setSecureSocketProtocols(SecureSocketProtocolsParameters secureSocketProtocols) {
        this.secureSocketProtocols = secureSocketProtocols;
    }

    public FilterParameters getSecureSocketProtocolsFilter() {
        return this.secureSocketProtocolsFilter;
    }

    public void setSecureSocketProtocolsFilter(FilterParameters secureSocketProtocolsFilter) {
        this.secureSocketProtocolsFilter = secureSocketProtocolsFilter;
    }

    public String getSessionTimeout() {
        return this.sessionTimeout;
    }

    public void setSessionTimeout(String sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

}
