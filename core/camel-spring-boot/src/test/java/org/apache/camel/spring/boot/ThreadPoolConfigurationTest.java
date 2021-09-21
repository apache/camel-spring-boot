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
package org.apache.camel.spring.boot;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.ThreadPoolProfile;
import org.apache.camel.spring.boot.threadpool.CamelThreadPoolAutoConfiguration;
import org.apache.camel.util.concurrent.ThreadPoolRejectedPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CamelThreadPoolAutoConfiguration.class,
        ThreadPoolConfigurationTest.TestConfiguration.class
    },
    properties = {
        "camel.threadpool.pool-size = 5",
        "camel.threadpool.max-pool-size = 10",
        "camel.threadpool.max-queue-size = 20",
        "camel.threadpool.rejected-policy = DiscardOldest",
        "camel.threadpool.config[smallPool].pool-size = 2",
        "camel.threadpool.config[smallPool].rejected-policy = Abort",
        "camel.threadpool.config[bigPool].pool-size = 20",
        "camel.threadpool.config[bigPool].max-pool-size = 50",
        "camel.threadpool.config[bigPool].max-queue-size = 500",
    }
)
public class ThreadPoolConfigurationTest {
    @Autowired
    private CamelContext context;

    @Test
    public void testThreadPool() throws Exception {
        ThreadPoolProfile dpp = context.getExecutorServiceManager().getDefaultThreadPoolProfile();
        Assertions.assertNotNull(dpp);
        Assertions.assertEquals("default", dpp.getId());
        Assertions.assertEquals(5, dpp.getPoolSize().intValue());
        Assertions.assertEquals(10, dpp.getMaxPoolSize().intValue());
        Assertions.assertEquals(20, dpp.getMaxQueueSize().intValue());
        Assertions.assertEquals(ThreadPoolRejectedPolicy.DiscardOldest, dpp.getRejectedPolicy());

        ThreadPoolProfile sp = context.getExecutorServiceManager().getThreadPoolProfile("smallPool");
        Assertions.assertNotNull(sp);
        Assertions.assertEquals("smallPool", sp.getId());
        Assertions.assertEquals(2, sp.getPoolSize().intValue());
        Assertions.assertEquals(10, sp.getMaxPoolSize().intValue());
        Assertions.assertEquals(20, sp.getMaxQueueSize().intValue());
        Assertions.assertEquals(ThreadPoolRejectedPolicy.Abort, sp.getRejectedPolicy());

        ThreadPoolProfile bp = context.getExecutorServiceManager().getThreadPoolProfile("bigPool");
        Assertions.assertNotNull(bp);
        Assertions.assertEquals("bigPool", bp.getId());
        Assertions.assertEquals(20, bp.getPoolSize().intValue());
        Assertions.assertEquals(50, bp.getMaxPoolSize().intValue());
        Assertions.assertEquals(500, bp.getMaxQueueSize().intValue());
        Assertions.assertEquals(ThreadPoolRejectedPolicy.DiscardOldest, bp.getRejectedPolicy());
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                        .to("mock:result");
                }
            };
        }
    }
}
