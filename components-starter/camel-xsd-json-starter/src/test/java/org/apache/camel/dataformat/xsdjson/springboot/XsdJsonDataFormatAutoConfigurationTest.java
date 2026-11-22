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
package org.apache.camel.dataformat.xsdjson.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.xsdjson.XsdJsonDataFormat;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(classes = {
        CamelAutoConfiguration.class, XsdJsonDataFormatAutoConfiguration.class,
        XsdJsonDataFormatAutoConfigurationTest.TestConfiguration.class },
        properties = {
                "camel.dataformat.xsd-json.schema=classpath:person.xsd",
                "camel.dataformat.xsd-json.element=person" })
public class XsdJsonDataFormatAutoConfigurationTest {

    private static final String XML = "<person xmlns=\"urn:person\"><name>Ann</name><age>42</age><tag>a</tag></person>";

    @Autowired
    CamelContext context;

    @Autowired
    ProducerTemplate template;

    @Test
    public void testOptionsFromSpringBoot() {
        XsdJsonDataFormat df = (XsdJsonDataFormat) context.resolveDataFormat("xsdJson");
        assertThat(df.getSchema()).isEqualTo("classpath:person.xsd");
        assertThat(df.getElement()).isEqualTo("person");
    }

    @Test
    public void testUnmarshalAndMarshal() {
        String json = template.requestBody("direct:toJson", XML, String.class);
        // the schema gives the types and the arrays: age is a number, a single tag is an array of one
        assertThat(json).isEqualToIgnoringWhitespace("{\"name\":\"Ann\",\"age\":42,\"tag\":[\"a\"]}");

        String xml = template.requestBody("direct:toXml", json, String.class);
        // the elements are written in the namespace of the schema
        assertThat(xml).endsWith("<ns1:person xmlns:ns1=\"urn:person\"><ns1:name>Ann</ns1:name><ns1:age>42</ns1:age>"
                                 + "<ns1:tag>a</ns1:tag></ns1:person>");
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:toJson").unmarshal().custom("xsdJson");
                    from("direct:toXml").marshal().custom("xsdJson");
                }
            };
        }
    }
}
