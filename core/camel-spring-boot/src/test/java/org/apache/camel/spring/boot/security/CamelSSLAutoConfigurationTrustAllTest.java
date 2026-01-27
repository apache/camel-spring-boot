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

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustAllTrustManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testing the ssl configuration
 */
public class CamelSSLAutoConfigurationTrustAllTest {

    @Test
    public void checkSSLTrustAllTest() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CamelSSLAutoConfiguration.class, CamelAutoConfiguration.class))
                .withPropertyValues("camel.ssl.enabled=true",
                        "camel.ssl.cert-alias=web",
                        "camel.ssl.key-managers.key-password=changeit",
                        "camel.ssl.key-managers.key-store.password=changeit",
                        "camel.ssl.key-managers.key-store.type=PKCS12",
                        "camel.ssl.trust-all-certificates=true")
                .run(context -> {

                    SSLContextParameters ssl = context.getBean(SSLContextParameters.class);
                    assertNotNull(ssl);

                    assertInstanceOf(TrustAllTrustManager.class, ssl.getTrustManagers().getTrustManager());
                });
    }

}
