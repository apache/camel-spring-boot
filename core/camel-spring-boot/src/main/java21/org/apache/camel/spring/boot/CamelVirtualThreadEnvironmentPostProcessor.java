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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Environment post processor that automatically sets the Camel virtual thread 
 * system property when Spring Boot virtual threads are enabled.
 * 
 * This processor runs very early in the Spring Boot startup process, before
 * any Camel classes are loaded, ensuring that Camel's ThreadType static 
 * initialization picks up the correct virtual thread configuration.
 */
public class CamelVirtualThreadEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CamelVirtualThreadEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Check if Spring Boot virtual threads are enabled
        String springVirtualThreads = environment.getProperty("spring.threads.virtual.enabled");
        
        if ("true".equalsIgnoreCase(springVirtualThreads)) {
            // Set the Camel virtual threads system property early, before Camel classes are loaded
            String existingCamelProperty = System.getProperty("camel.threads.virtual.enabled");
            
            if (existingCamelProperty == null) {
                System.setProperty("camel.threads.virtual.enabled", "true");
                LOG.info("Spring virtual threads enabled - automatically setting camel.threads.virtual.enabled=true");
            } else {
                LOG.debug("camel.threads.virtual.enabled already set to: {}", existingCamelProperty);
            }
        }
    }
}