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


import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;



@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        JacksonMarshalUnmarshalListTest.class,
        JacksonMarshalUnmarshalListTest.TestConfiguration.class
    }
)

public class JacksonMarshalUnmarshalListTest {

   
    @Autowired
    private CamelContext context;
    
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

        MockEndpoint.assertIsSatisfied(context);

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

        MockEndpoint.assertIsSatisfied(context);

        List list = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(1, list.size());

        TestPojo pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());
    }

    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    JacksonDataFormat format = new JacksonDataFormat(TestPojo.class);
                    format.useList();

                    from("direct:backPojo").unmarshal(format).to("mock:reversePojo");

                }
            };
        }
        
      
    }
    
    
    
    
}
