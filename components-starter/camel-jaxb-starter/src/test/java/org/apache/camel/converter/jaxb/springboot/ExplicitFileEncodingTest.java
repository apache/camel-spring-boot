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
package org.apache.camel.converter.jaxb.springboot;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.example.PurchaseOrder;
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
        ExplicitFileEncodingTest.class,
        ExplicitFileEncodingTest.TestConfiguration.class
    }
)
public class ExplicitFileEncodingTest extends FromFileBase {
    
    
        
    @Autowired
    ProducerTemplate template;

    
    @EndpointInject("mock:file")
    private MockEndpoint result;

        
    @Test
    public void testISOFileEncoding() throws Exception {
        PurchaseOrder order = new PurchaseOrder();
        //Data containing characters ÆØÅæøå that differ in utf-8 and iso
        String name = "\u00c6\u00d8\u00C5\u00e6\u00f8\u00e5";
        order.setName(name);
        order.setAmount(123.45);
        order.setPrice(2.22);

        
        result.expectedFileExists(testFile("output.txt"));

        template.sendBody("direct:start", order);
        result.assertIsSatisfied();

        JAXBContext jaxbContext = JAXBContext.newInstance("org.apache.camel.example");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        PurchaseOrder obj = (PurchaseOrder) unmarshaller.unmarshal(testFile("output.txt").toFile());
        assertEquals(obj.getName(), name);
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    JaxbDataFormat jaxb = new JaxbDataFormat("org.apache.camel.example");
                    jaxb.setEncoding("iso-8859-1");

                    from("direct:start")
                            .marshal(jaxb)
                            .to(fileUri("?fileName=output.txt&charset=iso-8859-1"));
                }
            };
        }
    }
}
