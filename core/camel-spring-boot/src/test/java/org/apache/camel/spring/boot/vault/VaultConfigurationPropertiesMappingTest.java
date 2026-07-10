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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.camel.vault.AwsVaultConfiguration;
import org.apache.camel.vault.AzureVaultConfiguration;
import org.apache.camel.vault.CyberArkVaultConfiguration;
import org.apache.camel.vault.GcpVaultConfiguration;
import org.apache.camel.vault.HashicorpVaultConfiguration;
import org.apache.camel.vault.IBMSecretsManagerVaultConfiguration;
import org.apache.camel.vault.KubernetesConfigMapVaultConfiguration;
import org.apache.camel.vault.KubernetesVaultConfiguration;
import org.apache.camel.vault.VaultConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Guards against drift between the Camel core vault configurations and the corresponding Spring Boot
 * {@code camel.vault.*} configuration properties classes.
 *
 * The vault auto-configurations copy the Spring Boot properties onto the Camel configuration with
 * {@link org.springframework.beans.BeanUtils#copyProperties(Object, Object)}, which matches properties by name and
 * type. When a vault option is added to Camel core, this test fails until the matching property (same name and type)
 * is added to the starter properties class, so the option stays configurable from Spring Boot.
 */
public class VaultConfigurationPropertiesMappingTest {

    static Stream<Arguments> vaultPairs() {
        return Stream.of(
                arguments(AwsVaultConfiguration.class, AwsVaultConfigurationProperties.class),
                arguments(AzureVaultConfiguration.class, AzureVaultConfigurationProperties.class),
                arguments(GcpVaultConfiguration.class, GcpVaultConfigurationProperties.class),
                arguments(HashicorpVaultConfiguration.class, HashicorpVaultConfigurationProperties.class),
                arguments(KubernetesVaultConfiguration.class, KubernetesVaultConfigurationProperties.class),
                arguments(KubernetesConfigMapVaultConfiguration.class,
                        KubernetesConfigMapVaultConfigurationProperties.class),
                arguments(IBMSecretsManagerVaultConfiguration.class, IBMVaultConfigurationProperties.class),
                arguments(CyberArkVaultConfiguration.class, CyberArkVaultConfigurationProperties.class));
    }

    @ParameterizedTest
    @MethodSource("vaultPairs")
    void starterExposesAllCamelVaultOptions(Class<?> camelConfiguration, Class<?> starterProperties) throws Exception {
        Map<String, PropertyDescriptor> starter = new HashMap<>();
        for (PropertyDescriptor pd : Introspector.getBeanInfo(starterProperties, Object.class)
                .getPropertyDescriptors()) {
            starter.put(pd.getName(), pd);
        }

        List<String> missing = new ArrayList<>();
        // only the options declared on the concrete configuration class, not the
        // per-provider sub-configurations inherited from VaultConfiguration
        for (PropertyDescriptor pd : Introspector.getBeanInfo(camelConfiguration, VaultConfiguration.class)
                .getPropertyDescriptors()) {
            if (pd.getWriteMethod() == null || pd.getReadMethod() == null) {
                continue;
            }
            PropertyDescriptor sp = starter.get(pd.getName());
            if (sp == null || sp.getReadMethod() == null || sp.getWriteMethod() == null
                    || !sp.getPropertyType().equals(pd.getPropertyType())) {
                missing.add(pd.getName() + " (" + pd.getPropertyType().getSimpleName() + ")");
            }
        }

        assertTrue(missing.isEmpty(),
                () -> camelConfiguration.getSimpleName() + " options not configurable via "
                        + starterProperties.getSimpleName() + " (add the missing properties): " + missing);
    }

}
