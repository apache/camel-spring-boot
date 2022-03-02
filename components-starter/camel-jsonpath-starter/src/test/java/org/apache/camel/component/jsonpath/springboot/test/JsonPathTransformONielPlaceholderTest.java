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
package org.apache.camel.component.jsonpath.springboot.test;


import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;

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
        JsonPathTransformONielPlaceholderTest.class,
        JsonPathTransformONielPlaceholderTest.TestConfiguration.class
    }
)
public class JsonPathTransformONielPlaceholderTest {


    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:authors")
    MockEndpoint mock;

    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                PropertiesComponent pc = context.getPropertiesComponent();
                Properties props = new Properties();
                props.put("who", "John O'Niel");
                props.put("search", "Sword's of Honour");
                pc.setInitialProperties(props);

            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here
            }
        };
    }

    
    @Test
    public void testAuthors() throws Exception {
        
        mock.expectedMessageCount(1);

        template.sendBody("direct:start", new File("src/test/resources/books.json"));

        mock.assertIsSatisfied();

        List<?> titles = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertEquals(2, titles.size());
        assertEquals("Sword's of Honour", titles.get(0));
        assertEquals("Camels in Space", titles.get(1));
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
                public void configure() throws Exception {
                    from("direct:start")
                            .transform().jsonpath("$.store.book[?(@.author == '{{who}}' || @.title == '{{search}}')].title")
                            .to("mock:authors");
                }
            };
        }
    }
}
