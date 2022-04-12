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
import org.testcontainers.shaded.org.awaitility.Awaitility;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//Based on S3ConsumerIT
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                S3MoveAfterReadConsumerTest.class,
                S3MoveAfterReadConsumerTest.TestConfiguration.class
        }
)
public class S3MoveAfterReadConsumerTest extends BaseS3 {

    @EndpointInject("mock:result")
    private MockEndpoint result;

    @Test
    public void sendIn() throws Exception {
        result.expectedMessageCount(3);

        template.send("direct:putObject", new Processor() {

            @Override
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "test.txt");
                exchange.getIn().setBody("Test");
            }
        });

        template.send("direct:putObject", new Processor() {

            @Override
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "test1.txt");
                exchange.getIn().setBody("Test1");
            }
        });

        template.send("direct:putObject", new Processor() {

            @Override
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "test2.txt");
                exchange.getIn().setBody("Test2");
            }
        });

        assertMockEndpointsSatisfied(10_000);

        //wait for finish
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
            () -> {
                List<S3Object> results = template.request("direct:listObjects", new Processor() {
                    @Override
                    public void process(Exchange exchange) {
                        exchange.getIn().setHeader(AWS2S3Constants.BUCKET_NAME, "already-read");
                        exchange.getIn().setHeader(AWS2S3Constants.S3_OPERATION, AWS2S3Operations.listObjects);
                    }
                }).getMessage().getBody(List.class);

                return results.size() == 3 && "movedPrefixtest.txtmovedSuffix".equals(results.get(0).key());
            });
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  BaseS3.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            final  String awsEndpoint = "aws2-s3://mycamel?autoCreateBucket=true";

            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:putObject").startupOrder(1).to(awsEndpoint).to("mock:result");

                    from("aws2-s3://mycamel?moveAfterRead=true&destinationBucket=already-read&autoCreateBucket=true&destinationBucketPrefix=RAW(movedPrefix)&destinationBucketSuffix=RAW(movedSuffix)")
                            .to("log:foo");

                    from("direct:listObjects").to("aws2-s3://mycamel?autoCreateBucket=false");
                }
            };
        }
    }
}
