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
package org.apache.camel.component.aws2.s3;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Based on S3ComponentManualIT
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                S3ComponentTest.class,
                S3ComponentTest.TestConfiguration.class
        }
)
public class S3ComponentTest extends BaseS3 {


    @EndpointInject("mock:result")
    private MockEndpoint result;

    @Test
    public void sendInOnly() throws Exception {
        result.reset();
        result.expectedMessageCount(2);

        Exchange exchange1 = template.send("direct:start", ExchangePattern.InOnly, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "CamelUnitTest1");
                exchange.getIn().setBody("This is my bucket content.");
            }
        });

        Exchange exchange2 = template.send("direct:start", ExchangePattern.InOnly, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "CamelUnitTest2");
                exchange.getIn().setBody("This is my bucket content.");
            }
        });

        assertMockEndpointsSatisfied(10_000);

        assertResultExchange(result.getExchanges().get(0));
        assertResultExchange(result.getExchanges().get(1));

        assertResponseMessage(exchange1.getIn());
        assertResponseMessage(exchange2.getIn());
    }

    @Test
    public void sendInOut() throws Exception {
        result.reset();
        result.expectedMessageCount(1);

        Exchange exchange = template.send("direct:start", ExchangePattern.InOut, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "CamelUnitTest");
                exchange.getIn().setBody("This is my bucket content.");
            }
        });

        assertMockEndpointsSatisfied();

        assertResultExchange(result.getExchanges().get(0));

        assertResponseMessage(exchange.getMessage());
    }

    private void assertResultExchange(Exchange resultExchange) {
        assertEquals("This is my bucket content.", resultExchange.getIn().getBody(String.class));
        assertEquals("mycamelbucket", resultExchange.getIn().getHeader(AWS2S3Constants.BUCKET_NAME));
        assertTrue(resultExchange.getIn().getHeader(AWS2S3Constants.KEY, String.class).startsWith("CamelUnitTest"));
        assertEquals("null", resultExchange.getIn().getHeader(AWS2S3Constants.VERSION_ID));
        // enabled
        // on
        // this
        // bucket
        assertNotNull(resultExchange.getIn().getHeader(AWS2S3Constants.LAST_MODIFIED));
        assertEquals("application/octet-stream", resultExchange.getIn().getHeader(AWS2S3Constants.CONTENT_TYPE));
        assertNull(resultExchange.getIn().getHeader(AWS2S3Constants.CONTENT_ENCODING));
        assertEquals(26L, resultExchange.getIn().getHeader(AWS2S3Constants.CONTENT_LENGTH));
        assertNull(resultExchange.getIn().getHeader(AWS2S3Constants.CONTENT_DISPOSITION));
        assertNull(resultExchange.getIn().getHeader(AWS2S3Constants.CONTENT_MD5));
        assertNull(resultExchange.getIn().getHeader(AWS2S3Constants.CACHE_CONTROL));
    }

    private void assertResponseMessage(Message message) {
        assertNull(message.getHeader(AWS2S3Constants.VERSION_ID));
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  BaseS3.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            final String s3EndpointUri
                    = "aws2-s3://mycamelbucket?autoCreateBucket=true";
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start").log("start start start start {body}").to(s3EndpointUri);

                    from(s3EndpointUri).log("finish finish finish fiisht {body}").to("mock:result");
                }
            };
        }
    }
}
