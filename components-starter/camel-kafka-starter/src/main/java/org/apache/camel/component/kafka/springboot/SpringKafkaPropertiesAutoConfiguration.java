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
package org.apache.camel.component.kafka.springboot;

import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

/**
 * Auto-configuration that bridges Spring Boot's {@code spring.kafka.*} properties
 * to the Camel Kafka component configuration ({@code camel.component.kafka.*}).
 * <p>
 * This allows users to configure Kafka once using standard Spring Boot properties
 * and have the Camel Kafka component automatically pick up those settings, without
 * needing to duplicate configuration under {@code camel.component.kafka.*}.
 * <p>
 * If a property is explicitly set under {@code camel.component.kafka.*}, it takes
 * precedence over the corresponding {@code spring.kafka.*} property.
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class, before = KafkaComponentAutoConfiguration.class)
@ConditionalOnClass(KafkaProperties.class)
@ConditionalOnBean(KafkaProperties.class)
public class SpringKafkaPropertiesAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SpringKafkaPropertiesAutoConfiguration.class);

    private static final String CAMEL_KAFKA_PREFIX = "camel.component.kafka.";
    private static final String SPRING_KAFKA_PREFIX = "spring.kafka.";

    private final KafkaProperties kafkaProperties;
    private final KafkaComponentConfiguration camelKafkaConfig;
    private final Binder binder;

    public SpringKafkaPropertiesAutoConfiguration(
            KafkaProperties kafkaProperties,
            KafkaComponentConfiguration camelKafkaConfig,
            Environment environment) {
        this.kafkaProperties = kafkaProperties;
        this.camelKafkaConfig = camelKafkaConfig;
        this.binder = Binder.get(environment);
    }

    @PostConstruct
    public void bridgeProperties() {
        boolean bridged = false;

        // Bootstrap servers — KafkaProperties defaults to ["localhost:9092"],
        // so we must check if the user explicitly set spring.kafka.bootstrap-servers
        if (!isCamelPropertyBound("brokers") && isSpringPropertyBound("bootstrap-servers")) {
            String brokers = String.join(",", kafkaProperties.getBootstrapServers());
            camelKafkaConfig.setBrokers(brokers);
            LOG.debug("Bridged spring.kafka.bootstrap-servers -> camel.component.kafka.brokers: {}", brokers);
            bridged = true;
        }

        // Client ID
        if (!isCamelPropertyBound("client-id") && kafkaProperties.getClientId() != null) {
            camelKafkaConfig.setClientId(kafkaProperties.getClientId());
            LOG.debug("Bridged spring.kafka.client-id -> camel.component.kafka.client-id");
            bridged = true;
        }

        // Security protocol
        if (!isCamelPropertyBound("security-protocol")
                && kafkaProperties.getSecurity() != null
                && kafkaProperties.getSecurity().getProtocol() != null) {
            camelKafkaConfig.setSecurityProtocol(kafkaProperties.getSecurity().getProtocol());
            LOG.debug("Bridged spring.kafka.security.protocol -> camel.component.kafka.security-protocol");
            bridged = true;
        }

        // Consumer group ID
        if (!isCamelPropertyBound("group-id")
                && kafkaProperties.getConsumer() != null
                && kafkaProperties.getConsumer().getGroupId() != null) {
            camelKafkaConfig.setGroupId(kafkaProperties.getConsumer().getGroupId());
            LOG.debug("Bridged spring.kafka.consumer.group-id -> camel.component.kafka.group-id");
            bridged = true;
        }

        // SSL properties
        bridged |= bridgeSslProperties();

        // SASL properties from spring.kafka.properties map
        bridged |= bridgeSaslProperties();

        if (bridged) {
            LOG.info("Bridged spring.kafka.* properties to camel.component.kafka.*");
        }
    }

    private boolean bridgeSslProperties() {
        KafkaProperties.Ssl ssl = kafkaProperties.getSsl();
        if (ssl == null) {
            return false;
        }

        boolean bridged = false;

        if (!isCamelPropertyBound("ssl-key-password") && ssl.getKeyPassword() != null) {
            camelKafkaConfig.setSslKeyPassword(ssl.getKeyPassword());
            bridged = true;
        }
        if (!isCamelPropertyBound("ssl-keystore-location") && ssl.getKeyStoreLocation() != null) {
            camelKafkaConfig.setSslKeystoreLocation(resourceToPath(ssl.getKeyStoreLocation()));
            bridged = true;
        }
        if (!isCamelPropertyBound("ssl-keystore-password") && ssl.getKeyStorePassword() != null) {
            camelKafkaConfig.setSslKeystorePassword(ssl.getKeyStorePassword());
            bridged = true;
        }
        if (!isCamelPropertyBound("ssl-keystore-type") && ssl.getKeyStoreType() != null) {
            camelKafkaConfig.setSslKeystoreType(ssl.getKeyStoreType());
            bridged = true;
        }
        if (!isCamelPropertyBound("ssl-truststore-location") && ssl.getTrustStoreLocation() != null) {
            camelKafkaConfig.setSslTruststoreLocation(resourceToPath(ssl.getTrustStoreLocation()));
            bridged = true;
        }
        if (!isCamelPropertyBound("ssl-truststore-password") && ssl.getTrustStorePassword() != null) {
            camelKafkaConfig.setSslTruststorePassword(ssl.getTrustStorePassword());
            bridged = true;
        }
        if (!isCamelPropertyBound("ssl-truststore-type") && ssl.getTrustStoreType() != null) {
            camelKafkaConfig.setSslTruststoreType(ssl.getTrustStoreType());
            bridged = true;
        }
        if (!isCamelPropertyBound("ssl-protocol") && ssl.getProtocol() != null) {
            camelKafkaConfig.setSslProtocol(ssl.getProtocol());
            bridged = true;
        }

        if (bridged) {
            LOG.debug("Bridged spring.kafka.ssl.* properties to camel.component.kafka.ssl-*");
        }
        return bridged;
    }

    private boolean bridgeSaslProperties() {
        Map<String, String> rawProps = kafkaProperties.getProperties();
        if (rawProps == null || rawProps.isEmpty()) {
            return false;
        }

        boolean bridged = false;

        if (!isCamelPropertyBound("sasl-mechanism")
                && rawProps.containsKey("sasl.mechanism")) {
            camelKafkaConfig.setSaslMechanism(rawProps.get("sasl.mechanism"));
            LOG.debug("Bridged spring.kafka.properties[sasl.mechanism] -> camel.component.kafka.sasl-mechanism");
            bridged = true;
        }
        if (!isCamelPropertyBound("sasl-jaas-config")
                && rawProps.containsKey("sasl.jaas.config")) {
            camelKafkaConfig.setSaslJaasConfig(rawProps.get("sasl.jaas.config"));
            LOG.debug("Bridged spring.kafka.properties[sasl.jaas.config] -> camel.component.kafka.sasl-jaas-config");
            bridged = true;
        }
        if (!isCamelPropertyBound("sasl-kerberos-service-name")
                && rawProps.containsKey("sasl.kerberos.service.name")) {
            camelKafkaConfig.setSaslKerberosServiceName(rawProps.get("sasl.kerberos.service.name"));
            LOG.debug("Bridged spring.kafka.properties[sasl.kerberos.service.name] -> camel.component.kafka.sasl-kerberos-service-name");
            bridged = true;
        }

        return bridged;
    }

    /**
     * Check if a Camel Kafka property was explicitly bound by the user.
     * Uses {@link Binder} which properly handles relaxed binding
     * (camelCase, kebab-case, underscore variants).
     */
    private boolean isCamelPropertyBound(String propertyName) {
        return binder.bind(CAMEL_KAFKA_PREFIX + propertyName, Bindable.of(String.class)).isBound();
    }

    /**
     * Check if a Spring Kafka property was explicitly set by the user.
     */
    private boolean isSpringPropertyBound(String propertyName) {
        return binder.bind(SPRING_KAFKA_PREFIX + propertyName, Bindable.of(Object.class)).isBound();
    }

    private static String resourceToPath(Resource resource) {
        try {
            return resource.getFile().getAbsolutePath();
        } catch (Exception e) {
            try {
                return resource.getURI().toString();
            } catch (Exception ex) {
                return resource.toString();
            }
        }
    }
}
