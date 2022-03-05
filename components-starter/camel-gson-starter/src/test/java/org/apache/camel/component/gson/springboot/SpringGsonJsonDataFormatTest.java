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



import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        SpringGsonJsonDataFormatTest.class
    },
    properties = {
        "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SpringGsonJsonDataFormatTest.xml"}

)
public class SpringGsonJsonDataFormatTest {

    @Autowired
    private CamelContext context;
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:reversePojo")
    MockEndpoint mock;
    
  
    
    @Bean(name = "pretty") 
    GsonDataFormat getPrettyGsonDataFormat() {
        GsonDataFormat gsonDataformat = new GsonDataFormat();
        gsonDataformat.setPrettyPrint(true);
        gsonDataformat.setUnmarshalTypeName("org.apache.camel.component.gson.springboot.TestPojo");
        return gsonDataformat;
    }
    
    @Bean(name = "gson") 
    GsonDataFormat getGsonDataFormat() {
        GsonDataFormat gsonDataformat = new GsonDataFormat();
        gsonDataformat.setUnmarshalTypeName("org.apache.camel.component.gson.springboot.TestPojo");
        return gsonDataformat;
    }

    @Test
    public void testMarshalAndUnmarshalPojo() throws Exception {
        TestPojo in = new TestPojo();
        in.setName("Camel");

        mock.reset();
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojo.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = template.requestBody("direct:inPojo", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

        template.sendBody("direct:backPojo", marshalled);

        mock.assertIsSatisfied();
    }

    @Test
    public void testMarshalAndUnmarshalPojoWithPrettyPrint() throws Exception {
        TestPojo in = new TestPojo();
        in.setName("Camel");
        mock.reset();
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojo.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = template.requestBody("direct:inPretty", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        String expected = "{\n"
                          + "  \"name\": \"Camel\""
                          + "\n}";
        assertEquals(expected, marshalledAsString);

        template.sendBody("direct:backPretty", marshalled);

        mock.assertIsSatisfied();
    }
}
