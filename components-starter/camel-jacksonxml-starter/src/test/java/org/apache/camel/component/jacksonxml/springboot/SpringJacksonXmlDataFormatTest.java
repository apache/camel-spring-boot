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
package org.apache.camel.component.jacksonxml.springboot;


import java.util.HashMap;
import java.util.Map;


import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jacksonxml.JacksonXMLDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
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
        SpringJacksonXmlDataFormatTest.class,
    },
        properties = {
    "camel.springboot.routes-include-pattern=file:src/test/resources/routes/SpringJacksonXmlDataFormatTest.xml"}
)

public class SpringJacksonXmlDataFormatTest {

    private static final String LS = System.lineSeparator();
    
    @Autowired
    private CamelContext context;
    
    @Autowired
    @Produce("direct:start")
    ProducerTemplate template;

    @EndpointInject("mock:reversePojo")
    MockEndpoint mockPojo;
    
    @EndpointInject("mock:reverse")
    MockEndpoint mock;
    
    @EndpointInject("mock:reverseAgeView")
    MockEndpoint mockAge;
    
    @Test
    public void testMarshalAndUnmarshalMap() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Map.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = template.requestBody("direct:in", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("<HashMap><name>Camel</name></HashMap>", marshalledAsString);

        template.sendBody("direct:back", marshalled);

        mock.assertIsSatisfied();
    }

    @Test
    public void testMarshalAndUnmarshalMapWithPrettyPrint() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Map.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = template.requestBody("direct:inPretty", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        String expected = "<HashMap>" + LS + "  <name>Camel</name>" + LS + "</HashMap>" + LS;
        assertEquals(expected, marshalledAsString);

        template.sendBody("direct:back", marshalled);

        mock.assertIsSatisfied();
    }

    @Test
    public void testMarshalAndUnmarshalPojo() throws Exception {
        TestPojo in = new TestPojo();
        in.setName("Camel");

        
        mockPojo.expectedMessageCount(1);
        mockPojo.message(0).body().isInstanceOf(TestPojo.class);
        mockPojo.message(0).body().isEqualTo(in);

        Object marshalled = template.requestBody("direct:inPojo", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("<TestPojo><name>Camel</name></TestPojo>", marshalledAsString);

        template.sendBody("direct:backPojo", marshalled);

        mock.assertIsSatisfied();
    }

    @Test
    public void testMarshalAndUnmarshalAgeView() throws Exception {
        TestPojoView in = new TestPojoView();

        
        mockAge.expectedMessageCount(1);
        mockAge.message(0).body().isInstanceOf(TestPojoView.class);
        mockAge.message(0).body().isEqualTo(in);

        Object marshalled = template.requestBody("direct:inAgeView", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("<TestPojoView><age>30</age><height>190</height></TestPojoView>", marshalledAsString);

        template.sendBody("direct:backAgeView", marshalled);

        mock.assertIsSatisfied();
    }
    
    @Bean(name = "jack") 
    JacksonXMLDataFormat getXmlDataFormat() {
        return new JacksonXMLDataFormat();
    }

    @Bean(name = "pretty") 
    JacksonXMLDataFormat getPrettyXmlDataFormat() {
        JacksonXMLDataFormat xmlDataformat = new JacksonXMLDataFormat();
        xmlDataformat.setPrettyPrint(true);
        return xmlDataformat;
    }

    @Bean(name = "pojo") 
    JacksonXMLDataFormat getPojoXmlDataFormat() {
        JacksonXMLDataFormat xmlDataformat = new JacksonXMLDataFormat();
        xmlDataformat.setUnmarshalTypeName("org.apache.camel.component.jacksonxml.springboot.TestPojo");
        return xmlDataformat;
    }
    
    @Bean(name = "view") 
    JacksonXMLDataFormat getViewXmlDataFormat() {
        JacksonXMLDataFormat xmlDataformat = new JacksonXMLDataFormat();
        xmlDataformat.setUnmarshalTypeName("org.apache.camel.component.jacksonxml.springboot.TestPojoView");
        xmlDataformat.setJsonView(org.apache.camel.component.jacksonxml.springboot.Views.Age.class);
        return xmlDataformat;
    }
}
