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

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;


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
        JsonPathSplitWriteAsStringTest.class,
        JsonPathSplitWriteAsStringTest.TestConfiguration.class
    }
)
public class JsonPathSplitWriteAsStringTest {

    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:line")
    MockEndpoint mock;

    @Test
    public void testSplitToJSon() throws Exception {
        
        mock.expectedMessageCount(2);
        // we want the output as JSON string
        mock.allMessages().body().isInstanceOf(String.class);
        mock.message(0).body().isEqualTo("{\"action\":\"CU\",\"id\":123,\"modifiedTime\":\"2015-07-28T11:40:09.520+02:00\"}");
        mock.message(1).body().isEqualTo("{\"action\":\"CU\",\"id\":456,\"modifiedTime\":\"2015-07-28T11:42:29.510+02:00\"}");

        template.sendBody("direct:start", new File("src/test/resources/content.json"));

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
                public void configure() throws Exception {
                    from("direct:start")
                            .split().jsonpathWriteAsString("$.content")
                            .to("mock:line")
                            .to("log:line")
                            .end();
                }
            };
        }
    }
}
