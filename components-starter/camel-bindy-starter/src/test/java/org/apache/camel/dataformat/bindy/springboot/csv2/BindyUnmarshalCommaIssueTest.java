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
package org.apache.camel.dataformat.bindy.springboot.csv2;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.dataformat.BindyType;
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
        BindyUnmarshalCommaIssueTest.class,
        BindyUnmarshalCommaIssueTest.TestConfiguration.class
    }
)
public class BindyUnmarshalCommaIssueTest {

    
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testBindyUnmarshalNoCommaIssue() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        String body = "123,\"Wednesday November 9 2011\",\"Central California\"";
        template.sendBody("direct:start", body);

        mock.assertIsSatisfied();

        WeatherModel model = mock.getReceivedExchanges().get(0).getIn().getBody(WeatherModel.class);

        assertEquals(123, model.getId());
        assertEquals("Wednesday November 9 2011", model.getDate());
        assertEquals("Central California", model.getPlace());
    }

    @Test
    public void testBindyUnmarshalCommaIssue() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        String body = "123,\"Wednesday, November 9, 2011\",\"Central California\"";
        template.sendBody("direct:start", body);

        mock.assertIsSatisfied();

        WeatherModel model = mock.getReceivedExchanges().get(0).getIn().getBody(WeatherModel.class);

        assertEquals(123, model.getId());
        assertEquals("Wednesday, November 9, 2011", model.getDate());
        assertEquals("Central California", model.getPlace());
    }

    @Test
    public void testBindyUnmarshalCommaIssueTwo() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        String body = "123,\"Wednesday, November 9, 2011\",\"Central California, United States\"";
        template.sendBody("direct:start", body);

        mock.assertIsSatisfied();

        WeatherModel model = mock.getReceivedExchanges().get(0).getIn().getBody(WeatherModel.class);

        assertEquals(123, model.getId());
        assertEquals("Wednesday, November 9, 2011", model.getDate());
        assertEquals("Central California, United States", model.getPlace());
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
                    from("direct:start")
                            .unmarshal().bindy(BindyType.Csv, WeatherModel.class)
                            .to("mock:result");
                }
            };
        }
    }
}
