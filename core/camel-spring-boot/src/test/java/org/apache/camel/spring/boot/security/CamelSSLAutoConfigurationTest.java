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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.jsse.GlobalSSLContextParametersSupplier;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Testing the ssl configuration
 */
public class CamelSSLAutoConfigurationTest {
    @Test
    public void checkSSLConfigPropertiesPresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelSSLAutoConfiguration.class, CamelAutoConfiguration.class))
                .withPropertyValues("camel.ssl.config.cert-alias=web",
                        "camel.ssl.config.key-managers.key-password=changeit",
                        "camel.ssl.config.key-managers.key-store.password=changeit",
                        "camel.ssl.config.key-managers.key-store.type=PKCS12",
                        "camel.ssl.config.trust-managers.key-store.password=changeit",
                        "camel.ssl.config.trust-managers.key-store.type=jks",
                        "camel.ssl.config.cipher-suites-filter.include[0]=abc",
                        "camel.ssl.config.cipher-suites-filter.include[1]=def",
                        "camel.ssl.config.cipher-suites-filter.exclude[0]=xxx")
                .run(context -> {
                    GlobalSSLContextParametersSupplier supplier = context
                            .getBean(GlobalSSLContextParametersSupplier.class);
                    assertThat(context).hasSingleBean(CamelSSLAutoConfiguration.class);
                    assertNotNull(supplier);
                    assertNotNull(supplier.get());
                    assertEquals("web", supplier.get().getCertAlias());

                    assertNotNull(supplier.get().getKeyManagers());
                    assertEquals("changeit", supplier.get().getKeyManagers().getKeyPassword());

                    assertNotNull(supplier.get().getKeyManagers().getKeyStore());
                    assertEquals("changeit", supplier.get().getKeyManagers().getKeyStore().getPassword());
                    assertEquals("PKCS12", supplier.get().getKeyManagers().getKeyStore().getType());

                    assertNotNull(supplier.get().getTrustManagers());
                    assertNotNull(supplier.get().getTrustManagers().getKeyStore());
                    assertEquals("changeit", supplier.get().getTrustManagers().getKeyStore().getPassword());
                    assertEquals("jks", supplier.get().getTrustManagers().getKeyStore().getType());

                    assertEquals(2, supplier.get().getCipherSuitesFilter().getInclude().size());
                    assertEquals("abc", supplier.get().getCipherSuitesFilter().getInclude().get(0));
                    assertEquals("def", supplier.get().getCipherSuitesFilter().getInclude().get(1));
                    assertEquals(1, supplier.get().getCipherSuitesFilter().getExclude().size());
                    assertEquals("xxx", supplier.get().getCipherSuitesFilter().getExclude().get(0));

                    // since no camel.ssl properties provided
                    Assertions.assertThrows(NoSuchBeanDefinitionException.class,
                            () -> context.getBean(SSLContextParameters.class));
                });
    }

    @Test
    public void checkSSLPropertiesPresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelSSLAutoConfiguration.class, CamelAutoConfiguration.class))
                .withPropertyValues("camel.ssl.cert-alias=intra", "camel.ssl.key-managers.key-password=secure",
                        "camel.ssl.key-managers.key-store.password=secure", "camel.ssl.key-managers.key-store.type=jks",
                        "camel.ssl.trust-managers.key-store.password=secure",
                        "camel.ssl.trust-managers.key-store.type=PKCS12")
                .run(context -> {
                    SSLContextParameters contextParams = context.getBean(SSLContextParameters.class);
                    assertNotNull(contextParams);
                    assertEquals("intra", contextParams.getCertAlias());

                    assertNotNull(contextParams.getKeyManagers());
                    assertEquals("secure", contextParams.getKeyManagers().getKeyPassword());

                    assertNotNull(contextParams.getKeyManagers().getKeyStore());
                    assertEquals("secure", contextParams.getKeyManagers().getKeyStore().getPassword());
                    assertEquals("jks", contextParams.getKeyManagers().getKeyStore().getType());

                    assertNotNull(contextParams.getTrustManagers());
                    assertNotNull(contextParams.getTrustManagers().getKeyStore());
                    assertEquals("secure", contextParams.getTrustManagers().getKeyStore().getPassword());
                    assertEquals("PKCS12", contextParams.getTrustManagers().getKeyStore().getType());

                    // since no camel.ssl.config properties provided
                    Assertions.assertThrows(NoSuchBeanDefinitionException.class,
                            () -> context.getBean(GlobalSSLContextParametersSupplier.class));
                });
    }

    @Test
    public void checkSSLAndConfigPropertiesBeanPresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelSSLAutoConfiguration.class, CamelAutoConfiguration.class))
                .withPropertyValues(
                        // camel.ssl.config
                        "camel.ssl.config.cert-alias=web", "camel.ssl.config.key-managers.key-password=changeit",
                        "camel.ssl.config.key-managers.key-store.password=changeit",
                        "camel.ssl.config.key-managers.key-store.type=PKCS12",
                        "camel.ssl.config.trust-managers.key-store.password=changeit",
                        "camel.ssl.config.trust-managers.key-store.type=jks",
                        // camel.ssl
                        "camel.ssl.cert-alias=intra", "camel.ssl.key-managers.key-password=secure",
                        "camel.ssl.key-managers.key-store.password=secure", "camel.ssl.key-managers.key-store.type=jks",
                        "camel.ssl.trust-managers.key-store.password=secure",
                        "camel.ssl.trust-managers.key-store.type=PKCS12")
                .run(context -> {
                    // bean with camel.ssl.config properties
                    GlobalSSLContextParametersSupplier supplier = context
                            .getBean(GlobalSSLContextParametersSupplier.class);
                    assertThat(context).hasSingleBean(CamelSSLAutoConfiguration.class);
                    assertNotNull(supplier);
                    assertNotNull(supplier.get());
                    assertEquals("web", supplier.get().getCertAlias());

                    assertNotNull(supplier.get().getKeyManagers());
                    assertEquals("changeit", supplier.get().getKeyManagers().getKeyPassword());

                    assertNotNull(supplier.get().getKeyManagers().getKeyStore());
                    assertEquals("changeit", supplier.get().getKeyManagers().getKeyStore().getPassword());
                    assertEquals("PKCS12", supplier.get().getKeyManagers().getKeyStore().getType());

                    assertNotNull(supplier.get().getTrustManagers());
                    assertNotNull(supplier.get().getTrustManagers().getKeyStore());
                    assertEquals("changeit", supplier.get().getTrustManagers().getKeyStore().getPassword());
                    assertEquals("jks", supplier.get().getTrustManagers().getKeyStore().getType());

                    // bean with camel.ssl properties
                    SSLContextParameters contextParams = context.getBean(SSLContextParameters.class);
                    assertNotNull(contextParams);
                    assertEquals("intra", contextParams.getCertAlias());

                    assertNotNull(contextParams.getKeyManagers());
                    assertEquals("secure", contextParams.getKeyManagers().getKeyPassword());

                    assertNotNull(contextParams.getKeyManagers().getKeyStore());
                    assertEquals("secure", contextParams.getKeyManagers().getKeyStore().getPassword());
                    assertEquals("jks", contextParams.getKeyManagers().getKeyStore().getType());

                    assertNotNull(contextParams.getTrustManagers());
                    assertNotNull(contextParams.getTrustManagers().getKeyStore());
                    assertEquals("secure", contextParams.getTrustManagers().getKeyStore().getPassword());
                    assertEquals("PKCS12", contextParams.getTrustManagers().getKeyStore().getType());
                });
    }

    @Test
    public void checkSSLPropertiesCopy() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelSSLAutoConfiguration.class, CamelAutoConfiguration.class))
                .withPropertyValues(
                        // camel.ssl.config
                        "camel.ssl.config.cert-alias=web", "camel.ssl.config.key-managers.key-password=changeit",
                        "camel.ssl.config.key-managers.key-store.password=changeit",
                        "camel.ssl.config.key-managers.key-store.type=PKCS12",
                        "camel.ssl.config.trust-managers.key-store.password=changeit",
                        "camel.ssl.config.trust-managers.key-store.type=jks",
                        // camel.ssl
                        "camel.ssl.cert-alias=intra")
                .run(context -> {
                    // bean with camel.ssl.config properties
                    GlobalSSLContextParametersSupplier supplier = context
                            .getBean(GlobalSSLContextParametersSupplier.class);
                    assertThat(context).hasSingleBean(CamelSSLAutoConfiguration.class);
                    assertNotNull(supplier);
                    assertNotNull(supplier.get());
                    assertEquals("web", supplier.get().getCertAlias());

                    assertNotNull(supplier.get().getKeyManagers());
                    assertEquals("changeit", supplier.get().getKeyManagers().getKeyPassword());

                    assertNotNull(supplier.get().getKeyManagers().getKeyStore());
                    assertEquals("changeit", supplier.get().getKeyManagers().getKeyStore().getPassword());
                    assertEquals("PKCS12", supplier.get().getKeyManagers().getKeyStore().getType());

                    assertNotNull(supplier.get().getTrustManagers());
                    assertNotNull(supplier.get().getTrustManagers().getKeyStore());
                    assertEquals("changeit", supplier.get().getTrustManagers().getKeyStore().getPassword());
                    assertEquals("jks", supplier.get().getTrustManagers().getKeyStore().getType());

                    // bean created since we have a single camel.ssl prop
                    SSLContextParameters contextParams = context.getBean(SSLContextParameters.class);
                    assertNotNull(contextParams);
                    assertEquals("intra", contextParams.getCertAlias());

                    // these copied from camel.ssl.config
                    assertNotNull(contextParams.getKeyManagers());
                    assertEquals("changeit", contextParams.getKeyManagers().getKeyPassword());

                    assertNotNull(contextParams.getKeyManagers().getKeyStore());
                    assertEquals("changeit", contextParams.getKeyManagers().getKeyStore().getPassword());
                    assertEquals("PKCS12", contextParams.getKeyManagers().getKeyStore().getType());

                    assertNotNull(contextParams.getTrustManagers());
                    assertNotNull(contextParams.getTrustManagers().getKeyStore());
                    assertEquals("changeit", contextParams.getTrustManagers().getKeyStore().getPassword());
                    assertEquals("jks", contextParams.getTrustManagers().getKeyStore().getType());
                });
    }

    @Test
    public void checkSSLPQCPropertiesPresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelSSLAutoConfiguration.class, CamelAutoConfiguration.class))
                .withPropertyValues(
                        "camel.ssl.cert-alias=pqc-test",
                        "camel.ssl.named-groups.named-group[0]=X25519MLKEM768",
                        "camel.ssl.named-groups.named-group[1]=x25519",
                        "camel.ssl.named-groups-filter.include[0]=.*MLKEM.*",
                        "camel.ssl.named-groups-filter.exclude[0]=.*legacy.*",
                        "camel.ssl.signature-schemes.signature-scheme[0]=ML-DSA",
                        "camel.ssl.signature-schemes.signature-scheme[1]=ed25519",
                        "camel.ssl.signature-schemes-filter.include[0]=.*DSA.*",
                        "camel.ssl.signature-schemes-filter.exclude[0]=.*SHA1.*")
                .run(context -> {
                    SSLContextParameters contextParams = context.getBean(SSLContextParameters.class);
                    assertNotNull(contextParams);
                    assertEquals("pqc-test", contextParams.getCertAlias());

                    // verify named groups (PQC key exchange)
                    assertNotNull(contextParams.getNamedGroups());
                    assertEquals(2, contextParams.getNamedGroups().getNamedGroup().size());
                    assertEquals("X25519MLKEM768", contextParams.getNamedGroups().getNamedGroup().get(0));
                    assertEquals("x25519", contextParams.getNamedGroups().getNamedGroup().get(1));

                    // verify named groups filter
                    assertNotNull(contextParams.getNamedGroupsFilter());
                    assertEquals(1, contextParams.getNamedGroupsFilter().getInclude().size());
                    assertEquals(".*MLKEM.*", contextParams.getNamedGroupsFilter().getInclude().get(0));
                    assertEquals(1, contextParams.getNamedGroupsFilter().getExclude().size());
                    assertEquals(".*legacy.*", contextParams.getNamedGroupsFilter().getExclude().get(0));

                    // verify signature schemes (PQC signatures)
                    assertNotNull(contextParams.getSignatureSchemes());
                    assertEquals(2, contextParams.getSignatureSchemes().getSignatureScheme().size());
                    assertEquals("ML-DSA", contextParams.getSignatureSchemes().getSignatureScheme().get(0));
                    assertEquals("ed25519", contextParams.getSignatureSchemes().getSignatureScheme().get(1));

                    // verify signature schemes filter
                    assertNotNull(contextParams.getSignatureSchemesFilter());
                    assertEquals(1, contextParams.getSignatureSchemesFilter().getInclude().size());
                    assertEquals(".*DSA.*", contextParams.getSignatureSchemesFilter().getInclude().get(0));
                    assertEquals(1, contextParams.getSignatureSchemesFilter().getExclude().size());
                    assertEquals(".*SHA1.*", contextParams.getSignatureSchemesFilter().getExclude().get(0));
                });
    }

    @Test
    public void checkNoSSLPropertiesPresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelSSLAutoConfiguration.class, CamelAutoConfiguration.class))
                .run(context -> {
                    Assertions.assertThrows(NoSuchBeanDefinitionException.class,
                            () -> context.getBean(SSLContextParameters.class));

                    Assertions.assertThrows(NoSuchBeanDefinitionException.class,
                            () -> context.getBean(GlobalSSLContextParametersSupplier.class));
                });
    }
}
