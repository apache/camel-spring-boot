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


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.reifier.dataformat.DataFormatReifier;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
        SpringJacksonObjectMapperRegistryTest.class,
    },
        properties = {
    "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SpringJacksonObjectMapperRegistryTest.xml"}
)

public class SpringJacksonObjectMapperRegistryTest {

   
    @Autowired
    private CamelContext context;
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:reverse")
    MockEndpoint mock;
    
    @Test
    public void testMarshalAndUnmarshalMap() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Map.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = template.requestBody("direct:in", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

        template.sendBody("direct:back", marshalled);

        mock.assertIsSatisfied();

        MyJsonObjectMapper mapper = (MyJsonObjectMapper) context.getRegistry().lookupByName("myJsonObjectMapper");
        assertNotNull(mapper);

        JacksonDataFormat df = (JacksonDataFormat) DataFormatReifier.getDataFormat(context, "jack");
        assertNotNull(df);
        assertSame(mapper, df.getObjectMapper());
    }

    @Bean(name = "myJsonObjectMapper")
    MyJsonObjectMapper getMyJsonObjectMapper() {
        return new MyJsonObjectMapper();
    }
    
    @Bean(name = "jack") 
    JsonDataFormat getViewJsonDataFormat() {
        JsonDataFormat jsonDataformat = new JsonDataFormat();
        jsonDataformat.library(JsonLibrary.Jackson);
        jsonDataformat.setObjectMapper("myJsonObjectMapper");
        return jsonDataformat;
    }
    
    class MyJsonObjectMapper extends ObjectMapper {
    }
  
    
    
}
