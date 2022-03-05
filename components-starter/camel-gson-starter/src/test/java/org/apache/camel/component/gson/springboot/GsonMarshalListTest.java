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


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        GsonMarshalListTest.class,
        GsonMarshalListTest.TestConfiguration.class
    }
)
public class GsonMarshalListTest {

    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:reversePojo")
    MockEndpoint mock;

    
    @Test
    public void testMarshalAndUnmarshalPojo() throws Exception {

        List<TestPojo> inList = new ArrayList<>();

        TestPojo in1 = new TestPojo();
        in1.setName("Camel1");

        TestPojo in2 = new TestPojo();
        in2.setName("Camel2");

        inList.add(in1);
        inList.add(in2);

        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);
        mock.message(0).body().isEqualTo(inList);

        String marshalled = template.requestBody("direct:inPojo", inList, String.class);
        assertEquals("[{\"name\":\"Camel1\"},{\"name\":\"Camel2\"}]", marshalled);

        template.sendBody("direct:backPojo", marshalled);

        mock.assertIsSatisfied();
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {

                    GsonDataFormat formatPojo = new GsonDataFormat();
                    Type genericType = new TypeToken<List<TestPojo>>() {
                    }.getType();
                    formatPojo.setUnmarshalGenericType(genericType);

                    from("direct:inPojo").marshal(formatPojo);
                    from("direct:backPojo").unmarshal(formatPojo).to("mock:reversePojo");
                }
            };
        }
    }
}
