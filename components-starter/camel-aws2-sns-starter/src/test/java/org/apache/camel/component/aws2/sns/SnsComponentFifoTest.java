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
package org.apache.camel.component.aws2.sns;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperties;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// Based on SnsComponentFifoManualIT
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SnsComponentFifoTest.class,
                SnsComponentFifoTest.TestConfiguration.class
        }
)
public class SnsComponentFifoTest extends BaseSns {

    @Test
    public void sendInOnly() {
        Exchange exchange = producerTemplate.send("direct:start", ExchangePattern.InOnly, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(Sns2Constants.SUBJECT, "This is my subject");
                exchange.getIn().setBody("This is my message text.");
            }
        });

        assertNotNull(exchange.getIn().getHeader(Sns2Constants.MESSAGE_ID));
    }

    @Test
    public void sendInOut() {
        Exchange exchange = producerTemplate.send("direct:start", ExchangePattern.InOut, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(Sns2Constants.SUBJECT, "This is my subject");
                exchange.getIn().setBody("This is my message text.");
            }
        });

        assertNotNull(exchange.getMessage().getHeader(Sns2Constants.MESSAGE_ID));
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  BaseSns.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {

            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start")
                            .toF("aws2-sns://%s.fifo?subject=The+subject+message&messageGroupIdStrategy=useExchangeId&autoCreateTopic=true", sharedNameGenerator.getName());
                }
            };
        }
    }

}
