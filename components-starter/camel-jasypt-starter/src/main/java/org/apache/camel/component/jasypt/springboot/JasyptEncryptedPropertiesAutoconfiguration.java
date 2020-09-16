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
package org.apache.camel.component.jasypt.springboot;

import org.apache.camel.component.properties.PropertiesParser;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.iv.IvGenerator;
import org.jasypt.iv.NoIvGenerator;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.salt.SaltGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;

import java.lang.annotation.Annotation;

import static org.apache.camel.component.jasypt.springboot.JasyptEncryptedPropertiesConfiguration.PREFIX;
import static org.apache.camel.component.jasypt.springboot.JasyptEncryptedPropertiesUtils.isIVNeeded;
import static org.apache.camel.util.ObjectHelper.isNotEmpty;
import static org.apache.camel.util.StringHelper.after;
import static org.springframework.boot.context.properties.source.ConfigurationPropertySources.from;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;


@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "camel.component.jasypt.enabled", matchIfMissing = true)
@AutoConfigureBefore(CamelAutoConfiguration.class)
public class JasyptEncryptedPropertiesAutoconfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(JasyptEncryptedPropertiesAutoconfiguration.class);

    private static final String SYSTEM_ENVIRONMENT_PREFIX = "sysenv:";

    private static final String SYSTEM_PROPERTIES_PREFIX = "sys:";

    @Bean
    public JasyptEncryptedPropertiesConfiguration JasyptEncryptedPropertiesAutoconfiguration(final ConfigurableEnvironment environment) {
        JasyptEncryptedPropertiesConfiguration config = new JasyptEncryptedPropertiesConfiguration();
        final BindHandler handler = new IgnoreErrorsBindHandler(BindHandler.DEFAULT);
        final MutablePropertySources propertySources = environment.getPropertySources();
        PropertySourcesPlaceholdersResolver propertyResolver = new PropertySourcesPlaceholdersResolver(propertySources);
        Iterable<ConfigurationPropertySource> configurationPropertySources = from(propertySources);
        ConversionService conversionService = ApplicationConversionService.getSharedInstance();
        final Binder binder = new Binder(configurationPropertySources, propertyResolver, conversionService);
        final ResolvableType type = forClass(JasyptEncryptedPropertiesConfiguration.class);
        final Annotation annotation = findAnnotation(JasyptEncryptedPropertiesConfiguration.class, ConfigurationProperties.class);
        final Annotation[] annotations = new Annotation[]{annotation};
        final Bindable<?> target = Bindable.of(type).withExistingValue(config).withAnnotations(annotations);
        binder.bind(PREFIX, target, handler);
        return config;
    }

    @Bean
    @ConditionalOnMissingBean(EnvironmentStringPBEConfig.class)
    public EnvironmentStringPBEConfig environmentVariablesConfiguration(JasyptEncryptedPropertiesConfiguration configuration) {
        EnvironmentStringPBEConfig environmentStringPBEConfig = new EnvironmentStringPBEConfig();
        environmentStringPBEConfig.setAlgorithm(configuration.getAlgorithm());
        environmentStringPBEConfig.setIvGenerator(getIVGenerator(configuration));
        environmentStringPBEConfig.setSaltGenerator(getSaltGenerator(configuration));
        environmentStringPBEConfig.setProviderClassName(configuration.getProviderClassName());
        parsePassword(environmentStringPBEConfig, configuration);
        return environmentStringPBEConfig;
    }

    @Bean
    @ConditionalOnMissingBean(StringEncryptor.class)
    public StringEncryptor stringEncryptor(EnvironmentStringPBEConfig environmentVariablesConfiguration) {
        StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
        standardPBEStringEncryptor.setConfig(environmentVariablesConfiguration);
        return standardPBEStringEncryptor;
    }

    @Bean
    public EncryptablePropertySourcesPlaceholderConfigurer propertyConfigurer(StringEncryptor stringEncryptor) {
        return new EncryptablePropertySourcesPlaceholderConfigurer(stringEncryptor);
    }

    /*
        This bean override the default org.apache.camel.spring.boot.SpringPropertiesParser
        and allow the use of encrypted properties inside the camel context.
     */
    @Bean
    public PropertiesParser encryptedPropertiesParser(PropertyResolver propertyResolver, StringEncryptor stringEncryptor) {
        return new JasyptSpringEncryptedPropertiesParser(propertyResolver, stringEncryptor);
    }

    public SaltGenerator getSaltGenerator(JasyptEncryptedPropertiesConfiguration configuration) {
        String saltGeneratorClassName = configuration.getSaltGeneratorClassName();
        SaltGenerator saltGenerator = loadClass(saltGeneratorClassName);
        if (saltGenerator != null) {
            return saltGenerator;
        }
        return new RandomSaltGenerator();
    }

    private IvGenerator getIVGenerator(JasyptEncryptedPropertiesConfiguration configuration) {
        String ivGeneratorClassName = configuration.getIvGeneratorClassName();
        IvGenerator ivGenerator = loadClass(ivGeneratorClassName);
        if (ivGenerator != null) {
            return ivGenerator;
        }
        String algorithm = configuration.getAlgorithm();
        return isIVNeeded(algorithm) ? new RandomIvGenerator() : new NoIvGenerator();
    }

    private <T> T loadClass(String className) {
        try {
            final Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            return (T) clazz.newInstance();
        } catch (Exception e) {
            throw new EncryptionInitializationException(e);
        }
    }

    private void parsePassword(EnvironmentStringPBEConfig environmentStringPBEConfig, JasyptEncryptedPropertiesConfiguration configuration) {
        String passwordReference = configuration.getPassword();
        if (isNotEmpty(passwordReference) && passwordReference.startsWith(SYSTEM_ENVIRONMENT_PREFIX)) {
            environmentStringPBEConfig.setPasswordEnvName(after(passwordReference, SYSTEM_ENVIRONMENT_PREFIX));
            return;
        }
        if (isNotEmpty(passwordReference) && passwordReference.startsWith(SYSTEM_PROPERTIES_PREFIX)) {
            environmentStringPBEConfig.setPasswordSysPropertyName(after(passwordReference, SYSTEM_PROPERTIES_PREFIX));
            return;
        }
        environmentStringPBEConfig.setPassword(passwordReference);
    }
}
