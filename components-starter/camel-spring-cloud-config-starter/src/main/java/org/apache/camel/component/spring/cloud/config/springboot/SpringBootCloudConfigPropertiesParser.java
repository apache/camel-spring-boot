package org.apache.camel.component.spring.cloud.config.springboot;

import org.apache.camel.component.spring.cloud.config.SpringCloudConfigPropertiesFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Properties;

public class SpringBootCloudConfigPropertiesParser implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootCloudConfigPropertiesParser.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Properties properties = new Properties();
        ConfigurableEnvironment environment = event.getEnvironment();

        if (Boolean.parseBoolean(environment.getProperty("camel.component.spring-cloud-config.early-resolve-properties"))) {
            SpringCloudConfigPropertiesFunction springCloudConfigPropertiesFunction = new SpringCloudConfigPropertiesFunction();
            springCloudConfigPropertiesFunction.setEnvironment(environment);
            for (PropertySource mutablePropertySources : event.getEnvironment().getPropertySources()) {
                if (mutablePropertySources instanceof MapPropertySource mapPropertySource) {
                    mapPropertySource.getSource().forEach((key, value) -> {
                        String stringValue = null;
                        if ((value instanceof OriginTrackedValue originTrackedValue &&
                                originTrackedValue.getValue() instanceof String v)) {
                            stringValue = v;
                        } else if (value instanceof String v) {
                            stringValue = v;
                        }
                        if (stringValue != null &&
                                stringValue.startsWith("{{spring-config:") &&
                                stringValue.endsWith("}}")) {
                            LOG.debug("decrypting and overriding property {}", key);
                            try {
                                String element = springCloudConfigPropertiesFunction.apply(stringValue
                                        .replace("{{spring-config:", "")
                                        .replace("}}", ""));
                                properties.put(key, element);
                            } catch (Exception e) {
                                // Log and do nothing
                                LOG.debug("failed to parse property {}. This exception is ignored.", key, e);
                            }
                        }
                    });
                }
            }
            environment.getPropertySources().addFirst(new PropertiesPropertySource("overridden-camel-spring-config-properties", properties));
        }
    }
}
