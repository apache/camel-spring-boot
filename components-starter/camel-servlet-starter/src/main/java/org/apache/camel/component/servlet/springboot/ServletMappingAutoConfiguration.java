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
package org.apache.camel.component.servlet.springboot;

import java.util.Map;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.component.servlet.ServletComponent;
import org.apache.camel.spring.boot.util.RemovedPropertiesHelper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

/**
 * Servlet mapping auto-configuration.
 */
@AutoConfiguration(afterName = "org.apache.camel.spring.boot.CamelAutoConfiguration")
@ConditionalOnProperty(name = "camel.servlet.mapping.enabled", matchIfMissing = true)
@ConditionalOnBean(type = "org.apache.camel.spring.boot.CamelAutoConfiguration")
@ConditionalOnWebApplication
@EnableConfigurationProperties({ ServletMappingConfiguration.class, MultipartProperties.class })
public class ServletMappingAutoConfiguration {

    /**
     * Camel servlet
     */
    @Bean
    CamelHttpTransportServlet camelHttpTransportServlet() {
        CamelHttpTransportServlet servlet = new CamelHttpTransportServlet();
        return servlet;
    }

    /**
     * Spring Boot servlet registration with the Camel server
     */
    @Bean
    ServletRegistrationBean camelServletRegistrationBean(CamelHttpTransportServlet servlet,
            ServletMappingConfiguration config, MultipartProperties multipartProperties, Environment environment) {
        warnRemovedMappingProperties(environment);
        ServletRegistrationBean mapping = new ServletRegistrationBean();
        mapping.setServlet(servlet);
        mapping.addUrlMappings(config.getContextPath());
        mapping.setName(config.getServletName());
        mapping.setLoadOnStartup(1);
        if (multipartProperties != null && multipartProperties.isEnabled()) {
            mapping.setMultipartConfig(multipartProperties.createMultipartConfig());
        }
        return mapping;
    }

    /**
     * Logs a WARN for each camel.component.servlet.mapping.* option the application sets, with the option to use
     * instead. These options were renamed to camel.servlet.mapping.* in Camel 3.10, and Spring Boot ignores the old
     * names without a log line.
     */
    static void warnRemovedMappingProperties(Environment environment) {
        Map<String, String> options = RemovedPropertiesHelper.dashedPropertyNames(ServletMappingConfiguration.class);
        RemovedPropertiesHelper.warnRemovedProperties(environment, "camel.component.servlet.mapping",
                "the servlet mapping options were renamed to camel.servlet.mapping in Camel 3.10", Set.of(),
                name -> options.containsKey(name) ? "camel.servlet.mapping." + options.get(name) : null);
    }

    /**
     * Ensures the Camel Servlet component is automatic created if no custom exists.
     */
    @Lazy
    @Bean(name = "servlet-component")
    @ConditionalOnMissingBean(ServletComponent.class)
    public ServletComponent configureServletComponent(CamelContext camelContext) throws Exception {
        ServletComponent component = new ServletComponent();
        component.setCamelContext(camelContext);
        return component;
    }

}
