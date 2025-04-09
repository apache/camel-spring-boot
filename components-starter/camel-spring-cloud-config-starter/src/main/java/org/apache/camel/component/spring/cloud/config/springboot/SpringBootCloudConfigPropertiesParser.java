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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Properties;

public class SpringBootCloudConfigPropertiesParser implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootCloudConfigPropertiesParser.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Properties properties = new Properties();
        ConfigurableEnvironment environment = event.getEnvironment();

        if (Boolean.parseBoolean(
                environment.getProperty("camel.component.spring-cloud-config.early-resolve-properties"))) {
            SpringCloudConfigPropertiesFunction springCloudConfigPropertiesFunction = new SpringCloudConfigPropertiesFunction();
            springCloudConfigPropertiesFunction.setEnvironment(environment);
            for (PropertySource mutablePropertySources : event.getEnvironment().getPropertySources()) {
                if (mutablePropertySources instanceof MapPropertySource mapPropertySource) {
                    mapPropertySource.getSource().forEach((key, value) -> {
                        String stringValue = null;
                        if ((value instanceof OriginTrackedValue originTrackedValue
                                && originTrackedValue.getValue() instanceof String v)) {
                            stringValue = v;
                        } else if (value instanceof String v) {
                            stringValue = v;
                        }
                        if (stringValue != null && stringValue.startsWith("{{spring-config:")
                                && stringValue.endsWith("}}")) {
                            LOG.debug("decrypting and overriding property {}", key);
                            try {
                                String element = springCloudConfigPropertiesFunction
                                        .apply(stringValue.replace("{{spring-config:", "").replace("}}", ""));
                                properties.put(key, element);
                            } catch (Exception e) {
                                // Log and do nothing
                                LOG.debug("failed to parse property {}. This exception is ignored.", key, e);
                            }
                        }
                    });
                }
            }
            environment.getPropertySources()
                    .addFirst(new PropertiesPropertySource("overridden-camel-spring-config-properties", properties));
        }
    }
}
