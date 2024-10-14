package org.apache.camel.component.aws.secretsmanager.springboot;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerPropertiesFunction;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

import java.util.Properties;

public class SpringBootAwsSecretsManagerPropertiesParser implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SpringBootAwsSecretsManagerPropertiesParser.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        SecretsManagerClient client;
        ConfigurableEnvironment environment = event.getEnvironment();
        if (Boolean.parseBoolean(environment.getProperty("camel.component.aws-secrets-manager.early-resolve-properties"))) {
            String accessKey = environment.getProperty("camel.vault.aws.accessKey");
            String secretKey = environment.getProperty("camel.vault.aws.secretKey");
            String region = environment.getProperty("camel.vault.aws.region");
            boolean useDefaultCredentialsProvider = Boolean.parseBoolean(environment.getProperty("camel.vault.aws.defaultCredentialsProvider"));
            boolean useProfileCredentialsProvider = Boolean.parseBoolean(environment.getProperty("camel.vault.aws.profileCredentialsProvider"));
            String profileName = environment.getProperty("camel.vault.aws.profileName");
            if (ObjectHelper.isNotEmpty(accessKey) && ObjectHelper.isNotEmpty(secretKey) && ObjectHelper.isNotEmpty(region)) {
                SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder();
                AwsBasicCredentials cred = AwsBasicCredentials.create(accessKey, secretKey);
                clientBuilder = clientBuilder.credentialsProvider(StaticCredentialsProvider.create(cred));
                clientBuilder.region(Region.of(region));
                client = clientBuilder.build();
            } else if (useDefaultCredentialsProvider && ObjectHelper.isNotEmpty(region)) {
                SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder();
                clientBuilder.region(Region.of(region));
                client = clientBuilder.build();
            } else if (useProfileCredentialsProvider && ObjectHelper.isNotEmpty(profileName)) {
                SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder();
                clientBuilder.credentialsProvider(ProfileCredentialsProvider.create(profileName));
                clientBuilder.region(Region.of(region));
                client = clientBuilder.build();
            } else {
                throw new RuntimeCamelException(
                        "Using the AWS Secrets Manager Properties Function requires setting AWS credentials as application properties or environment variables");
            }
            SecretsManagerPropertiesFunction secretsManagerPropertiesFunction = new SecretsManagerPropertiesFunction(client);

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
                                stringValue.startsWith("{{aws:") &&
                                stringValue.endsWith("}}")) {
                            LOG.debug("decrypting and overriding property {}", key);
                            try {
                                String element = secretsManagerPropertiesFunction.apply(stringValue
                                        .replace("{{aws:", "")
                                        .replace("}}", ""));
                                System.err.println("Element: " + element);
                                props.put(key, element);
                            } catch (Exception e) {
                                // Log and do nothing
                                LOG.debug("failed to parse property {}. This exception is ignored.", key, e);
                            }
                        }
                    });
                }
            }

            environment.getPropertySources().addFirst(new PropertiesPropertySource("overridden-camel-aws-secrets-manager-properties", props));
        }
    }
}
