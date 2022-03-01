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


import static org.apache.camel.test.junit5.TestSupport.assertIsInstanceOf;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
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
        JsonPathMapTransformTest.class,
        JsonPathMapTransformTest.TestConfiguration.class
    }
)
public class JsonPathMapTransformTest {

    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:authors")
    MockEndpoint mock;

    @Test
    public void testAuthors() throws Exception {
        mock.expectedMessageCount(1);

        // should be a map
        Configuration.ConfigurationBuilder builder = Configuration.builder();
        builder.jsonProvider(new JacksonJsonProvider());
        builder.mappingProvider(new JacksonMappingProvider());
        Object document = builder.build().jsonProvider()
                .parse(new FileInputStream("src/test/resources/books.json"), "utf-8");
        assertIsInstanceOf(Map.class, document);

        template.sendBody("direct:start", document);

        mock.assertIsSatisfied();

        List<?> authors = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertEquals("Nigel Rees", authors.get(0));
        assertEquals("Evelyn Waugh", authors.get(1));
    }
    

    // *************************************
    // Config
    // *************************************

    @org.springframework.context.annotation.Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                            .transform().jsonpath("$.store.book[*].author")
                            .to("mock:authors");
                }
            };
        }
    }
}
