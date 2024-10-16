package org.apache.camel.component.google.secret.manager.springboot;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.google.secret.manager.GoogleSecretManagerPropertiesFunction;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.util.Properties;

public class SpringBootGoogleSecretManagerPropertiesParser implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootGoogleSecretManagerPropertiesParser.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        SecretManagerServiceClient client;
        ConfigurableEnvironment environment = event.getEnvironment();
        String projectId;
        if (Boolean.parseBoolean(environment.getProperty("camel.component.google-secret-manager.early-resolve-properties"))) {
            projectId = environment.getProperty("camel.vault.gcp.projectId");
            boolean useDefaultInstance = Boolean.parseBoolean(environment.getProperty("camel.vault.gcp.useDefaultInstance"));
            if (useDefaultInstance && ObjectHelper.isNotEmpty(projectId)) {
                SecretManagerServiceSettings settings = null;
                try {
                    settings = SecretManagerServiceSettings.newBuilder().build();
                    client = SecretManagerServiceClient.create(settings);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeCamelException(
                        "Using the GCP Secret Manager Properties Function in Spring Boot early resolver mode requires setting GCP project Id as application properties and use default instance option to true");
            }
            GoogleSecretManagerPropertiesFunction secretsManagerPropertiesFunction = new GoogleSecretManagerPropertiesFunction(client, projectId);
            final Properties props = new Properties();
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
                                stringValue.startsWith("{{gcp:") &&
                                stringValue.endsWith("}}")) {
                            LOG.debug("decrypting and overriding property {}", key);
                            try {
                                String element = secretsManagerPropertiesFunction.apply(stringValue
                                        .replace("{{gcp:", "")
                                        .replace("}}", ""));
                                props.put(key, element);
                            } catch (Exception e) {
                                // Log and do nothing
                                LOG.debug("failed to parse property {}. This exception is ignored.", key, e);
                            }
                        }
                    });
                }
            }
            environment.getPropertySources().addFirst(new PropertiesPropertySource("overridden-camel-google-secret-manager-properties", props));
        }
    }
}
