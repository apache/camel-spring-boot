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




import javax.validation.ValidationProviderResolver;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.validator.BeanValidationException;
import org.apache.camel.component.bean.validator.HibernateValidationProviderResolver;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        HibernateValidationProviderResolverTest.class,
        HibernateValidationProviderResolverTest.TestConfiguration.class
    }
)
public class HibernateValidationProviderResolverTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:test")
    MockEndpoint mockEndpoint;
    
    @Bean("myValidationProviderResolver")
    ValidationProviderResolver getValidationProviderResolver() {
        return new HibernateValidationProviderResolver();
    }
    
    @Test
    void shouldResolveHibernateValidationProviderResolver() throws InterruptedException {
        // Given
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.message(0).body().isInstanceOf(CarWithAnnotations.class);
        CarWithAnnotations carWithNullFields = new CarWithAnnotations(null, null);

        template.send("direct:test", exchange -> {
            Message in = exchange.getIn();
            in.setBody(carWithNullFields);
        });
        

        // Then
        mockEndpoint.assertIsSatisfied();
    }
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    onException(BeanValidationException.class).to(mockEndpoint);

                    from("direct:test").to(
                            "bean-validator://ValidationProviderResolverTest?validationProviderResolver=#myValidationProviderResolver");
                }
            };
        }
    }
    
   

}
