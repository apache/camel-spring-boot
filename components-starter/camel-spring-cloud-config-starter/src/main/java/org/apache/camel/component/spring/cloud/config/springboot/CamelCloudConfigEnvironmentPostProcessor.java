package org.apache.camel.component.spring.cloud.config.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class CamelCloudConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final Logger LOG = LoggerFactory.getLogger(CamelCloudConfigEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Do an early property load, this way the environment is loaded with properties
        // this post processor will be invoked later on by spring boot
        ConfigDataEnvironmentPostProcessor.applyTo(environment);

        // Map Camel Spring Config properties to Spring Boot specific properties
        Properties properties = new Properties();
        if (environment.containsProperty("camel.vault.spring-config.uris") && !environment.containsProperty("spring.config.import")) {
            LOG.debug("Converting Camel property \"{}\" to Spring Boot \"{}\"", "camel.vault.spring-config.uris", "spring.config.import");
            properties.put("spring.config.import", "optional:configserver:" + environment.getProperty("camel.vault.spring-config.uris"));
        }
        convertPropertyIfExists(environment, "spring.cloud.config.username", "camel.vault.spring-config.username", properties);
        convertPropertyIfExists(environment, "spring.cloud.config.password", "camel.vault.spring-config.password", properties);
        convertPropertyIfExists(environment, "spring.cloud.config.label", "camel.vault.spring-config.label", properties);
        convertPropertyIfExists(environment, "spring.cloud.config.token", "camel.vault.spring-config.token", properties);
        convertPropertyIfExists(environment, "spring.cloud.config.profile", "camel.vault.spring-config.profile", properties);

        environment.getPropertySources().addFirst(new PropertiesPropertySource("overridden-camel-spring-cloud-config-properties", properties));
    }

    private void convertPropertyIfExists(ConfigurableEnvironment environment,
                                         String springPropertyName,
                                         String camelPropertyName,
                                         Properties properties) {
        if (environment.containsProperty(camelPropertyName) && !environment.containsProperty(springPropertyName)) {
            LOG.debug("Converting Camel property \"{}\" to Spring Boot \"{}\"", camelPropertyName, springPropertyName);
            properties.put(springPropertyName, environment.getProperty(camelPropertyName));
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }
}
