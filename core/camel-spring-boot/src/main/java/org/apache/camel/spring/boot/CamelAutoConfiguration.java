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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ContextEvents;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.clock.Clock;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.properties.PropertiesParser;
import org.apache.camel.main.DebuggerConfigurationProperties;
import org.apache.camel.main.DefaultConfigurationConfigurer;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.RoutesCollector;
import org.apache.camel.model.Model;
import org.apache.camel.spi.BeanRepository;
import org.apache.camel.spi.CliConnector;
import org.apache.camel.spi.CliConnectorFactory;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.PackageScanResourceResolver;
import org.apache.camel.spi.StartupConditionStrategy;
import org.apache.camel.spi.StartupStepRecorder;
import org.apache.camel.spi.VariableRepository;
import org.apache.camel.spi.VariableRepositoryFactory;
import org.apache.camel.spring.boot.aot.CamelRuntimeHints;
import org.apache.camel.spring.spi.ApplicationContextBeanRepository;
import org.apache.camel.spring.spi.CamelBeanPostProcessor;
import org.apache.camel.support.DefaultRegistry;
import org.apache.camel.support.ResetableClock;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.support.startup.DefaultStartupConditionStrategy;
import org.apache.camel.support.startup.EnvStartupCondition;
import org.apache.camel.support.startup.FileStartupCondition;
import org.apache.camel.support.startup.LoggingStartupStepRecorder;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
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
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.core.OrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

@ImportRuntimeHints(CamelRuntimeHints.class)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CamelConfigurationProperties.class, CamelStartupConditionConfigurationProperties.class, PropertiesComponentConfiguration.class})
@Import(TypeConversionConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class CamelAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CamelAutoConfiguration.class);

    /**
     * Spring-aware Camel context for the application. Auto-detects and loads all routes available in the Spring
     * context.
     */
    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case CamelContext::shutdown or CamelContext::stop would
    // be used for bean destruction. As SpringCamelContext is a lifecycle
    // bean (implements Lifecycle) additional invocations of shutdown or
    // close would be superfluous.
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(CamelContext.class)
    CamelContext camelContext(ApplicationContext applicationContext, CamelConfigurationProperties config,
                              CamelBeanPostProcessor beanPostProcessor, StartupConditionStrategy startup) throws Exception {
        Clock clock = new ResetableClock();
        CamelSpringBootApplicationController controller = new CamelSpringBootApplicationController(applicationContext);
        CamelContext camelContext = new SpringBootCamelContext(applicationContext,
                config.getMain().isWarnOnEarlyShutdown(), controller);
        camelContext.getClock().add(ContextEvents.BOOT, clock);
        controller.setCamelContext(camelContext);
        // bean post processor is created before CamelContext
        beanPostProcessor.setCamelContext(camelContext);
        camelContext.getCamelContextExtension().addContextPlugin(CamelBeanPostProcessor.class, beanPostProcessor);
        // startup condition is created before CamelContext
        camelContext.getCamelContextExtension().addContextPlugin(StartupConditionStrategy.class, startup);
        return doConfigureCamelContext(applicationContext, camelContext, config, controller);
    }

    /**
     * Not to be used by Camel end users
     */
    public static CamelContext doConfigureCamelContext(ApplicationContext applicationContext, CamelContext camelContext,
                                                       CamelConfigurationProperties config,
                                                       CamelSpringBootApplicationController controller) throws Exception {

        // inject camel context on controller
        CamelContextAware.trySetCamelContext(controller, camelContext);

        // setup startup recorder before building context
        configureStartupRecorder(camelContext, config);

        camelContext.build();

        var listeners = controller.getMain().getMainListeners();
        if (!listeners.isEmpty()) {
            for (MainListener listener : listeners) {
                listener.beforeInitialize(controller.getMain());
            }
            // allow doing custom configuration before camel is started
            for (MainListener listener : listeners) {
                listener.beforeConfigure(controller.getMain());
            }
        }

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
            camelContext.getCamelContextExtension().setRegistry(new DefaultRegistry(reps));
        }

        if (ObjectHelper.isNotEmpty(config.getMain().getFileConfigurations())) {
            Environment env = applicationContext.getEnvironment();
            if (env instanceof ConfigurableEnvironment) {
                MutablePropertySources sources = ((ConfigurableEnvironment) env).getPropertySources();
                if (!sources.contains("camel-file-configuration")) {
                    sources.addFirst(new FilePropertySource("camel-file-configuration", applicationContext,
                            config.getMain().getFileConfigurations()));
                }
            }
        }

        // configure camel.variable.xx configurations
        Environment env = applicationContext.getEnvironment();
        if (env instanceof ConfigurableEnvironment cev) {
            Map<String, String> vars = doExtractVariablesFromSpringBoot(cev);
            if (!vars.isEmpty()) {
                // set variables
                for (String key : vars.keySet()) {
                    String value = vars.get(key);
                    String id = StringHelper.before(key, ":", "global");
                    key = StringHelper.after(key, ":", key);
                    VariableRepository repo = camelContext.getCamelContextExtension()
                            .getContextPlugin(VariableRepositoryFactory.class).getVariableRepository(id);
                    // it may be a resource to load from disk then
                    if (ResourceHelper.hasScheme(value)) {
                        InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(camelContext, value);
                        value = IOHelper.loadText(is);
                        IOHelper.close(is);
                    }
                    repo.setVariable(key, value);
                }
            }
        }

        // setup debugger eager
        configureDebugger(applicationContext, camelContext);
        // setup cli connector eager
        configureCliConnector(applicationContext, camelContext);

        camelContext.getCamelContextExtension().addContextPlugin(PackageScanClassResolver.class,
                new FatJarPackageScanClassResolver());
        camelContext.getCamelContextExtension().addContextPlugin(PackageScanResourceResolver.class,
                new FatJarPackageScanResourceResolver());

        if (config.getMain().getRouteFilterIncludePattern() != null
            || config.getMain().getRouteFilterExcludePattern() != null) {
            LOG.info("Route filtering pattern: include={}, exclude={}", config.getMain().getRouteFilterIncludePattern(),
                    config.getMain().getRouteFilterExcludePattern());
            camelContext.getCamelContextExtension().getContextPlugin(Model.class).setRouteFilterPattern(
                    config.getMain().getRouteFilterIncludePattern(), config.getMain().getRouteFilterExcludePattern());
        }

        // configure the common/default options
        DefaultConfigurationConfigurer.configure(camelContext, config.getMain());
        // lookup and configure SPI beans
        DefaultConfigurationConfigurer.afterConfigure(camelContext);
        // and call after all properties are set
        DefaultConfigurationConfigurer.afterPropertiesSet(camelContext);

        for (MainListener listener : listeners) {
            listener.afterConfigure(controller.getMain());
        }

        return camelContext;
    }

    protected static Map<String, String> doExtractVariablesFromSpringBoot(ConfigurableEnvironment env) {
        Map<String, String> answer = new LinkedHashMap<>();

        // grab all variables
        env.getPropertySources().forEach(ps -> {
            if (ps instanceof EnumerablePropertySource eps) {
                for (String n : eps.getPropertyNames()) {
                    if (n.startsWith("camel.variable.")) {
                        String v = env.getRequiredProperty(n);
                        n = n.substring(15);
                        answer.put(n, v);
                    }
                }
            }
        });

        return answer;
    }

    static void configureDebugger(ApplicationContext applicationContext, CamelContext camelContext) {
        try {
            DebuggerConfigurationProperties debug = applicationContext.getBean(DebuggerConfigurationProperties.class);
            DefaultConfigurationConfigurer.configureBacklogDebugger(camelContext, debug);
        } catch (BeansException e) {
            // optional so ignore
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeException(e);
        }
    }

    static void configureCliConnector(ApplicationContext applicationContext, CamelContext camelContext) {
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
        if ("false".equals(config.getMain().getStartupRecorder())) {
            camelContext.getCamelContextExtension().getStartupStepRecorder().setEnabled(false);
        } else if ("logging".equals(config.getMain().getStartupRecorder())) {
            camelContext.getCamelContextExtension().setStartupStepRecorder(new LoggingStartupStepRecorder());
        } else if ("java-flight-recorder".equals(config.getMain().getStartupRecorder())
                   || config.getMain().getStartupRecorder() == null) {
            // try to auto discover camel-jfr to use
            StartupStepRecorder fr = camelContext.getCamelContextExtension().getBootstrapFactoryFinder()
                    .newInstance(StartupStepRecorder.FACTORY, StartupStepRecorder.class).orElse(null);
            if (fr != null) {
                LOG.debug("Discovered startup recorder: {} from classpath", fr);
                fr.setRecording(config.getMain().isStartupRecorderRecording());
                fr.setStartupRecorderDuration(config.getMain().getStartupRecorderDuration());
                fr.setRecordingProfile(config.getMain().getStartupRecorderProfile());
                fr.setMaxDepth(config.getMain().getStartupRecorderMaxDepth());
                camelContext.getCamelContextExtension().setStartupStepRecorder(fr);
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean(RoutesCollector.class)
    @ConditionalOnMissingClass("org.apache.camel.spring.boot.endpointdsl.EndpointDslRouteCollector")
    RoutesCollector routesCollector(ApplicationContext applicationContext, CamelConfigurationProperties config) {
        return new CamelSpringBootRoutesCollector(applicationContext, config.getMain().isIncludeNonSingletons());
    }

    @Bean
    @ConditionalOnMissingBean(CamelSpringBootApplicationListener.class)
    CamelSpringBootApplicationListener routesCollectorListener(ApplicationContext applicationContext,
                                                               CamelConfigurationProperties config, RoutesCollector routesCollector) {
        Collection<CamelContextConfiguration> configurations = applicationContext
                .getBeansOfType(CamelContextConfiguration.class).values();
        return new CamelSpringBootApplicationListener(applicationContext, new ArrayList(configurations), config,
                routesCollector);
    }

    /**
     * Default fluent producer template for the bootstrapped Camel context. Create the bean lazy as it should only be
     * created if its in-use.
     */
    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case Service::close (FluentProducerTemplate implements Service)
    // would be used for bean destruction. And we want Camel to handle the
    // lifecycle.
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(FluentProducerTemplate.class)
    @Lazy
    FluentProducerTemplate fluentProducerTemplate(CamelContext camelContext, CamelConfigurationProperties config)
            throws Exception {
        final FluentProducerTemplate fluentProducerTemplate = camelContext
                .createFluentProducerTemplate(config.getMain().getProducerTemplateCacheSize());
        // we add this fluentProducerTemplate as a Service to CamelContext so that it performs proper lifecycle (start
        // and stop)
        camelContext.addService(fluentProducerTemplate);
        return fluentProducerTemplate;
    }

    /**
     * Default producer template for the bootstrapped Camel context. Create the bean lazy as it should only be created
     * if its in-use.
     */
    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case Service::close (ProducerTemplate implements Service)
    // would be used for bean destruction. And we want Camel to handle the
    // lifecycle.
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(ProducerTemplate.class)
    @Lazy
    ProducerTemplate producerTemplate(CamelContext camelContext, CamelConfigurationProperties config) throws Exception {
        final ProducerTemplate producerTemplate = camelContext
                .createProducerTemplate(config.getMain().getProducerTemplateCacheSize());
        // we add this producerTemplate as a Service to CamelContext so that it performs proper lifecycle (start and
        // stop)
        camelContext.addService(producerTemplate);
        return producerTemplate;
    }

    /**
     * Default consumer template for the bootstrapped Camel context. Create the bean lazy as it should only be created
     * if its in-use.
     */
    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case Service::close (ConsumerTemplate implements Service)
    // would be used for bean destruction. And we want Camel to handle the
    // lifecycle.
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(ConsumerTemplate.class)
    @Lazy
    ConsumerTemplate consumerTemplate(CamelContext camelContext, CamelConfigurationProperties config) throws Exception {
        final ConsumerTemplate consumerTemplate = camelContext
                .createConsumerTemplate(config.getMain().getConsumerTemplateCacheSize());
        // we add this consumerTemplate as a Service to CamelContext so that it performs proper lifecycle (start and
        // stop)
        camelContext.addService(consumerTemplate);
        return consumerTemplate;
    }

    // SpringCamelContext integration

    @Bean
    @ConditionalOnMissingBean(PropertiesParser.class)
    PropertiesParser propertiesParser(Environment env) {
        return new SpringPropertiesParser(env);
    }

    // We explicitly declare the destroyMethod to be "" as the Spring @Bean
    // annotation defaults to AbstractBeanDefinition.INFER_METHOD otherwise
    // and in that case ShutdownableService::shutdown/Service::close
    // (PropertiesComponent extends ServiceSupport) would be used for bean
    // destruction. And we want Camel to handle the lifecycle.
    @Bean(destroyMethod = "")
    PropertiesComponent properties(ApplicationContext applicationContext, PropertiesParser parser, PropertiesComponentConfiguration configuration) {
        PropertiesComponent pc = new PropertiesComponent();
        if (configuration.getAutoDiscoverPropertiesSources() != null) {
            pc.setAutoDiscoverPropertiesSources(configuration.getAutoDiscoverPropertiesSources());
        }
        if (configuration.getDefaultFallbackEnabled() != null) {
            pc.setDefaultFallbackEnabled(configuration.getDefaultFallbackEnabled());
        }
        if (configuration.getEncoding() != null) {
            pc.setEncoding(configuration.getEncoding());
        }
        if (configuration.getEnvironmentVariableMode() != null) {
            pc.setEnvironmentVariableMode(configuration.getEnvironmentVariableMode());
        }
        if (configuration.getSystemPropertiesMode() != null) {
            pc.setSystemPropertiesMode(configuration.getSystemPropertiesMode());
        }
        if (configuration.getIgnoreMissingLocation() != null) {
            pc.setIgnoreMissingLocation(configuration.getIgnoreMissingLocation());
        }
        if (configuration.getNestedPlaceholder() != null) {
            pc.setNestedPlaceholder(configuration.getNestedPlaceholder());
        }
        if (configuration.getLocation() != null) {
            pc.setLocation(configuration.getLocation());
        }
        if (configuration.getInitialProperties() != null) {
            Properties prop = applicationContext.getBean(configuration.getInitialProperties(), Properties.class);
            pc.setInitialProperties(prop);
        }
        if (configuration.getOverrideProperties() != null) {
            Properties prop = applicationContext.getBean(configuration.getOverrideProperties(), Properties.class);
            pc.setOverrideProperties(prop);
        }
        if (configuration.getPropertiesParser() != null) {
            PropertiesParser pp = applicationContext.getBean(configuration.getPropertiesParser(), PropertiesParser.class);
            pc.setPropertiesParser(pp);
        } else {
            pc.setPropertiesParser(parser);
        }
        return pc;
    }

    /**
     * Camel post processor - required to support Camel annotations.
     */
    @Bean
    CamelBeanPostProcessor camelBeanPostProcessor(ApplicationContext applicationContext) {
        return new CamelSpringBootBeanPostProcessor(applicationContext);
    }

    /**
     * Camel startup strategy - used early by Camel
     */
    @Bean
    StartupConditionStrategy startupConditionStrategy(CamelStartupConditionConfigurationProperties config) {
        StartupConditionStrategy scs = new DefaultStartupConditionStrategy();
        scs.setEnabled(config.isEnabled());
        scs.setInterval(config.getInterval());
        scs.setTimeout(config.getTimeout());
        scs.setOnTimeout(config.getOnTimeout());
        String envExist = config.getEnvironmentVariableExists();
        if (envExist != null) {
            scs.addStartupCondition(new EnvStartupCondition(envExist));
        }
        String file = config.getFileExists();
        if (file != null) {
            scs.addStartupCondition(new FileStartupCondition(file));
        }
        scs.addStartupConditions(config.getCustomClassNames());
        return scs;
    }

}
