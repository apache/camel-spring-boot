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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

//Based on S3ObjectRangeOperationManualIT
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                S3ObjectRangeOperationTest.class,
                S3ObjectRangeOperationTest.TestConfiguration.class
        }
)
public class S3ObjectRangeOperationTest extends BaseS3 {
    private static final Logger LOG = LoggerFactory.getLogger(S3ObjectRangeOperationTest.class);

    @EndpointInject("mock:result")
    private MockEndpoint result;

    @Test
    public void sendIn() throws Exception {
        result.expectedMessageCount(1);

        template.send("direct:putObject", new Processor() {

            @Override
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "element.txt");
                exchange.getIn().setBody("0123456789");
            }
        });


        template.send("direct:getObjectRange", new Processor() {

            @Override
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "element.txt");
                exchange.getIn().setHeader(AWS2S3Constants.RANGE_START, 0);
                exchange.getIn().setHeader(AWS2S3Constants.RANGE_END, 2);
            }
        });
        assertMockEndpointsSatisfied();
        Exchange exchange = result.getExchanges().get(0);
        ResponseInputStream<GetObjectResponse> s3 = exchange.getIn().getBody(ResponseInputStream.class);
        Assertions.assertEquals("012", readInputStream(s3));
    }

    private String readInputStream(ResponseInputStream<GetObjectResponse> s3Object) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(s3Object, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        return textBuilder.toString();
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  BaseS3.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder(S3Client s3Client) {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    String awsEndpoint = "aws2-s3://mycamelbucket?operation=getObjectRange&autoCreateBucket=false";
                    String awsOriginalEndpoint = "aws2-s3://mycamelbucket?autoCreateBucket=true";

                    from("direct:putObject").startupOrder(1).to(awsOriginalEndpoint);

                    from("direct:getObjectRange").startupOrder(2).to(awsEndpoint).to("mock:result");
                }
            };
        }
    }
}
