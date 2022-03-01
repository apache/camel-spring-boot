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
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        JsonPathSplitTest.class,
        JsonPathSplitTest.TestConfiguration.class
    }
)
public class JsonPathSplitTest {

    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:authors")
    MockEndpoint mock;
    
    @EndpointInject("mock:result-1")
    MockEndpoint mock1;
    
    @EndpointInject("mock:result-2")
    MockEndpoint mock2;
    
    @EndpointInject("mock:result-3")
    MockEndpoint mock3;

    @Test
    public void testSplit() throws Exception {
        mock.expectedMessageCount(3);

        String out = template.requestBody("direct:start", new File("src/test/resources/books.json"), String.class);
        assertNotNull(out);

        mock.assertIsSatisfied();

        Map row = mock.getReceivedExchanges().get(0).getIn().getBody(Map.class);
        assertEquals("Nigel Rees", row.get("author"));
        assertEquals(Double.valueOf("8.95"), row.get("price"));

        row = mock.getReceivedExchanges().get(1).getIn().getBody(Map.class);
        assertEquals("Evelyn Waugh", row.get("author"));
        assertEquals(Double.valueOf("12.99"), row.get("price"));

        // should preserve quotes etc
        assertTrue(out.contains("\"author\": \"Nigel Rees\""));
        assertTrue(out.contains("\"price\": 8.95"));
        assertTrue(out.contains("\"title\": \"Sword's of Honour\""));
        assertTrue(out.contains("\"price\": 12.99,"));
    }

    @Test
    public void testJsonPathSplitDelimiterDisable() throws Exception {
        // CAMEL-15769
        String json = "{ \"text\": { \"div\": \"some , text\" } }";
        
        mock1.expectedPropertyReceived("CamelSplitSize", 1);
        mock1.expectedBodiesReceived("some , text");
        template.sendBody("direct:start-1", json);
        mock1.assertIsSatisfied();
    }

    @Test
    public void testJsonPathSplitDelimiterDefault() throws Exception {
        // CAMEL-15769
        String json = "{ \"text\": { \"div\": \"some , text\" } }";
        
        mock2.expectedPropertyReceived("CamelSplitSize", 2);
        mock2.expectedBodiesReceived("some ", " text");
        template.sendBody("direct:start-2", json);
        mock2.assertIsSatisfied();
    }

    @Test
    public void testJsonPathSplitDelimiter() throws Exception {
        // CAMEL-15769
        String json = "{ \"text\": { \"div\": \"some ,# text\" } }";
        
        mock3.expectedPropertyReceived("CamelSplitSize", 2);
        mock3.expectedBodiesReceived("some ,", " text");
        template.sendBody("direct:start-3", json);
        mock3.assertIsSatisfied();
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
                            .split().jsonpath("$.store.book[*]")
                            .to("mock:authors")
                            .convertBodyTo(String.class);

                    from("direct:start-1")
                            .split(jsonpath("text.div", String.class), "false")
                            .to("mock:result-1");

                    from("direct:start-2")
                            .split(jsonpath("text.div", String.class))
                            .to("mock:result-2");

                    from("direct:start-3")
                            .split(jsonpath("text.div", String.class), "#")
                            .to("mock:result-3");
                }
            };
        }
    }
}
