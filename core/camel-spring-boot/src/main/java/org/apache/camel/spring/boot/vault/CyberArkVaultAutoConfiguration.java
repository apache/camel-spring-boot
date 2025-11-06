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
import org.apache.camel.vault.CyberArkVaultConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CyberArkVaultConfigurationProperties.class)
@AutoConfigureAfter(CamelAutoConfiguration.class)
public class CyberArkVaultAutoConfiguration {

    @Bean
    public CyberArkVaultConfiguration cyberarkVaultConfiguration(CyberArkVaultConfigurationProperties config) {
        CyberArkVaultConfiguration answer = new CyberArkVaultConfiguration();
        answer.setUrl(config.getUrl());
        answer.setAccount(config.getAccount());
        answer.setUsername(config.getUsername());
        answer.setPassword(config.getPassword());
        answer.setApiKey(config.getApiKey());
        answer.setAuthToken(config.getAuthToken());
        answer.setVerifySsl(config.isVerifySsl());
        answer.setCertificatePath(config.getCertificatePath());
        answer.setSecrets(config.getSecrets());
        return answer;
    }

}
