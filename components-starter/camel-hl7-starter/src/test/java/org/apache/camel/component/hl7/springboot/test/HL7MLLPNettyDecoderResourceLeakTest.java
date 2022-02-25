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
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7MLLPCodec;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ca.uhn.hl7v2.model.Message;
import io.netty.util.ResourceLeakDetector;

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
        HL7MLLPNettyDecoderResourceLeakTest.class,
        HL7MLLPNettyDecoderResourceLeakTest.TestConfiguration.class
    }
)
public class HL7MLLPNettyDecoderResourceLeakTest extends HL7TestSupport {

    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:result")
    MockEndpoint mock;

    @Bean("hl7decoder")
    private HL7MLLPNettyDecoderFactory getHL7MLLPNettyDecoderFactory() {
        return new HL7MLLPNettyDecoderFactory();
    }

    @Bean("hl7encoder")
    private HL7MLLPNettyEncoderFactory getHL7MLLPNettyEncoderFactory() {
        return new HL7MLLPNettyEncoderFactory();
    }

    @BeforeAll
    // As the ResourceLeakDetector just write error log when it find the leak,
    // We need to check the log file to see if there is a leak.
    public static void enableNettyResourceLeakDetector() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }
    
    @Test
    public void testSendHL7Message() throws Exception {
        assertDoesNotThrow(() -> sendHL7Message());
    }

    private void sendHL7Message() {
        String message = "MSH|^~\\&|MYSENDER|MYRECEIVER|MYAPPLICATION||200612211200||QRY^A19|1234|P|2.4";

        for (int i = 0; i < 10; i++) {
            template.sendBody("netty:tcp://127.0.0.1:" + getPort() + "?decoders=#hl7decoder&encoders=#hl7encoder", message);
        }
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
                    from("netty:tcp://127.0.0.1:" + getPort() + "?decoders=#hl7decoder&encoders=#hl7encoder")
                            .process(new Processor() {
                                public void process(Exchange exchange) throws Exception {
                                    Message input = exchange.getIn().getBody(Message.class);
                                    exchange.getOut().setBody(input.generateACK());
                                }
                            }).to("mock:result");
                }
            };
        }
    }
    
    @Bean(name = "hl7codec")
    private HL7MLLPCodec addHl7MllpCodec() throws Exception {
        HL7MLLPCodec codec = new HL7MLLPCodec();
        codec.setCharset("iso-8859-1");
        return codec;
    }
}
