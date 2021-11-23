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
package org.apache.camel.spring.boot.xml;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.engine.DefaultInjector;
import org.apache.camel.spi.Injector;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelConfigurationProperties;
import org.apache.camel.spring.spi.SpringInjector;
import org.apache.camel.spring.xml.XmlCamelContextConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Used to merge Camel Spring Boot configuration with {@link org.apache.camel.CamelContext} that
 * has been created from XML files. This allows to configure your Camel applications with Spring Boot
 * configuration for both Java and XML Camel routes in similar way.
 */
public class SpringBootXmlCamelContextConfigurer implements XmlCamelContextConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootXmlCamelContextConfigurer.class);

    @Override
    public void configure(ApplicationContext applicationContext, SpringCamelContext camelContext) {
        CamelConfigurationProperties config = applicationContext.getBean(CamelConfigurationProperties.class);
        Injector injector = camelContext.getInjector();
        try {
            LOG.debug("Merging XML based CamelContext with Spring Boot configuration properties");
            // spring boot is not capable at this phase to use an injector that is creating beans
            // via spring-boot itself, so use a default injector instead
            camelContext.setInjector(new DefaultInjector(camelContext));
            CamelAutoConfiguration.doConfigureCamelContext(applicationContext, camelContext, config);
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        } finally {
            // restore original injector
            camelContext.setInjector(injector);
        }
    }
}
