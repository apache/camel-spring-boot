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
import org.apache.camel.dataformat.bindy.model.tab.PurchaseOrder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        BindyTabSeparatorTest.class,
        BindyTabSeparatorTest.TestConfiguration.class
    }
)
public class BindyTabSeparatorTest {

    @Autowired
    ProducerTemplate template;
    
    @EndpointInject("mock:marshal")
    MockEndpoint marshal;
    
    @EndpointInject("mock:unmarshal")
    MockEndpoint unmarshal;
    
    
    @Test
    public void testUnmarshal() throws Exception {
        unmarshal.reset();
        unmarshal.expectedMessageCount(1);

        template.sendBody("direct:unmarshal", "123\tCamel in Action\t2\tPlease hurry\tJane Doe\tJohn Doe\n");

        unmarshal.assertIsSatisfied();

        PurchaseOrder order = unmarshal.getReceivedExchanges().get(0).getIn().getBody(PurchaseOrder.class);

        assertEquals(123, order.getId());
        assertEquals("Camel in Action", order.getName());
        assertEquals(2, order.getAmount());
        assertEquals("Please hurry", order.getOrderText());
        assertEquals("Jane Doe", order.getSalesRef());
        assertEquals("John Doe", order.getCustomerRef());
    }

    @Test
    public void testMarshal() throws Exception {
        marshal.reset();
        marshal.expectedBodiesReceived("123\tCamel in Action\t2\tPlease hurry\tJane Doe\tJohn Doe\n");

        PurchaseOrder order = new PurchaseOrder();
        order.setId(123);
        order.setName("Camel in Action");
        order.setAmount(2);
        order.setOrderText("Please hurry");
        order.setSalesRef("Jane Doe");
        order.setCustomerRef("John Doe");

        template.sendBody("direct:marshal", order);

        marshal.assertIsSatisfied();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnmarshalEmptyTrailingNoneRequiredFields() throws Exception {
        unmarshal.reset();
        unmarshal.expectedMessageCount(1);

        template.sendBodyAndHeader("direct:unmarshal",
                "123\tCamel in Action\t2\t\t\n"
                                                       + "456\tCamel in Action\t1\t\t\t\n"
                                                       + "456\tCamel in Action\t2\t\t\n"
                                                       + "456\tCamel in Action\t1\t\t\t\n",
                Exchange.CONTENT_ENCODING, "iso8859-1");

        unmarshal.assertIsSatisfied();

        List<PurchaseOrder> orders = (List<PurchaseOrder>) unmarshal.getReceivedExchanges().get(0).getIn().getBody();
        PurchaseOrder order = orders.get(0);

        assertEquals(123, order.getId());
        assertEquals("Camel in Action", order.getName());
        assertEquals(2, order.getAmount());
        assertEquals("", order.getOrderText());
        assertEquals("", order.getSalesRef());
        assertNull(order.getCustomerRef());
    }

    @Test
    public void testMarshalEmptyTrailingNoneRequiredFields() throws Exception {
        marshal.reset();
        marshal.expectedBodiesReceived("123\tCamel in Action\t2\t\t\t\n");

        PurchaseOrder order = new PurchaseOrder();
        order.setId(123);
        order.setName("Camel in Action");
        order.setAmount(2);
        order.setOrderText("");
        order.setSalesRef("");
        order.setCustomerRef("");

        template.sendBody("direct:marshal", order);

        marshal.assertIsSatisfied();
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
                    BindyCsvDataFormat bindy
                            = new BindyCsvDataFormat(org.apache.camel.dataformat.bindy.model.tab.PurchaseOrder.class);

                    from("direct:marshal")
                            .marshal(bindy)
                            .convertBodyTo(String.class)
                            .to("mock:marshal");

                    from("direct:unmarshal")
                            .unmarshal(bindy)
                            .to("mock:unmarshal");
                }
            };
        }
    }
    
    

}
