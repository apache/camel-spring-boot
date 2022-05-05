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





import java.lang.annotation.ElementType;
import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.Path;
import javax.validation.TraversableResolver;
import javax.validation.Path.Node;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.bean.validator.BeanValidatorEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        BeanValidatorConfigurationTest.class
    }
)
public class BeanValidatorConfigurationTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    
    private static MessageInterpolator messageInterpolator;
    
    private static TraversableResolver traversableResolver;
    
    private static ConstraintValidatorFactory constraintValidatorFactory;

    
    @Bean("myMessageInterpolator")
    private MessageInterpolator getMessageInterpolator() {
        messageInterpolator = new MyMessageInterpolator();
        return messageInterpolator;
    }
    @Bean("myTraversableResolver")
    private TraversableResolver getTraversableResolver() {
        traversableResolver = new MyTraversableResolver();
        return traversableResolver;
    }
    @Bean("myConstraintValidatorFactory")
    private ConstraintValidatorFactory getConstraintValidatorFactory() {
        constraintValidatorFactory = new MyConstraintValidatorFactory();
        return constraintValidatorFactory;
    }

    
    @DisabledOnOs(AIX)
    @Test
    void configureWithDefaults() {
        BeanValidatorEndpoint endpoint = context.getEndpoint("bean-validator://x", BeanValidatorEndpoint.class);
        assertNull(endpoint.getGroup());
    }

    @DisabledOnOs(AIX)
    @Test
    void configureBeanValidator() {
        BeanValidatorEndpoint endpoint = context
                .getEndpoint("bean-validator://x" + "?group=org.apache.camel.component.bean.validator.OptionalChecks"
                             + "&messageInterpolator=#myMessageInterpolator"
                             + "&traversableResolver=#myTraversableResolver"
                             + "&constraintValidatorFactory=#myConstraintValidatorFactory",
                        BeanValidatorEndpoint.class);

        assertEquals("org.apache.camel.component.bean.validator.OptionalChecks", endpoint.getGroup());
        assertSame(messageInterpolator, endpoint.getMessageInterpolator());
        assertSame(traversableResolver, endpoint.getTraversableResolver());
        assertSame(constraintValidatorFactory, endpoint.getConstraintValidatorFactory());
    }
    
    class MyMessageInterpolator implements MessageInterpolator {

        @Override
        public String interpolate(String messageTemplate, Context context) {
            return null;
        }

        @Override
        public String interpolate(String messageTemplate, Context context, Locale locale) {
            return null;
        }
    }

    class MyTraversableResolver implements TraversableResolver {

        @Override
        public boolean isCascadable(
                Object traversableObject, Node traversableProperty, Class<?> rootBeanType, Path pathToTraversableObject,
                ElementType elementType) {
            return false;
        }

        @Override
        public boolean isReachable(
                Object traversableObject, Node traversableProperty, Class<?> rootBeanType, Path pathToTraversableObject,
                ElementType elementType) {
            return false;
        }
    }

    class MyConstraintValidatorFactory implements ConstraintValidatorFactory {

        @Override
        public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
            return null;
        }

        @Override
        public void releaseInstance(ConstraintValidator<?, ?> arg0) {
            // noop
        }
    }

}
