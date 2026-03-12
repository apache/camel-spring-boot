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

import java.util.Collections;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Configuration;
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
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(KafkaProperties.class)
@AutoConfigureBefore(KafkaComponentAutoConfiguration.class)
public class SpringKafkaPropertiesAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SpringKafkaPropertiesAutoConfiguration.class);

    private final KafkaProperties kafkaProperties;
    private final KafkaComponentConfiguration camelKafkaConfig;
    private final Environment environment;

    public SpringKafkaPropertiesAutoConfiguration(
            KafkaProperties kafkaProperties,
            KafkaComponentConfiguration camelKafkaConfig,
            Environment environment) {
        this.kafkaProperties = kafkaProperties;
        this.camelKafkaConfig = camelKafkaConfig;
        this.environment = environment;
    }

    @PostConstruct
    public void bridgeProperties() {
        // Get the set of camel.component.kafka.* properties explicitly set by the user
        Map<String, Object> camelKafkaProps = Binder.get(environment)
                .bind("camel.component.kafka", Bindable.mapOf(String.class, Object.class))
                .orElse(Collections.emptyMap());

        boolean bridged = false;

        // Bootstrap servers
        if (!camelKafkaProps.containsKey("brokers")
                && kafkaProperties.getBootstrapServers() != null
                && !kafkaProperties.getBootstrapServers().isEmpty()) {
            String brokers = String.join(",", kafkaProperties.getBootstrapServers());
            camelKafkaConfig.setBrokers(brokers);
            LOG.debug("Bridged spring.kafka.bootstrap-servers -> camel.component.kafka.brokers: {}", brokers);
            bridged = true;
        }

        // Client ID
        if (!camelKafkaProps.containsKey("client-id")
                && kafkaProperties.getClientId() != null) {
            camelKafkaConfig.setClientId(kafkaProperties.getClientId());
            LOG.debug("Bridged spring.kafka.client-id -> camel.component.kafka.client-id");
            bridged = true;
        }

        // Security protocol
        if (!camelKafkaProps.containsKey("security-protocol")
                && kafkaProperties.getSecurity() != null
                && kafkaProperties.getSecurity().getProtocol() != null) {
            camelKafkaConfig.setSecurityProtocol(kafkaProperties.getSecurity().getProtocol());
            LOG.debug("Bridged spring.kafka.security.protocol -> camel.component.kafka.security-protocol");
            bridged = true;
        }

        // Consumer group ID
        if (!camelKafkaProps.containsKey("group-id")
                && kafkaProperties.getConsumer() != null
                && kafkaProperties.getConsumer().getGroupId() != null) {
            camelKafkaConfig.setGroupId(kafkaProperties.getConsumer().getGroupId());
            LOG.debug("Bridged spring.kafka.consumer.group-id -> camel.component.kafka.group-id");
            bridged = true;
        }

        // SSL properties
        bridged |= bridgeSslProperties(camelKafkaProps);

        // SASL properties from spring.kafka.properties map
        bridged |= bridgeSaslProperties(camelKafkaProps);

        if (bridged) {
            LOG.info("Bridged spring.kafka.* properties to camel.component.kafka.*");
        }
    }

    private boolean bridgeSslProperties(Map<String, Object> camelKafkaProps) {
        KafkaProperties.Ssl ssl = kafkaProperties.getSsl();
        if (ssl == null) {
            return false;
        }

        boolean bridged = false;

        if (!camelKafkaProps.containsKey("ssl-key-password") && ssl.getKeyPassword() != null) {
            camelKafkaConfig.setSslKeyPassword(ssl.getKeyPassword());
            bridged = true;
        }
        if (!camelKafkaProps.containsKey("ssl-keystore-location") && ssl.getKeyStoreLocation() != null) {
            camelKafkaConfig.setSslKeystoreLocation(resourceToPath(ssl.getKeyStoreLocation()));
            bridged = true;
        }
        if (!camelKafkaProps.containsKey("ssl-keystore-password") && ssl.getKeyStorePassword() != null) {
            camelKafkaConfig.setSslKeystorePassword(ssl.getKeyStorePassword());
            bridged = true;
        }
        if (!camelKafkaProps.containsKey("ssl-keystore-type") && ssl.getKeyStoreType() != null) {
            camelKafkaConfig.setSslKeystoreType(ssl.getKeyStoreType());
            bridged = true;
        }
        if (!camelKafkaProps.containsKey("ssl-truststore-location") && ssl.getTrustStoreLocation() != null) {
            camelKafkaConfig.setSslTruststoreLocation(resourceToPath(ssl.getTrustStoreLocation()));
            bridged = true;
        }
        if (!camelKafkaProps.containsKey("ssl-truststore-password") && ssl.getTrustStorePassword() != null) {
            camelKafkaConfig.setSslTruststorePassword(ssl.getTrustStorePassword());
            bridged = true;
        }
        if (!camelKafkaProps.containsKey("ssl-truststore-type") && ssl.getTrustStoreType() != null) {
            camelKafkaConfig.setSslTruststoreType(ssl.getTrustStoreType());
            bridged = true;
        }
        if (!camelKafkaProps.containsKey("ssl-protocol") && ssl.getProtocol() != null) {
            camelKafkaConfig.setSslProtocol(ssl.getProtocol());
            bridged = true;
        }

        if (bridged) {
            LOG.debug("Bridged spring.kafka.ssl.* properties to camel.component.kafka.ssl-*");
        }
        return bridged;
    }

    private boolean bridgeSaslProperties(Map<String, Object> camelKafkaProps) {
        Map<String, String> rawProps = kafkaProperties.getProperties();
        if (rawProps == null || rawProps.isEmpty()) {
            return false;
        }

        boolean bridged = false;

        if (!camelKafkaProps.containsKey("sasl-mechanism")
                && rawProps.containsKey("sasl.mechanism")) {
            camelKafkaConfig.setSaslMechanism(rawProps.get("sasl.mechanism"));
            LOG.debug("Bridged spring.kafka.properties[sasl.mechanism] -> camel.component.kafka.sasl-mechanism");
            bridged = true;
        }
        if (!camelKafkaProps.containsKey("sasl-jaas-config")
                && rawProps.containsKey("sasl.jaas.config")) {
            camelKafkaConfig.setSaslJaasConfig(rawProps.get("sasl.jaas.config"));
            LOG.debug("Bridged spring.kafka.properties[sasl.jaas.config] -> camel.component.kafka.sasl-jaas-config");
            bridged = true;
        }
        if (!camelKafkaProps.containsKey("sasl-kerberos-service-name")
                && rawProps.containsKey("sasl.kerberos.service.name")) {
            camelKafkaConfig.setSaslKerberosServiceName(rawProps.get("sasl.kerberos.service.name"));
            LOG.debug("Bridged spring.kafka.properties[sasl.kerberos.service.name] -> camel.component.kafka.sasl-kerberos-service-name");
            bridged = true;
        }

        return bridged;
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
