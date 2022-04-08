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

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.dataformat.bindy.model.simple.spanLastRecord.SpanLastRecord;
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
        BindySimpleCsvAutospanLineTest.class,
        BindySimpleCsvAutospanLineTest.TestConfiguration.class
    }
)
public class BindySimpleCsvAutospanLineTest {

    @Autowired
    ProducerTemplate template;
    
    @EndpointInject("mock:unmarshal")
    MockEndpoint mock;
    
    @Test
    public void testUnmarshalNoNeedToSpanLine() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        template.sendBody("direct:unmarshal", "1,hei,kommentar");

        mock.assertIsSatisfied();

        //final List<Map<?, SpanLastRecord>> rows = CastUtils.cast(mock.getReceivedExchanges().get(0).getIn().getBody(List.class));
        //final SpanLastRecord order = rows.get(0).get(SpanLastRecord.class.getName());

        final SpanLastRecord order = mock.getReceivedExchanges().get(0).getIn().getBody(SpanLastRecord.class);

        assertEquals(1, order.getRecordId());
        assertEquals("hei", order.getName());
        assertEquals("kommentar", order.getComment());
    }

    @Test
    public void testUnmarshalSpanningLine() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        template.sendBody("direct:unmarshal", "1,hei,kommentar,test,noe,hei");

        mock.assertIsSatisfied();

        final SpanLastRecord order = mock.getReceivedExchanges().get(0).getIn().getBody(SpanLastRecord.class);

        assertEquals(1, order.getRecordId());
        assertEquals("hei", order.getName());
        assertEquals("kommentar,test,noe,hei", order.getComment());
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
                    final BindyCsvDataFormat bindy = new BindyCsvDataFormat(SpanLastRecord.class);

                    from("direct:unmarshal")
                            .unmarshal(bindy)
                            .to("mock:unmarshal");
                }
            };
        }
    }
    
    

}
