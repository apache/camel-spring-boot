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

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.vault.AwsVaultConfiguration;
import org.apache.camel.vault.AzureVaultConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(AzureVaultConfigurationProperties.class)
@AutoConfigureAfter(CamelAutoConfiguration.class)
public class AzureVaultAutoConfiguration {

    @Bean
    public AzureVaultConfiguration azureVaultConfiguration(AzureVaultConfigurationProperties config) {
        AzureVaultConfiguration answer = new AzureVaultConfiguration();
        answer.setClientId(config.getClientId());
        answer.setClientSecret(config.getClientSecret());
        answer.setVaultName(config.getVaultName());
        answer.setTenantId(config.getTenantId());
        return answer;
    }

}
