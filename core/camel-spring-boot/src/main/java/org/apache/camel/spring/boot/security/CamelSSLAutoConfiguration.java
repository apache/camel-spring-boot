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

import java.util.Collections;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.main.MainHelper;
import org.apache.camel.spi.ThreadPoolProfile;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.jsse.*;
import org.apache.camel.util.OrderedLocationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CamelSSLConfigurationProperties.class)
public class CamelSSLAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Conditional(CamelSSLAutoConfiguration.SSLCondition.class)
    public SSLContextParameters sslContextParameters(CamelContext camelContext,
            CamelSSLConfigurationProperties properties) {
        // use any camel.ssl.config props
        SSLContextParameters sslContext = camelContext.getSSLContextParameters() != null
                ? copyParams(camelContext.getSSLContextParameters()) : new SSLContextParameters();

        // override with any camel.ssl props
        SSLContextParameters config = new SSLContextBuilder(sslContext).certAlias(properties.getCertAlias())
                .cipherSuites(properties.getCipherSuites()).cipherSuitesFilter(properties.getCipherSuitesFilter())
                .clientParameters(properties.getClientParameters()).keyManagers(properties.getKeyManagers())
                .provider(properties.getProvider()).secureRandom(properties.getSecureRandom())
                .secureSocketProtocol(properties.getSecureSocketProtocol())
                .secureSocketProtocols(properties.getSecureSocketProtocols())
                .secureSocketProtocolsFilter(properties.getSecureSocketProtocolsFilter())
                .serverParameters(properties.getServerParameters()).sessionTimeout(properties.getSessionTimeout())
                .trustManager(properties.getTrustManagers()).build();

        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(CamelSSLAutoConfiguration.SSLConfigCondition.class)
    public GlobalSSLContextParametersSupplier sslContextParametersSupplier(CamelSSLConfigurationProperties properties) {
        final SSLContextParameters config = properties.getConfig() != null ? properties.getConfig()
                : new SSLContextParameters();
        return () -> config;
    }

    public static class SSLCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata annotatedTypeMetadata) {
            Binder binder = Binder.get(context.getEnvironment());
            Map<String, Object> sslProperties = binder.bind("camel.ssl", Bindable.mapOf(String.class, Object.class))
                    .orElse(Collections.emptyMap());
            sslProperties.remove("config");
            ConditionMessage.Builder message = ConditionMessage.forCondition("camel.ssl");
            if (sslProperties.size() > 0) {
                return ConditionOutcome.match(message.because("enabled"));
            }

            return ConditionOutcome.noMatch(message.because("not enabled"));
        }
    }

    public static class SSLConfigCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata annotatedTypeMetadata) {
            Binder binder = Binder.get(context.getEnvironment());
            Map<String, Object> sslProperties = binder
                    .bind("camel.ssl.config", Bindable.mapOf(String.class, Object.class))
                    .orElse(Collections.emptyMap());
            ConditionMessage.Builder message = ConditionMessage.forCondition("camel.ssl.config");
            if (!sslProperties.isEmpty()) {
                return ConditionOutcome.match(message.because("enabled"));
            }

            return ConditionOutcome.noMatch(message.because("not enabled"));
        }
    }

    private SSLContextParameters copyParams(SSLContextParameters sslContextParameters) {
        SSLContextParameters copy = new SSLContextParameters();
        copy.setCertAlias(sslContextParameters.getCertAlias());
        copy.setCipherSuites(sslContextParameters.getCipherSuites());
        copy.setCipherSuitesFilter(sslContextParameters.getCipherSuitesFilter());
        copy.setClientParameters(sslContextParameters.getClientParameters());
        copy.setKeyManagers(sslContextParameters.getKeyManagers());
        copy.setProvider(sslContextParameters.getProvider());
        copy.setSecureRandom(sslContextParameters.getSecureRandom());
        copy.setSecureSocketProtocol(sslContextParameters.getSecureSocketProtocol());
        copy.setSecureSocketProtocols(sslContextParameters.getSecureSocketProtocols());
        copy.setSecureSocketProtocolsFilter(sslContextParameters.getSecureSocketProtocolsFilter());
        copy.setServerParameters(sslContextParameters.getServerParameters());
        copy.setSessionTimeout(sslContextParameters.getSessionTimeout());
        copy.setTrustManagers(sslContextParameters.getTrustManagers());
        return copy;
    }

    private class SSLContextBuilder {

        private SSLContextParameters sslContextParameters;

        public SSLContextBuilder(SSLContextParameters sslContextParameters) {
            this.sslContextParameters = sslContextParameters;
        }

        public SSLContextBuilder certAlias(String certAlias) {
            if (certAlias != null) {
                sslContextParameters.setCertAlias(certAlias);
            }
            return this;
        }

        public SSLContextBuilder cipherSuites(CipherSuitesParameters cipherSuites) {
            if (cipherSuites != null) {
                sslContextParameters.setCipherSuites(cipherSuites);
            }
            return this;
        }

        public SSLContextBuilder cipherSuitesFilter(FilterParameters cipherSuitesFilter) {
            if (cipherSuitesFilter != null) {
                sslContextParameters.setCipherSuitesFilter(cipherSuitesFilter);
            }
            return this;
        }

        public SSLContextBuilder clientParameters(SSLContextClientParameters clientParameters) {
            if (clientParameters != null) {
                sslContextParameters.setClientParameters(clientParameters);
            }
            return this;
        }

        public SSLContextBuilder keyManagers(KeyManagersParameters keyManagers) {
            if (keyManagers != null) {
                sslContextParameters.setKeyManagers(keyManagers);
            }
            return this;
        }

        public SSLContextBuilder provider(String provider) {
            if (provider != null) {
                sslContextParameters.setProvider(provider);
            }
            return this;
        }

        public SSLContextBuilder secureRandom(SecureRandomParameters secureRandom) {
            if (secureRandom != null) {
                sslContextParameters.setSecureRandom(secureRandom);
            }
            return this;
        }

        public SSLContextBuilder secureSocketProtocol(String secureSocketProtocol) {
            if (secureSocketProtocol != null) {
                sslContextParameters.setSecureSocketProtocol(secureSocketProtocol);
            }
            return this;
        }

        public SSLContextBuilder secureSocketProtocols(SecureSocketProtocolsParameters secureSocketProtocols) {
            if (secureSocketProtocols != null) {
                sslContextParameters.setSecureSocketProtocols(secureSocketProtocols);
            }
            return this;
        }

        public SSLContextBuilder secureSocketProtocolsFilter(FilterParameters secureSocketProtocolsFilter) {
            if (secureSocketProtocolsFilter != null) {
                sslContextParameters.setSecureSocketProtocolsFilter(secureSocketProtocolsFilter);
            }
            return this;
        }

        public SSLContextBuilder serverParameters(SSLContextServerParameters serverParameters) {
            if (serverParameters != null) {
                sslContextParameters.setServerParameters(serverParameters);
            }
            return this;
        }

        public SSLContextBuilder sessionTimeout(String sessionTimeout) {
            if (sessionTimeout != null) {
                sslContextParameters.setSessionTimeout(sessionTimeout);
            }
            return this;
        }

        public SSLContextBuilder trustManager(TrustManagersParameters trustManager) {
            if (trustManager != null) {
                sslContextParameters.setTrustManagers(trustManager);
            }
            return this;
        }

        public SSLContextParameters build() {
            return this.sslContextParameters;
        }
    }

}
