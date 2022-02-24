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
package org.apache.camel.component.jackson.springboot.test;


import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;



@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        JacksonMarshalContentTypeHeaderTest.class,
        JacksonMarshalContentTypeHeaderTest.TestConfiguration.class
    }
)

public class JacksonMarshalContentTypeHeaderTest {

    @Autowired
    @Produce("direct:start")
    ProducerTemplate template;

    @EndpointInject("mock:reversePojo")
    MockEndpoint mock;
    
    @Test
    public void testYes() throws Exception {
        final Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        Exchange out = template.request("direct:yes", exchange -> exchange.getIn().setBody(in));

        assertNotNull(out);
        assertTrue(out.hasOut());
        assertEquals("application/json", out.getMessage().getHeader(Exchange.CONTENT_TYPE));
    }

    @Test
    public void testYes2() throws Exception {
        final Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        Exchange out = template.request("direct:yes2", exchange -> exchange.getIn().setBody(in));

        assertNotNull(out);
        assertTrue(out.hasOut());
        assertEquals("application/json", out.getMessage().getHeader(Exchange.CONTENT_TYPE));
    }

    @Test
    public void testNo() throws Exception {
        final Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        Exchange out = template.request("direct:no", exchange -> exchange.getIn().setBody(in));

        assertNotNull(out);
        assertTrue(out.hasOut());
        assertNull(out.getMessage().getHeader(Exchange.CONTENT_TYPE));
    }

    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    JacksonDataFormat format = new JacksonDataFormat();
                    from("direct:yes").marshal(format);

                    from("direct:yes2").marshal().json(JsonLibrary.Jackson);

                    JacksonDataFormat formatNoHeader = new JacksonDataFormat();
                    formatNoHeader.setContentTypeHeader(false);
                    from("direct:no").marshal(formatNoHeader);
                }
            };
        }
        
      
    }
    
    
    
    
}
