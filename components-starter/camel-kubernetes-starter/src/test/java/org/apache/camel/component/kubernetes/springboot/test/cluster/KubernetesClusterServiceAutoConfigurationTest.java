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
package org.apache.camel.component.kubernetes.springboot.test.cluster;

import org.apache.camel.component.kubernetes.cluster.KubernetesClusterService;
import org.apache.camel.component.kubernetes.springboot.cluster.KubernetesClusterServiceAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubernetesClusterServiceAutoConfigurationTest {

    @Test
    public void testDisable() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KubernetesClusterServiceAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class)
            .run(
                context -> {
                    assertThat(context).doesNotHaveBean(KubernetesClusterService.class);
                }
            );
    }

    /**
     * Testing that the service can be enabled and configured completely.
     */
    @Test
    public void testPropertiesMapped() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KubernetesClusterServiceAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                "spring.main.banner-mode=off",
                "camel.cluster.kubernetes.enabled=true",
                "camel.cluster.kubernetes.id=myid1",
                "camel.cluster.kubernetes.master-url=http://myurl:9000",
                "camel.cluster.kubernetes.connection-timeout-millis=1234",
                "camel.cluster.kubernetes.kubernetes-namespace=ns1",
                "camel.cluster.kubernetes.config-map-name=cm",
                "camel.cluster.kubernetes.pod-name=mypod1",
                "camel.cluster.kubernetes.cluster-labels.app=myapp",
                "camel.cluster.kubernetes.cluster-labels.provider=myprovider",
                "camel.cluster.kubernetes.lease-duration-millis=10000",
                "camel.cluster.kubernetes.renew-deadline-millis=8000",
                "camel.cluster.kubernetes.retry-period-millis=4000")
            .run(
                context -> {
                    final KubernetesClusterService clusterService = context.getBean(KubernetesClusterService.class);

                    assertEquals("myid1", clusterService.getId());
                    assertEquals("http://myurl:9000", clusterService.getMasterUrl());
                    assertEquals(Integer.valueOf(1234), clusterService.getConnectionTimeoutMillis());
                    assertEquals("ns1", clusterService.getKubernetesNamespace());
                    assertEquals("cm", clusterService.getConfigMapName());
                    assertEquals("mypod1", clusterService.getPodName());

                    assertNotNull(clusterService.getClusterLabels());
                    assertEquals(2, clusterService.getClusterLabels().size());
                    assertEquals("myapp", clusterService.getClusterLabels().get("app"));
                    assertEquals("myprovider", clusterService.getClusterLabels().get("provider"));

                    assertEquals(1.2, clusterService.getJitterFactor(), 1e-10);
                    assertEquals(10000, clusterService.getLeaseDurationMillis());
                    assertEquals(8000, clusterService.getRenewDeadlineMillis());
                    assertEquals(4000, clusterService.getRetryPeriodMillis());
                }
            );
    }

    @EnableAutoConfiguration
    @Configuration
    public static class TestConfiguration {
    }
}

