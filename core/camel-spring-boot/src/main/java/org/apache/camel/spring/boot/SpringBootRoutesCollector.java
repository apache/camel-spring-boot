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

import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.main.DefaultRoutesCollector;
import org.springframework.context.ApplicationContext;

/**
 * Spring Boot {@link org.apache.camel.main.RoutesCollector}.
 */
public class SpringBootRoutesCollector extends DefaultRoutesCollector {

    private final ApplicationContext applicationContext;

    public SpringBootRoutesCollector(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected <T> Collection<T> findByType(CamelContext camelContext, Class<T> type) {
        // lookup in application context
        return applicationContext.getBeansOfType(type, true, true).values();
    }

}
