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

import org.apache.camel.CamelContext;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SpringKafkaPropertiesBridgeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CamelAutoConfiguration.class,
                    KafkaAutoConfiguration.class,
                    KafkaComponentAutoConfiguration.class,
                    SpringKafkaPropertiesAutoConfiguration.class));

    @Test
    void shouldBridgeBootstrapServers() {
        contextRunner
                .withPropertyValues(
                        "spring.kafka.bootstrap-servers=broker1:9092,broker2:9092",
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    assertThat(kafka.getConfiguration().getBrokers())
                            .isEqualTo("broker1:9092,broker2:9092");
                });
    }

    @Test
    void shouldPreferExplicitCamelProperty() {
        contextRunner
                .withPropertyValues(
                        "spring.kafka.bootstrap-servers=broker1:9092",
                        "camel.component.kafka.brokers=my-broker:9092",
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    assertThat(kafka.getConfiguration().getBrokers())
                            .isEqualTo("my-broker:9092");
                });
    }

    @Test
    void shouldBridgeSecurityProtocol() {
        contextRunner
                .withPropertyValues(
                        "spring.kafka.security.protocol=SASL_SSL",
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    assertThat(kafka.getConfiguration().getSecurityProtocol())
                            .isEqualTo("SASL_SSL");
                });
    }

    @Test
    void shouldBridgeSslProperties() {
        contextRunner
                .withPropertyValues(
                        "spring.kafka.ssl.key-store-password=keypass",
                        "spring.kafka.ssl.key-store-type=PKCS12",
                        "spring.kafka.ssl.trust-store-password=trustpass",
                        "spring.kafka.ssl.trust-store-type=PEM",
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    assertThat(kafka.getConfiguration().getSslKeystorePassword())
                            .isEqualTo("keypass");
                    assertThat(kafka.getConfiguration().getSslKeystoreType())
                            .isEqualTo("PKCS12");
                    assertThat(kafka.getConfiguration().getSslTruststorePassword())
                            .isEqualTo("trustpass");
                    assertThat(kafka.getConfiguration().getSslTruststoreType())
                            .isEqualTo("PEM");
                });
    }

    @Test
    void shouldBridgeConsumerGroupId() {
        contextRunner
                .withPropertyValues(
                        "spring.kafka.consumer.group-id=my-group",
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    assertThat(kafka.getConfiguration().getGroupId())
                            .isEqualTo("my-group");
                });
    }

    @Test
    void shouldBridgeSaslMechanismFromProperties() {
        contextRunner
                .withPropertyValues(
                        "spring.kafka.properties.sasl.mechanism=PLAIN",
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    assertThat(kafka.getConfiguration().getSaslMechanism())
                            .isEqualTo("PLAIN");
                });
    }

    @Test
    void shouldBridgeSaslJaasConfigFromProperties() {
        String jaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"user\" password=\"pass\";";
        contextRunner
                .withPropertyValues(
                        "spring.kafka.properties.sasl.jaas.config=" + jaasConfig,
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    assertThat(kafka.getConfiguration().getSaslJaasConfig())
                            .isEqualTo(jaasConfig);
                });
    }

    @Test
    void shouldNotBridgeWhenSpringKafkaNotConfigured() {
        contextRunner
                .withPropertyValues(
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    // Default value should remain
                    assertThat(kafka.getConfiguration().getSecurityProtocol())
                            .isEqualTo("PLAINTEXT");
                });
    }

    @Test
    void shouldBridgeClientId() {
        contextRunner
                .withPropertyValues(
                        "spring.kafka.client-id=my-client",
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    assertThat(kafka.getConfiguration().getClientId())
                            .isEqualTo("my-client");
                });
    }

    @Test
    void shouldNotBridgeWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "spring.kafka.bootstrap-servers=broker1:9092",
                        "spring.kafka.security.protocol=SASL_SSL",
                        "camel.component.kafka.bridge-spring-kafka-properties=false",
                        "camel.component.kafka.enabled=true")
                .run(context -> {
                    KafkaComponent kafka = context.getBean(CamelContext.class)
                            .getComponent("kafka", KafkaComponent.class);
                    // Bridge is disabled, so spring.kafka properties should NOT be applied
                    assertThat(kafka.getConfiguration().getBrokers())
                            .isNotEqualTo("broker1:9092");
                    assertThat(kafka.getConfiguration().getSecurityProtocol())
                            .isEqualTo("PLAINTEXT");
                });
    }
}
