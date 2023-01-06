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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.properties.PropertiesParser;
import org.apache.camel.main.DefaultConfigurationConfigurer;
import org.apache.camel.main.DefaultRoutesCollector;
import org.apache.camel.main.RoutesCollector;
import org.apache.camel.model.Model;
import org.apache.camel.spi.BeanRepository;
import org.apache.camel.spi.CliConnector;
import org.apache.camel.spi.CliConnectorFactory;
import org.apache.camel.spi.StartupStepRecorder;
import org.apache.camel.spring.spi.ApplicationContextBeanRepository;
import org.apache.camel.spring.spi.CamelBeanPostProcessor;
import org.apache.camel.support.DefaultRegistry;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.support.startup.LoggingStartupStepRecorder;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.core.OrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CamelConfigurationProperties.class)
@Import(TypeConversionConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class CamelAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CamelAutoConfiguration.class);

    /**
     * Spring-aware Camel context for the application. Auto-detects and loads all routes available in the Spring context.
     */
    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case CamelContext::shutdown or CamelContext::stop would
    // be used for bean destruction. As SpringCamelContext is a lifecycle
    // bean (implements Lifecycle) additional invocations of shutdown or
    // close would be superfluous.
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(CamelContext.class)
    CamelContext camelContext(ApplicationContext applicationContext,
                              CamelConfigurationProperties config,
                              CamelBeanPostProcessor beanPostProcessor) throws Exception {
        CamelContext camelContext = new SpringBootCamelContext(applicationContext, config.isWarnOnEarlyShutdown());
        // bean post processor is created before CamelContext
        beanPostProcessor.setCamelContext(camelContext);
        camelContext.adapt(ExtendedCamelContext.class).setBeanPostProcessor(beanPostProcessor);
        return doConfigureCamelContext(applicationContext, camelContext, config);
    }

    /**
     * Not to be used by Camel end users
     */
    public static CamelContext doConfigureCamelContext(ApplicationContext applicationContext,
                                                CamelContext camelContext,
                                                CamelConfigurationProperties config) throws Exception {

        // setup startup recorder before building context
        configureStartupRecorder(camelContext, config);

        camelContext.build();

        // initialize properties component eager
        PropertiesComponent pc = applicationContext.getBeanProvider(PropertiesComponent.class).getIfAvailable();
        if (pc != null) {
            pc.setCamelContext(camelContext);
            camelContext.setPropertiesComponent(pc);
        }

        final Map<String, BeanRepository> repositories = applicationContext.getBeansOfType(BeanRepository.class);
        if (!repositories.isEmpty()) {
            List<BeanRepository> reps = new ArrayList<>();
            // include default bean repository as well
            reps.add(new ApplicationContextBeanRepository(applicationContext));
            // and then any custom
            reps.addAll(repositories.values());
            // sort by ordered
            OrderComparator.sort(reps);
            // and plugin as new registry
            camelContext.adapt(ExtendedCamelContext.class).setRegistry(new DefaultRegistry(reps));
        }

        if (ObjectHelper.isNotEmpty(config.getFileConfigurations())) {
            Environment env = applicationContext.getEnvironment();
            if (env instanceof ConfigurableEnvironment) {
                MutablePropertySources sources = ((ConfigurableEnvironment) env).getPropertySources();
                if (!sources.contains("camel-file-configuration")) {
                    sources.addFirst(new FilePropertySource("camel-file-configuration", applicationContext, config.getFileConfigurations()));
                }
            }
        }

        // setup cli connector eager
        configureCliConnector(applicationContext, camelContext);

        camelContext.adapt(ExtendedCamelContext.class).setPackageScanClassResolver(new FatJarPackageScanClassResolver());
        camelContext.adapt(ExtendedCamelContext.class).setPackageScanResourceResolver(new FatJarPackageScanResourceResolver());

        if (config.getRouteFilterIncludePattern() != null || config.getRouteFilterExcludePattern() != null) {
            LOG.info("Route filtering pattern: include={}, exclude={}", config.getRouteFilterIncludePattern(), config.getRouteFilterExcludePattern());
            camelContext.getExtension(Model.class).setRouteFilterPattern(config.getRouteFilterIncludePattern(), config.getRouteFilterExcludePattern());
        }

        // configure the common/default options
        DefaultConfigurationConfigurer.configure(camelContext, config);
        // lookup and configure SPI beans
        DefaultConfigurationConfigurer.afterConfigure(camelContext);
        // and call after all properties are set
        DefaultConfigurationConfigurer.afterPropertiesSet(camelContext);

        return camelContext;
    }

    static void configureCliConnector(ApplicationContext applicationContext,
                                      CamelContext camelContext) {

        // factory is bound eager into spring bean registry
        try {
            CliConnectorFactory ccf = applicationContext.getBean(CliConnectorFactory.class);
            CliConnector connector = ccf.createConnector();
            camelContext.addService(connector, true);
            // force start cli connector early as otherwise it will be deferred until context is started
            // but, we want status available during startup phase
            ServiceHelper.startService(connector);
        } catch (BeansException e) {
            // optional so ignore
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeException(e);
        }
    }

    static void configureStartupRecorder(CamelContext camelContext, CamelConfigurationProperties config) {
        if ("false".equals(config.getStartupRecorder())) {
            camelContext.adapt(ExtendedCamelContext.class).getStartupStepRecorder().setEnabled(false);
        } else if ("logging".equals(config.getStartupRecorder())) {
            camelContext.adapt(ExtendedCamelContext.class).setStartupStepRecorder(new LoggingStartupStepRecorder());
        } else if ("java-flight-recorder".equals(config.getStartupRecorder())
                || config.getStartupRecorder() == null) {
            // try to auto discover camel-jfr to use
            StartupStepRecorder fr = camelContext.adapt(ExtendedCamelContext.class).getBootstrapFactoryFinder()
                    .newInstance(StartupStepRecorder.FACTORY, StartupStepRecorder.class).orElse(null);
            if (fr != null) {
                LOG.debug("Discovered startup recorder: {} from classpath", fr);
                fr.setRecording(config.isStartupRecorderRecording());
                fr.setStartupRecorderDuration(config.getStartupRecorderDuration());
                fr.setRecordingProfile(config.getStartupRecorderProfile());
                fr.setMaxDepth(config.getStartupRecorderMaxDepth());
                camelContext.adapt(ExtendedCamelContext.class).setStartupStepRecorder(fr);
            }
        }
    }

    @Bean
    CamelSpringBootApplicationController applicationController(ApplicationContext applicationContext, CamelContext camelContext) {
        return new CamelSpringBootApplicationController(applicationContext, camelContext);
    }

    @Bean
    @ConditionalOnMissingBean(RoutesCollector.class)
    @ConditionalOnMissingClass("org.apache.camel.spring.boot.endpointdsl.EndpointDslRouteCollector")
    RoutesCollector routesCollector() {
        // default collector works with spring boot
        return new DefaultRoutesCollector();
    }

    @Bean
    @ConditionalOnMissingBean(CamelSpringBootApplicationListener.class)
    CamelSpringBootApplicationListener routesCollectorListener(ApplicationContext applicationContext, CamelConfigurationProperties config,
                                                               RoutesCollector routesCollector) {
        Collection<CamelContextConfiguration> configurations = applicationContext.getBeansOfType(CamelContextConfiguration.class).values();
        return new CamelSpringBootApplicationListener(applicationContext, new ArrayList(configurations), config, routesCollector);
    }

    /**
     * Default fluent producer template for the bootstrapped Camel context.
     * Create the bean lazy as it should only be created if its in-use.
     */
    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case Service::close (FluentProducerTemplate implements Service)
    // would be used for bean destruction. And we want Camel to handle the
    // lifecycle.
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(FluentProducerTemplate.class)
    @Lazy
    FluentProducerTemplate fluentProducerTemplate(CamelContext camelContext,
                                                 CamelConfigurationProperties config) throws Exception {
        final FluentProducerTemplate fluentProducerTemplate = camelContext.createFluentProducerTemplate(config.getProducerTemplateCacheSize());
        // we add this fluentProducerTemplate as a Service to CamelContext so that it performs proper lifecycle (start and stop)
        camelContext.addService(fluentProducerTemplate);
        return fluentProducerTemplate;
    }

    /**
     * Default producer template for the bootstrapped Camel context.
     * Create the bean lazy as it should only be created if its in-use.
     */
    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case Service::close (ProducerTemplate implements Service)
    // would be used for bean destruction. And we want Camel to handle the
    // lifecycle.
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(ProducerTemplate.class)
    @Lazy
    ProducerTemplate producerTemplate(CamelContext camelContext,
                                      CamelConfigurationProperties config) throws Exception {
        final ProducerTemplate producerTemplate = camelContext.createProducerTemplate(config.getProducerTemplateCacheSize());
        // we add this producerTemplate as a Service to CamelContext so that it performs proper lifecycle (start and stop)
        camelContext.addService(producerTemplate);
        return producerTemplate;
    }

    /**
     * Default consumer template for the bootstrapped Camel context.
     * Create the bean lazy as it should only be created if its in-use.
     */
    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case Service::close (ConsumerTemplate implements Service)
    // would be used for bean destruction. And we want Camel to handle the
    // lifecycle.
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(ConsumerTemplate.class)
    @Lazy
    ConsumerTemplate consumerTemplate(CamelContext camelContext,
                                      CamelConfigurationProperties config) throws Exception {
        final ConsumerTemplate consumerTemplate = camelContext.createConsumerTemplate(config.getConsumerTemplateCacheSize());
        // we add this consumerTemplate as a Service to CamelContext so that it performs proper lifecycle (start and stop)
        camelContext.addService(consumerTemplate);
        return consumerTemplate;
    }

    // SpringCamelContext integration

    @Bean
    @ConditionalOnMissingBean(PropertiesParser.class)
    PropertiesParser propertiesParser() {
        return new SpringPropertiesParser();
    }

    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case ShutdownableService::shutdown/Service::close
    // (PropertiesComponent extends ServiceSupport) would be used for bean
    // destruction. And we want Camel to handle the lifecycle.
    @Bean(destroyMethod = "")
    PropertiesComponent properties(PropertiesParser parser) {
        PropertiesComponent pc = new PropertiesComponent();
        pc.setPropertiesParser(parser);
        return pc;
    }

    /**
     * Camel post processor - required to support Camel annotations.
     */
    @Bean
    CamelBeanPostProcessor camelBeanPostProcessor(ApplicationContext applicationContext) {
        return new CamelSpringBootBeanPostProcessor(applicationContext);
    }

}
