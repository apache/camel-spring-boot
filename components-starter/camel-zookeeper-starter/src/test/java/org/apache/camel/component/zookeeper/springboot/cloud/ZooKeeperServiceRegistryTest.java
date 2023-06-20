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
package org.apache.camel.component.zookeeper.springboot.cloud;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.cloud.ServiceRegistry;
import org.apache.camel.component.zookeeper.cloud.MetaData;
import org.apache.camel.impl.cloud.DefaultServiceDefinition;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class ZooKeeperServiceRegistryTest {

    private static final String SERVICE_PATH = "/camel";
    private static final String SERVICE_ID = UUID.randomUUID().toString();
    private static final String SERVICE_NAME = "my-service";
    private static final String SERVICE_HOST = "localhost";
    private static final int SERVICE_PORT = AvailablePortFinder.getNextAvailable();

    @TempDir
    Path temporaryFolder ;

    @Test
    public void testServiceRegistry() throws Exception {
        final int zkPort =  AvailablePortFinder.getNextAvailable();
        final File zkDir =  temporaryFolder.toFile();

        try (final TestingServer zkServer = new TestingServer(zkPort, zkDir);
             final ZooKeeperTestClient zkClient = new ZooKeeperTestClient("localhost:" + zkPort)) {
            zkServer.start();
            zkClient.start();
            new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class)
                .withPropertyValues(
                    "debug=false",
                    "spring.main.banner-mode=OFF",
                    "spring.application.name=" + UUID.randomUUID(),
                    "camel.cloud.zookeeper.enabled=true",
                    "camel.cloud.zookeeper.nodes=localhost:" + zkPort,
                    "camel.cloud.zookeeper.id=" + UUID.randomUUID(),
                    "camel.cloud.zookeeper.base-path=" + SERVICE_PATH,
                    "camel.cloud.zookeeper.service-host=localhost")
                .run(
                    context -> {
                        assertThat(context).hasSingleBean(CamelContext.class);
                        assertThat(context).hasSingleBean(ServiceRegistry.class);

                        final ServiceRegistry serviceRegistry = context.getBean(ServiceRegistry.class);

                        assertThat(serviceRegistry).isNotNull();
                        serviceRegistry.start();
                        serviceRegistry.register(
                            DefaultServiceDefinition.builder()
                                .withHost(SERVICE_HOST)
                                .withPort(SERVICE_PORT)
                                .withName(SERVICE_NAME)
                                .withId(SERVICE_ID)
                                .build()
                        );

                        try (ServiceDiscovery<MetaData> discovery = zkClient.discovery()) {
                            final Collection<ServiceInstance<MetaData>> services = discovery.queryForInstances(SERVICE_NAME);

                            assertThat(services).hasSize(1);
                            assertThat(services).first().hasFieldOrPropertyWithValue("id", SERVICE_ID);
                            assertThat(services).first().hasFieldOrPropertyWithValue("name", SERVICE_NAME);
                            assertThat(services).first().hasFieldOrPropertyWithValue("address", SERVICE_HOST);
                            assertThat(services).first().hasFieldOrPropertyWithValue("port", SERVICE_PORT);
                        }
                    }
                );
        }
    }

    // *************************************
    // Config
    // *************************************

    @EnableAutoConfiguration
    @Configuration
    public static class TestConfiguration {
    }

    // *************************************
    // Helpers
    // *************************************

    public static class ZooKeeperTestClient implements AutoCloseable {
        private final CuratorFramework curator;
        private final ServiceDiscovery<MetaData> discovery;

        public ZooKeeperTestClient(String nodes) {
            curator = CuratorFrameworkFactory.builder()
                .connectString(nodes)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
            discovery = ServiceDiscoveryBuilder.builder(MetaData.class)
                .client(curator)
                .basePath(SERVICE_PATH)
                .serializer(new JsonInstanceSerializer<>(MetaData.class))
                .build();
        }

        public ServiceDiscovery<MetaData> discovery() {
            return discovery;
        }

        public void start() throws Exception {
            curator.start();
            discovery.start();
        }

        @Override
        public void close() {
            CloseableUtils.closeQuietly(discovery);
            CloseableUtils.closeQuietly(curator);
        }
    }
}
