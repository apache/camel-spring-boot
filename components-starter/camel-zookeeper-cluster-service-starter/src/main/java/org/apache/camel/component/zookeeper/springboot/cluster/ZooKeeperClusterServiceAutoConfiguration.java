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
package org.apache.camel.component.zookeeper.springboot.cluster;

import org.apache.camel.cluster.CamelClusterService;
import org.apache.camel.component.zookeeper.cluster.ZooKeeperClusterService;
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

@Configuration
@AutoConfigureBefore({ ClusteredRouteControllerAutoConfiguration.class, CamelAutoConfiguration.class })
@ConditionalOnProperty(prefix = "camel.cluster.zookeeper", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ZooKeeperClusterServiceConfiguration.class)
public class ZooKeeperClusterServiceAutoConfiguration {

    @Autowired
    private ZooKeeperClusterServiceConfiguration configuration;

    @Bean(name = "zookeeper-cluster-service")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CamelClusterService zookeeperClusterService() throws Exception {
        ZooKeeperClusterService service = new ZooKeeperClusterService();

        Optional.ofNullable(configuration.getId()).ifPresent(service::setId);
        Optional.ofNullable(configuration.getOrder()).ifPresent(service::setOrder);
        Optional.ofNullable(configuration.getAttributes()).ifPresent(service::setAttributes);
        Optional.ofNullable(configuration.getBasePath()).ifPresent(service::setBasePath);
        Optional.ofNullable(configuration.getNamespace()).ifPresent(service::setNamespace);
        Optional.ofNullable(configuration.getAuthInfoList()).ifPresent(service::setAuthInfoList);
        Optional.of(configuration.getConnectionTimeout()).ifPresent(service::setConnectionTimeout);
        Optional.ofNullable(configuration.getConnectionTimeoutUnit()).ifPresent(service::setConnectionTimeoutUnit);
        Optional.ofNullable(configuration.getCuratorFramework()).ifPresent(service::setCuratorFramework);
        Optional.of(configuration.getMaxCloseWait()).ifPresent(service::setMaxCloseWait);
        Optional.ofNullable(configuration.getMaxCloseWaitUnit()).ifPresent(service::setMaxCloseWaitUnit);
        Optional.ofNullable(configuration.getNodes()).ifPresent(service::setNodes);
        Optional.of(configuration.getReconnectBaseSleepTime()).ifPresent(service::setReconnectBaseSleepTime);
        Optional.ofNullable(configuration.getReconnectBaseSleepTimeUnit())
                .ifPresent(service::setReconnectBaseSleepTimeUnit);
        Optional.of(configuration.getReconnectMaxRetries()).ifPresent(service::setReconnectMaxRetries);
        Optional.ofNullable(configuration.getRetryPolicy()).ifPresent(service::setRetryPolicy);
        Optional.of(configuration.getSessionTimeout()).ifPresent(service::setSessionTimeout);
        Optional.ofNullable(configuration.getSessionTimeoutUnit()).ifPresent(service::setSessionTimeoutUnit);
        service.setConfiguration(configuration);

        return service;
    }
}
