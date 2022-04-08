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
import org.apache.camel.dataformat.bindy.util.ConverterUtils;
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
        BindyMarshalUnmarshalWithDefaultValueTest.class,
        BindyMarshalUnmarshalWithDefaultValueTest.TestConfiguration.class
    }
)
public class BindyMarshalUnmarshalWithDefaultValueTest {

    
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:resultMarshal")
    MockEndpoint mockMarshal;
    
    @EndpointInject("mock:resultUnmarshal")
    MockEndpoint mockUnmarshal;

    
    @Test
    public void testBindyMarshalWithDefaultValue() throws Exception {
        
        mockMarshal.expectedBodiesReceived(
                "\"123\",\"Wednesday, November 9, 2011\",\"North Pole\"" + ConverterUtils.getStringCarriageReturn("WINDOWS"));

        WeatherModel model = new WeatherModel();
        model.setId(123);
        model.setDate("Wednesday, November 9, 2011");

        template.sendBody("direct:marshal", model);

        mockMarshal.assertIsSatisfied();
    }

    @Test
    public void testBindyUnmarshalWithDefaultValue() {
        

        String request = "\"123\",\"Wednesday, November 9, 2011\",\"\"" + ConverterUtils.getStringCarriageReturn("WINDOWS");
        template.sendBody("direct:unmarshal", request);
        WeatherModel answer = mockUnmarshal.getReceivedExchanges().get(0).getIn().getBody(WeatherModel.class);
        assertEquals(123, answer.getId());
        assertEquals("Wednesday, November 9, 2011", answer.getDate());
        assertEquals("North Pole", answer.getPlace());
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
                    from("direct:marshal")
                            .marshal().bindy(BindyType.Csv, WeatherModel.class)
                            .to("mock:resultMarshal");
                    from("direct:unmarshal")
                            .unmarshal().bindy(BindyType.Csv, WeatherModel.class)
                            .to("mock:resultUnmarshal");
                }
            };
        }
    }
}
