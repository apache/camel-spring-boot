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
package org.apache.camel.jaxb;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.example.Address;
import org.apache.camel.example.Order;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        JaxbMarshalNamespacePrefixMapperTest.class
    }
)
public class JaxbMarshalNamespacePrefixMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(JaxbMarshalNamespacePrefixMapperTest.class);
    
    @Autowired
    ProducerTemplate template;
    
   
    @EndpointInject("mock:result")
    private MockEndpoint mock;

    @Bean("myPrefix")
    public Map<String, String> addMap() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("http://www.camel.apache.org/jaxb/example/order/1", "o");
        map.put("http://www.camel.apache.org/jaxb/example/address/1", "a");

        return map;
    }

    @Test
    public void testNamespacePrefix() throws Exception {
        mock.expectedMessageCount(1);

        Order order = new Order();
        order.setId("1");
        Address address = new Address();
        address.setStreet("Main Street");
        address.setStreetNumber("3a");
        address.setZip("65843");
        address.setCity("Sulzbach");
        order.setAddress(address);

        template.sendBody("direct:start", order);

        mock.assertIsSatisfied();

        String xml = mock.getExchanges().get(0).getIn().getBody(String.class);
        LOG.info(xml);

        assertTrue(xml.contains("xmlns:a=\"http://www.camel.apache.org/jaxb/example/address/1\""));
        assertTrue(xml.contains("xmlns:o=\"http://www.camel.apache.org/jaxb/example/order/1\""));
        assertTrue(xml.contains("<o:id>1</o:id>"));
        assertTrue(xml.contains("<a:street>Main Street</a:street>"));
        assertTrue(xml.contains("</o:order>"));
    }

    @Bean
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                JaxbDataFormat df = new JaxbDataFormat();
                df.setContextPath("org.apache.camel.example");
                df.setNamespacePrefixRef("myPrefix");

                from("direct:start")
                        .marshal(df)
                        .to("mock:result");

            }
        };
    }
}
