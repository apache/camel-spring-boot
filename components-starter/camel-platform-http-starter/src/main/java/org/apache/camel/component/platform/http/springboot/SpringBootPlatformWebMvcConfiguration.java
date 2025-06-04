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

import org.apache.camel.spring.boot.ComponentConfigurationProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({ComponentConfigurationProperties.class,PlatformHttpComponentConfiguration.class, WebMvcProperties.class})
public class SpringBootPlatformWebMvcConfiguration implements WebMvcConfigurer {

    private PlatformHttpComponentConfiguration platformHttpComponentConfiguration;
    private WebMvcProperties webMvcProperties;

    public SpringBootPlatformWebMvcConfiguration(PlatformHttpComponentConfiguration platformHttpComponentConfiguration,
                                                 WebMvcProperties webMvcProperties) {
        this.platformHttpComponentConfiguration = platformHttpComponentConfiguration;
        this.webMvcProperties = webMvcProperties;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        if (webMvcProperties.getAsync().getRequestTimeout() == null &&
                platformHttpComponentConfiguration.getRequestTimeout() != null) {
            configurer.setDefaultTimeout(platformHttpComponentConfiguration.getRequestTimeout());
        }

        if (webMvcProperties.getAsync().getRequestTimeout() != null &&
                platformHttpComponentConfiguration.getRequestTimeout() == null) {
            platformHttpComponentConfiguration.setRequestTimeout(webMvcProperties.getAsync().getRequestTimeout().toMillis());
        }
    }
}
