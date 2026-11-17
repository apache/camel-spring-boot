/**
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
package org.apache.camel.component.file.springboot.cluster;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.camel.cluster.CamelClusterService;
import org.apache.camel.component.file.cluster.FileLockClusterService;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.cluster.ClusteredRouteControllerAutoConfiguration;
import org.apache.camel.spring.boot.cluster.TimePatternConverter;
import org.apache.camel.spring.boot.util.RemovedPropertiesHelper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration(before = { ClusteredRouteControllerAutoConfiguration.class, CamelAutoConfiguration.class })
@ConditionalOnProperty(prefix = "camel.cluster.file", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(FileLockClusterServiceConfiguration.class)
public class FileLockClusterServiceAutoConfiguration {

    private final FileLockClusterServiceConfiguration configuration;

    public FileLockClusterServiceAutoConfiguration(FileLockClusterServiceConfiguration configuration,
            Environment environment) {
        this.configuration = configuration;
        // the options were renamed in Camel 3.10, and Spring Boot ignores the old names without a log line
        RemovedPropertiesHelper.warnRenamedProperties(environment, "camel.component.file.cluster.service",
                "camel.cluster.file", "3.10", FileLockClusterServiceConfiguration.class);
    }

    @Bean(name = "file-lock-cluster-service")
    public CamelClusterService fileClusterService() throws Exception {
        FileLockClusterService service = new FileLockClusterService();

        Optional.ofNullable(configuration.getId()).ifPresent(service::setId);
        Optional.ofNullable(configuration.getRoot()).ifPresent(service::setRoot);
        Optional.ofNullable(configuration.getOrder()).ifPresent(service::setOrder);
        Optional.ofNullable(configuration.getAttributes()).ifPresent(service::setAttributes);
        Optional.ofNullable(configuration.getAcquireLockDelay()).map(TimePatternConverter::toMilliSeconds)
                .ifPresent(v -> service.setAcquireLockDelay(v, TimeUnit.MILLISECONDS));
        Optional.ofNullable(configuration.getAcquireLockInterval()).map(TimePatternConverter::toMilliSeconds)
                .ifPresent(v -> service.setAcquireLockInterval(v, TimeUnit.MILLISECONDS));
        Optional.ofNullable(configuration.getHeartbeatTimeoutMultiplier()).ifPresent(service::setHeartbeatTimeoutMultiplier);
        Optional.ofNullable(configuration.getClusterDataTaskMaxAttempts()).ifPresent(service::setClusterDataTaskMaxAttempts);
        Optional.ofNullable(configuration.getClusterDataTaskTimeout()).ifPresent(service::setClusterDataTaskTimeout);

        return service;
    }
}
