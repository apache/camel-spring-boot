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
package org.apache.camel.component.gson.springboot;


import com.google.gson.FieldNamingPolicy;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        SpringGsonFieldNamePolicyTest.class
    },
    properties = {
        "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SpringGsonFieldNamePolicyTest.xml"}

)
public class SpringGsonFieldNamePolicyTest {

    
    @Autowired
    ProducerTemplate template;

    
  
    
    
    
    @Bean(name = "gson") 
    GsonDataFormat getGsonDataFormat() {
        GsonDataFormat gsonDataformat = new GsonDataFormat();
        gsonDataformat.setUnmarshalTypeName("org.apache.camel.component.gson.springboot.PersonPojo");
        gsonDataformat.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        return gsonDataformat;
    }

    @Test
    public void testUnmarshalPojo() {
        String json = "{\"id\":\"123\",\"first_name\":\"Donald\",\"last_name\":\"Duck\"}";
        PersonPojo pojo = template.requestBody("direct:backPojo", json, PersonPojo.class);
        assertNotNull(pojo);

        assertEquals(123, pojo.getId());
        assertEquals("Donald", pojo.getFirstName());
        assertEquals("Duck", pojo.getLastName());
    }

    @Test
    public void testMarshalPojo() {
        PersonPojo pojo = new PersonPojo();
        pojo.setId(123);
        pojo.setFirstName("Donald");
        pojo.setLastName("Duck");

        String expected = "{\"id\":123,\"first_name\":\"Donald\",\"last_name\":\"Duck\"}";
        String json = template.requestBody("direct:inPojo", pojo, String.class);
        assertEquals(expected, json);
    }
}
