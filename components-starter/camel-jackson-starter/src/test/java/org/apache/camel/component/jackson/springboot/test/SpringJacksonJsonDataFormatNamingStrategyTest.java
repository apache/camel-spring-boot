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
package org.apache.camel.component.jackson.springboot.test;


import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;



@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SpringJacksonJsonDataFormatNamingStrategyTest.class,
    },
    properties = {
    "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SpringJacksonJsonDataFormatNamingStrategyTest.xml"}
)

public class SpringJacksonJsonDataFormatNamingStrategyTest {

   
    @Autowired
    private CamelContext context;
    
    @Autowired
    @Produce("direct:start")
    ProducerTemplate template;

    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testMarshalAndUnmarshalMap() throws Exception {
        PojoNamingStrategy pojoNamingStrategy = new PojoNamingStrategy();
        pojoNamingStrategy.setFieldOne("test");
        pojoNamingStrategy.setFieldTwo("supertest");

        
        mock.expectedMessageCount(1);
        Object marshalled = template.requestBody("direct:in", pojoNamingStrategy);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"field.one\":\"test\",\"field.two\":\"supertest\"}", marshalledAsString);

        mock.assertIsSatisfied();
    }

    @Bean(name = "jack") 
    JsonDataFormat getViewJsonDataFormat() {
        JsonDataFormat jsonDataformat = new JsonDataFormat();
        jsonDataformat.library(JsonLibrary.Jackson);
        jsonDataformat.setNamingStrategy("LOWER_DOT_CASE");
        return jsonDataformat;
    }
     
    
}
