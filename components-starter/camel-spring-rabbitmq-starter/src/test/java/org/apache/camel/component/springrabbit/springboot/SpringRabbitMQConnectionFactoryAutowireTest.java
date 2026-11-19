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
package org.apache.camel.component.springrabbit.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.component.springrabbit.SpringRabbitMQComponent;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The starter alone (no {@code spring-boot-starter-rabbitmq}) must give the component the {@link ConnectionFactory}
 * and {@link AmqpAdmin} that Spring Boot auto-configures. No broker is needed: the connection factory connects lazily.
 */
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = SpringRabbitMQConnectionFactoryAutowireTest.TestConfiguration.class)
class SpringRabbitMQConnectionFactoryAutowireTest {

    @Autowired
    CamelContext context;

    @Autowired
    ConnectionFactory connectionFactory;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Test
    void componentUsesBootConnectionFactory() {
        SpringRabbitMQComponent component = context.getComponent("spring-rabbitmq", SpringRabbitMQComponent.class);

        assertThat(component.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(component.getAmqpAdmin()).isSameAs(amqpAdmin);
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {
    }
}
