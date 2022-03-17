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
package org.apache.camel.jaxb;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConversionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.example.Bar;
import org.apache.camel.example.Foo;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        FallbackTypeConverterShouldThrowExceptionTest.class
    }
)
public class FallbackTypeConverterShouldThrowExceptionTest  {

    private static AtomicInteger failed = new AtomicInteger();
    private static AtomicInteger failed2 = new AtomicInteger();
    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
   
    @EndpointInject("mock:a")
    private MockEndpoint mockA;
    
    @EndpointInject("mock:b")
    private MockEndpoint mockB;
    
    @BeforeEach
    private void setup() {
        failed = new AtomicInteger();
        failed2 = new AtomicInteger();
    }

    @Test
    public void testJaxbModel() throws Exception {
        MockEndpoint.resetMocks(context);
        Object foo = new Foo();
        mockA.expectedBodiesReceived(foo);
        mockB.expectedBodiesReceived(foo);

        template.sendBody("direct:a", foo);

        MockEndpoint.assertIsSatisfied(context);

        assertEquals(0, failed.get());
        assertEquals(0, failed2.get());
    }

    @Test
    public void testNoneJaxbModel() throws Exception {
        MockEndpoint.resetMocks(context);
        Object camel = "Camel";
        mockA.expectedBodiesReceived(camel);
        mockB.expectedBodiesReceived(camel);

        template.sendBody("direct:a", camel);

        MockEndpoint.assertIsSatisfied(context);

        assertEquals(1, failed.get());
        assertEquals(0, failed2.get());
    }

    @Test
    public void testAnotherJaxbModel() throws Exception {
        MockEndpoint.resetMocks(context);
        Object bar = new Bar();
        mockA.expectedBodiesReceived(bar);
        mockB.expectedBodiesReceived(bar);

        template.sendBody("direct:a", bar);

        MockEndpoint.assertIsSatisfied(context);

        assertEquals(1, failed.get());
        assertEquals(0, failed2.get());
    }

    @Bean
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder(context) {

            @Override
            public void configure() throws Exception {
                from("direct:a").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        try {
                            exchange.getIn().getBody(Foo.class);
                        } catch (TypeConversionException e) {
                            failed.incrementAndGet();
                        }
                    }
                }).to("mock:a").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        try {
                            exchange.getIn().getBody(List.class);
                        } catch (TypeConversionException e) {
                            // there is no type converters from the POJO -> List
                            // so we should really not fail at all at this point
                            failed2.incrementAndGet();
                        }
                    }

                }).to("mock:b");
            }

        };
    }

}
