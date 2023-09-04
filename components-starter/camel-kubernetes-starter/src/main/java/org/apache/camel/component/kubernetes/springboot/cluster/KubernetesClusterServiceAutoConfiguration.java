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
package org.apache.camel.component.kubernetes.springboot.cluster;

import org.apache.camel.cluster.CamelClusterService;
import org.apache.camel.component.kubernetes.cluster.KubernetesClusterService;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.cluster.ClusteredRouteControllerAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Optional;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ ClusteredRouteControllerAutoConfiguration.class, CamelAutoConfiguration.class })
@ConditionalOnProperty(prefix = "camel.cluster.kubernetes", name = "enabled")
@EnableConfigurationProperties(KubernetesClusterServiceConfiguration.class)
public class KubernetesClusterServiceAutoConfiguration {

    @Autowired
    private KubernetesClusterServiceConfiguration configuration;

    @Bean(name = "kubernetes-cluster-service")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CamelClusterService kubernetesClusterService() throws Exception {
        KubernetesClusterService service = new KubernetesClusterService();

        Optional.ofNullable(configuration.getId())
                .ifPresent(service::setId);
        Optional.ofNullable(configuration.getRetryPeriodMillis())
                .ifPresent(service::setRetryPeriodMillis);
        Optional.ofNullable(configuration.getOrder())
                .ifPresent(service::setOrder);
        Optional.ofNullable(configuration.getAttributes())
                .ifPresent(service::setAttributes);
        Optional.ofNullable(configuration.getClusterLabels())
                .ifPresent(service::setClusterLabels);
        Optional.ofNullable(configuration.getKubernetesNamespace())
                .ifPresent(service::setKubernetesNamespace);
        Optional.ofNullable(configuration.getConfigMapName())
                .ifPresent(service::setConfigMapName);
        Optional.ofNullable(configuration.getConnectionTimeoutMillis())
                .ifPresent(service::setConnectionTimeoutMillis);
        Optional.ofNullable(configuration.getJitterFactor())
                .ifPresent(service::setJitterFactor);
        Optional.ofNullable(configuration.getLeaseDurationMillis())
                .ifPresent(service::setLeaseDurationMillis);
        Optional.ofNullable(configuration.getMasterUrl())
                .ifPresent(service::setMasterUrl);
        Optional.ofNullable(configuration.getRenewDeadlineMillis())
                .ifPresent(service::setRenewDeadlineMillis);
        Optional.ofNullable(configuration.getPodName())
                .ifPresent(service::setPodName);

        return service;
    }
}
