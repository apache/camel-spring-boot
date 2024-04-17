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

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.cluster.CamelClusterService;
import org.apache.camel.component.zookeeper.cluster.ZooKeeperClusterService;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class ZooKeeperClusterServiceTest {
    private static final String SERVICE_PATH = "/camel";

    @TempDir
    Path temporaryFolder;


    @Test
    public void testDisable() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ZooKeeperClusterServiceAutoConfiguration.class))
                .withUserConfiguration(TestConfiguration.class)
                .withPropertyValues(
                        "spring.main.banner-mode=off",
                        "camel.cluster.zookeeper.enabled=false")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(ZooKeeperClusterService.class);
                        });
    }

    @Test
    public void testClusterService() throws Exception {
        final int zkPort = AvailablePortFinder.getNextAvailable();
        final File zkDir = temporaryFolder.toFile();

        try (TestingServer zkServer = new TestingServer(zkPort, zkDir)) {
            zkServer.start();

            new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues(
                        "spring.main.banner-mode=OFF",
                        "spring.application.name=" + UUID.randomUUID(),
                        "camel.cluster.zookeeper.nodes=localhost:" + zkPort,
                        "camel.cluster.zookeeper.id=" + UUID.randomUUID(),
                        "camel.cluster.zookeeper.base-path=" + SERVICE_PATH)
                    .run(context -> {
                        assertThat(context).hasSingleBean(CamelContext.class);
                        assertThat(context).hasSingleBean(CamelClusterService.class);

                        final CamelClusterService clusterService = context.getBean(CamelClusterService.class);

                        assertThat(clusterService).isNotNull();
                        clusterService.start();

                        assertThat(clusterService).isInstanceOf(ZooKeeperClusterService.class);
                    });
        }
    }

    @EnableAutoConfiguration
    @Configuration
    public static class TestConfiguration {
    }
}
