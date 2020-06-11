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
package org.apache.camel.spring.boot.threadpool;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.ThreadPoolProfileBuilder;
import org.apache.camel.spi.ThreadPoolProfile;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CamelThreadPoolConfigurationProperties.class)
@AutoConfigureAfter(CamelAutoConfiguration.class)
public class CamelThreadPoolAutoConfiguration {

    @Bean
    public ThreadPoolProfile threadPool(CamelContext camelContext, CamelThreadPoolConfigurationProperties tp) {
        // okay we have all properties set so we should be able to create thread pool profiles and register them on camel
        ThreadPoolProfile defaultProfile = camelContext.getExecutorServiceManager().getDefaultThreadPoolProfile();
        final ThreadPoolProfile dp = new ThreadPoolProfileBuilder("default", defaultProfile)
                .poolSize(tp.getPoolSize())
                .maxPoolSize(tp.getMaxPoolSize())
                .keepAliveTime(tp.getKeepAliveTime(), tp.getTimeUnit())
                .maxQueueSize(tp.getMaxQueueSize())
                .allowCoreThreadTimeOut(tp.getAllowCoreThreadTimeOut())
                .rejectedPolicy(tp.getRejectedPolicy()).build();

        tp.getConfig().forEach((k, v) -> {
            ThreadPoolProfileBuilder builder = new ThreadPoolProfileBuilder(k, dp);
            final ThreadPoolProfile tpp = builder.poolSize(v.getPoolSize())
                    .maxPoolSize(v.getMaxPoolSize())
                    .keepAliveTime(v.getKeepAliveTime(), v.getTimeUnit())
                    .maxQueueSize(v.getMaxQueueSize())
                    .allowCoreThreadTimeOut(v.getAllowCoreThreadTimeOut())
                    .rejectedPolicy(v.getRejectedPolicy()).build();
            if (!tpp.isEmpty()) {
                camelContext.getExecutorServiceManager().registerThreadPoolProfile(tpp);
            }
        });

        if (!dp.isEmpty()) {
            dp.setDefaultProfile(true);
            camelContext.getExecutorServiceManager().setDefaultThreadPoolProfile(dp);
        }

        // need to return something
        return dp;
    }

}
