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
package org.apache.camel.component.resilience4j.micrometer;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Resilience4jMicrometerFactory;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@Conditional({ ConditionalOnCamelContextAndAutoConfigurationBeans.class })
@ConditionalOnProperty(prefix = "camel.resilience4j", name = "micrometerEnabled", havingValue = "true", matchIfMissing = true)
public class Resilience4jMicrometerAutoConfiguration {

    @Bean
    public Resilience4jMicrometerFactory resilience4jMicrometerFactory(CamelContext camelContext) throws Exception {
        Resilience4jMicrometerFactory factory = new DefaultResilience4jMicrometerFactory();
        factory.setCamelContext(camelContext);
        camelContext.addService(factory);
        return factory;
    }

}
