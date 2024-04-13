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
package org.apache.camel.component.infinispan.remote.springboot.cluster;

import org.apache.camel.component.infinispan.remote.cluster.InfinispanRemoteClusterService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InfinispanEmbeddedClusterServiceAutoConfigurationTest {

    @Test
    public void testDisable() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InfinispanRemoteClusterServiceAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                "spring.main.banner-mode=off",
                "camel.cluster.infinispan.remote.enabled=false")
            .run(
                context -> {
                    assertThat(context).doesNotHaveBean(InfinispanRemoteClusterService.class);
                });
    }

    /**
     * Testing that the service can be enabled and configured completely.
     */
    @Test
    public void testPropertiesMapped() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InfinispanRemoteClusterServiceAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                "spring.main.banner-mode=off",
                "camel.cluster.infinispan.remote.id=myid1")
            .run(
                context -> {
                    final InfinispanRemoteClusterService clusterService = context.getBean(InfinispanRemoteClusterService.class);

                    assertEquals("myid1", clusterService.getId());
                });
    }

    @EnableAutoConfiguration
    @Configuration
    public static class TestConfiguration {
    }
}
