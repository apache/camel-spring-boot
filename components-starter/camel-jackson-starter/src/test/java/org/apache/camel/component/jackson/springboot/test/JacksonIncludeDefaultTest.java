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



import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;


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
        JacksonIncludeDefaultTest.class,
        JacksonIncludeDefaultTest.TestConfiguration.class
    }
)

public class JacksonIncludeDefaultTest {

    @Autowired
    @Produce("direct:start")
    ProducerTemplate template;

    @EndpointInject("mock:marshal")
    MockEndpoint mock;
    
    @Test
    public void testMmarshalPojo() throws Exception {
        
        mock.expectedMessageCount(1);
        mock.message(0).body(String.class).isEqualTo("{\"name\":\"Camel\",\"country\":null}");

        TestOtherPojo pojo = new TestOtherPojo();
        pojo.setName("Camel");

        template.sendBody("direct:marshal", pojo);

        mock.assertIsSatisfied();
    }
    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    JacksonDataFormat format = new JacksonDataFormat();

                    from("direct:marshal").marshal(format).to("mock:marshal");
                }
            };
        }
        
      
    }
    
    
    
    
}
