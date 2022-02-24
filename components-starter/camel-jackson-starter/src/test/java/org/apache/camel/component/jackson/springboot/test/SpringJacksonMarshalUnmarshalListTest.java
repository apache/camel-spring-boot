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


import java.util.List;


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
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        SpringJacksonMarshalUnmarshalListTest.class,
    },
        properties = {
    "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SpringJacksonMarshalUnmarshalListTest.xml"}
)

public class SpringJacksonMarshalUnmarshalListTest {

    @Autowired
    @Produce("direct:start")
    ProducerTemplate template;

    @EndpointInject("mock:reversePojo")
    MockEndpoint mock;
    
    @Test
    public void testUnmarshalListPojo() throws Exception {
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);

        String json = "[{\"name\":\"Camel\"}, {\"name\":\"World\"}]";
        template.sendBody("direct:backPojo", json);

        mock.assertIsSatisfied();

        List list = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(2, list.size());

        TestPojo pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());
        pojo = (TestPojo) list.get(1);
        assertEquals("World", pojo.getName());
    }

    @Test
    public void testUnmarshalListPojoOneElement() throws Exception {
        
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);

        String json = "[{\"name\":\"Camel\"}]";
        template.sendBody("direct:backPojo", json);

        mock.assertIsSatisfied();

        List list = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(1, list.size());

        TestPojo pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());
    }
    
    @Bean(name = "pojo") 
    JsonDataFormat getPojoJsonDataFormat() {
        JsonDataFormat jsonDataformat = new JsonDataFormat();
        jsonDataformat.library(JsonLibrary.Jackson);
        jsonDataformat.useList(true);
        jsonDataformat.unmarshalType("org.apache.camel.component.jackson.springboot.test.TestPojo");
        return jsonDataformat;
    }
    
    
    
}
