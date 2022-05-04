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
package org.apache.camel.component.bean.validator.springboot;





import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.GenericBootstrap;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.bean.validator.BeanValidatorEndpoint;
import org.apache.camel.component.bean.validator.BeanValidatorProducer;
import org.apache.camel.component.bean.validator.HibernateValidationProviderResolver;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.condition.OS.AIX;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        ValidatorFactoryRegistryTest.class
    }
)
public class ValidatorFactoryRegistryTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    
    
    private static ValidatorFactory validatorFactory;

    
    private static ValidatorFactory otherValidatorFactory;
    
    @Bean("myValidatorFactory")
    private ValidatorFactory getMyValidatorFactory() {
        GenericBootstrap bootstrap = Validation.byDefaultProvider();
        bootstrap.providerResolver(new HibernateValidationProviderResolver());

        validatorFactory = bootstrap.configure().buildValidatorFactory();
        return validatorFactory;
    }
    
    @Bean("otherValidatorFactory")
    private ValidatorFactory getOtherValidatorFactory() {
        GenericBootstrap bootstrap = Validation.byDefaultProvider();
        bootstrap.providerResolver(new HibernateValidationProviderResolver());

        otherValidatorFactory = bootstrap.configure().buildValidatorFactory();
        return otherValidatorFactory;
    }

    @DisabledOnOs(AIX)
    @Test
    void configureValidatorFactoryFromRegistry() throws Exception {
        BeanValidatorEndpoint endpoint
                = context.getEndpoint("bean-validator:dummy?validatorFactory=#otherValidatorFactory",
                        BeanValidatorEndpoint.class);
        BeanValidatorProducer producer = (BeanValidatorProducer) endpoint.createProducer();

        assertSame(otherValidatorFactory, endpoint.getValidatorFactory());
        assertSame(otherValidatorFactory, producer.getValidatorFactory());
    }

}
