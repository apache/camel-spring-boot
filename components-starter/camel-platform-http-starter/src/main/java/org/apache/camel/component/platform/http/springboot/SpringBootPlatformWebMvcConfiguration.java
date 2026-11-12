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
package org.apache.camel.component.platform.http.springboot;

import java.time.Duration;

import org.apache.camel.CamelContext;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.spring.boot.ComponentConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@EnableConfigurationProperties({ ComponentConfigurationProperties.class, PlatformHttpComponentConfiguration.class })
public class SpringBootPlatformWebMvcConfiguration implements WebMvcConfigurer {

    private final CamelContext camelContext;
    private final PlatformHttpComponentConfiguration platformHttpComponentConfiguration;
    private final ObjectProvider<WebMvcProperties> webMvcProperties;

    // WebMvcProperties is only registered in a servlet web application
    public SpringBootPlatformWebMvcConfiguration(CamelContext camelContext,
                                                 PlatformHttpComponentConfiguration platformHttpComponentConfiguration,
                                                 ObjectProvider<WebMvcProperties> webMvcProperties) {
        this.camelContext = camelContext;
        this.platformHttpComponentConfiguration = platformHttpComponentConfiguration;
        this.webMvcProperties = webMvcProperties;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        WebMvcProperties properties = webMvcProperties.getIfAvailable();
        Duration requestTimeout = properties != null ? properties.getAsync().getRequestTimeout() : null;

        if (requestTimeout == null && platformHttpComponentConfiguration.getRequestTimeout() != null) {
            configurer.setDefaultTimeout(platformHttpComponentConfiguration.getRequestTimeout());
        }

        if (requestTimeout != null && platformHttpComponentConfiguration.getRequestTimeout() == null) {
            long timeoutMillis = requestTimeout.toMillis();
            // Write onto the configuration bean so it is picked up if the component is configured later
            platformHttpComponentConfiguration.setRequestTimeout(timeoutMillis);
            // Also set directly on the component: the customizer may have already run before this
            // configurer is called (the eager platformHttpEngineRequestMapping bean forces it), so
            // setting the configuration bean alone is not sufficient in that case.
            PlatformHttpComponent component
                    = (PlatformHttpComponent) camelContext.getComponent("platform-http", false, false);
            if (component != null && component.getRequestTimeout() == 0) {
                component.setRequestTimeout(timeoutMillis);
            }
        }
    }
}
