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
package org.apache.camel.spring.boot.routetemplate;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.main.PropertiesRouteTemplateParametersSource;
import org.apache.camel.spi.RouteTemplateParameterSource;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(CamelAutoConfiguration.class)
@EnableConfigurationProperties(CamelRouteTemplateConfigurationProperties.class)
@AutoConfigureAfter(CamelAutoConfiguration.class)
public class CamelRouteTemplateAutoConfiguration {

    @Bean
    public RouteTemplateParameterSource routeTemplate(CamelContext camelContext, CamelRouteTemplateConfigurationProperties rt) {
        if (rt.getConfig() == null) {
            return null;
        }

        PropertiesRouteTemplateParametersSource source = new PropertiesRouteTemplateParametersSource();
        int counter = 0;
        for (Map<String, String> e : rt.getConfig()) {
            for (Map.Entry<String, String> entry : e.entrySet()) {
                source.addParameter(String.valueOf(counter), entry.getKey(), entry.getValue());
            }
            counter++;
        }

        camelContext.getRegistry().bind("CamelSpringBootRouteTemplateParametersSource", RouteTemplateParameterSource.class, source);

        return source;
    }

}
