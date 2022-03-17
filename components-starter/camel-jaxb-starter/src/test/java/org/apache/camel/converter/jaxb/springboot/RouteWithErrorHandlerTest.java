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



import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.example.InvalidOrderException;
import org.apache.camel.example.PurchaseOrder;
import org.apache.camel.spi.DataFormat;
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
        RouteWithErrorHandlerTest.class,
        RouteWithErrorHandlerTest.TestConfiguration.class
    }
)
public class RouteWithErrorHandlerTest {
    
    @Autowired
    CamelContext context;
        
    @Autowired
    ProducerTemplate template;

    
    @EndpointInject("mock:wine")
    private MockEndpoint mockWine;

    @EndpointInject("mock:invalid")
    private MockEndpoint mockInvalid;
    
    @EndpointInject("mock:result")
    private MockEndpoint mockResult;
    
    @EndpointInject("mock:error")
    private MockEndpoint mockError;

    
    @Test
    public void testOk() throws Exception {
        MockEndpoint.resetMocks(context);
        PurchaseOrder order = new PurchaseOrder();
        order.setName("Wine");
        order.setAmount(123.45);
        order.setPrice(2.22);

        
        mockWine.expectedBodiesReceived(order);

        template.sendBody("direct:start", "<purchaseOrder name='Wine' amount='123.45' price='2.22'/>");

        mockWine.assertIsSatisfied();
    }

    @Test
    public void testUnmarshalError() throws Exception {
        MockEndpoint.resetMocks(context);
        mockError.expectedMessageCount(1);
        mockError.message(0).body(String.class).contains("<foo");
        mockInvalid.expectedMessageCount(0);
        mockResult.expectedMessageCount(0);

        template.sendBody("direct:start", "<foo/>");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    public void testNotWine() throws Exception {
        MockEndpoint.resetMocks(context);
        PurchaseOrder order = new PurchaseOrder();
        order.setName("Beer");
        order.setAmount(2);
        order.setPrice(1.99);

        
        mockInvalid.expectedBodiesReceived(order);
        mockError.expectedMessageCount(0);
        mockResult.expectedMessageCount(0);

        template.sendBody("direct:start", "<purchaseOrder name='Beer' amount='2.0' price='1.99'/>");

        MockEndpoint.assertIsSatisfied(context);
    }

    
    public static boolean isWine(PurchaseOrder order) {
        return "Wine".equalsIgnoreCase(order.getName());
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
                    errorHandler(deadLetterChannel("mock:error").redeliveryDelay(0));

                    onException(InvalidOrderException.class).maximumRedeliveries(0).handled(true)
                            .to("mock:invalid");

                    DataFormat jaxb = new JaxbDataFormat("org.apache.camel.example");

                    from("direct:start")
                            .unmarshal(jaxb)
                            .choice()
                                .when().method(RouteWithErrorHandlerTest.class, "isWine").to("mock:wine")
                                .otherwise().throwException(new InvalidOrderException("We only like wine"))
                            .end();
                }
            };
        }
    }
}
