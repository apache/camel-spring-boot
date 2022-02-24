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
package org.apache.camel.component.hl7.springboot.test;



import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7MLLPCodec;
import org.apache.camel.component.mock.MockEndpoint;
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
        HL7MLLPCodecPlainStringTest.class,
        HL7MLLPCodecPlainStringTest.TestConfiguration.class
    }
)
public class HL7MLLPCodecPlainStringTest extends HL7TestSupport {

    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:result")
    MockEndpoint mock;

    
    @Bean("hl7codec")
    private HL7MLLPCodec addCodec() throws Exception {

        HL7MLLPCodec codec = new HL7MLLPCodec();
        codec.setCharset("iso-8859-1");

        return codec;
    }

    @Test
    public void testPlainString() throws Exception {
        // START SNIPPET: e1
        mock.expectedBodiesReceived("Bye World");

        // send plain hello world as String
        Object out = template.requestBody("mina:tcp://127.0.0.1:" + getPort() + "?sync=true&codec=#hl7codec", "Hello World",
                String.class);

        mock.assertIsSatisfied();

        // and the response is also just plain String
        assertEquals("Bye World", out);
        // END SNIPPET: e1
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
                    // START SNIPPET: e2
                    from("mina:tcp://127.0.0.1:" + getPort() + "?sync=true&codec=#hl7codec").process(exchange -> {
                        // use plain String as message format
                        String body = exchange.getIn().getBody(String.class);
                        assertEquals("Hello World", body);

                        // return the response as plain string
                        exchange.getMessage().setBody("Bye World");
                    }).to("mock:result");
                    // END SNIPPET: e2
                }
            };
        }
    }
    
    
}
