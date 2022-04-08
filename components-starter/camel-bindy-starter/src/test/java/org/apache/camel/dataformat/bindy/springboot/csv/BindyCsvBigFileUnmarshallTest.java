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
package org.apache.camel.dataformat.bindy.springboot.csv;

import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
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
        BindyCsvBigFileUnmarshallTest.class,
        BindyCsvBigFileUnmarshallTest.TestConfiguration.class
    }
)
public class BindyCsvBigFileUnmarshallTest {

    @Autowired
    ProducerTemplate template;
    
    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @Test
    public void testUnMarshallMessage() throws Exception {
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.assertIsSatisfied();

        assertCountRecords();
    }

    protected void assertCountRecords() {
        Exchange exchange = resultEndpoint.getExchanges().get(0);

        List<?> models = exchange.getIn().getBody(List.class);
        assertEquals(10000, models.size());
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
                    BindyCsvDataFormat camelDataFormat
                            = new BindyCsvDataFormat(org.apache.camel.dataformat.bindy.model.simple.oneclass.Order.class);
                    from("file://src/test/data/big?noop=true").unmarshal(camelDataFormat).to("mock:result");
                }
            };
        }
    }
    
    

}
