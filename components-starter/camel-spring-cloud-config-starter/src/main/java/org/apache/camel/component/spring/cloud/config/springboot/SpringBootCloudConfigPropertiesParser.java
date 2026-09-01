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
package org.apache.camel.component.spring.cloud.config.springboot;

import org.apache.camel.component.spring.cloud.config.SpringCloudConfigPropertiesFunction;
import org.apache.camel.spi.PropertiesFunction;
import org.apache.camel.spring.boot.AbstractEarlyResolutionPropertiesParser;
import org.springframework.core.env.ConfigurableEnvironment;

public class SpringBootCloudConfigPropertiesParser extends AbstractEarlyResolutionPropertiesParser {

    @Override
    protected String getEarlyResolutionProperty() {
        return "camel.component.spring-cloud-config.early-resolve-properties";
    }

    @Override
    protected String getOverridePropertySourceName() {
        return "overridden-camel-spring-config-properties";
    }

    @Override
    protected String getSourceDescription() {
        return "Spring Cloud Config";
    }

    @Override
    protected PropertiesFunction createPropertiesFunction(ConfigurableEnvironment environment) {
        SpringCloudConfigPropertiesFunction springCloudConfigPropertiesFunction
                = new SpringCloudConfigPropertiesFunction();
        springCloudConfigPropertiesFunction.setEnvironment(environment);
        return springCloudConfigPropertiesFunction;
    }
}
