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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//based on S3StreamUploadOperationIT
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                S3StreamUploadOperationTest.class,
                S3StreamUploadOperationTest.TestConfiguration.class
        }
)
public class S3StreamUploadOperationTest extends BaseS3 {


    @EndpointInject("mock:result")
    private MockEndpoint result;

    @Test
    public void sendIn() throws Exception {
        result.expectedMessageCount(1000);

        for (int i = 0; i < 1000; i++) {
            template.sendBody("direct:stream1", "Andrea\n");
        }

        assertMockEndpointsSatisfied();

        Exchange ex = template.request("direct:listObjects", new Processor() {

            @Override
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.S3_OPERATION, AWS2S3Operations.listObjects);
            }
        });

        List<S3Object> resp = ex.getMessage().getBody(List.class);
        assertEquals(40, resp.size());
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
                    String awsEndpoint1
                            = "aws2-s3://mycamel-1?autoCreateBucket=true&streamingUploadMode=true&keyName=fileTest.txt&batchMessageNumber=25&namingStrategy=random";

                    from("direct:stream1").to(awsEndpoint1).to("mock:result");

                    String awsEndpoint = "aws2-s3://mycamel-1?autoCreateBucket=true";

                    from("direct:listObjects").to(awsEndpoint);
                }
            };
        }
    }

}
