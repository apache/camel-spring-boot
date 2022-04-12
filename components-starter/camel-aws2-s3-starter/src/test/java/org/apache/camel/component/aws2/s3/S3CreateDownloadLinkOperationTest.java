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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

//Based on S3CreateDownloadLinkOperationIT
@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                S3CreateDownloadLinkOperationTest.class,
                S3CreateDownloadLinkOperationTest.TestConfiguration.class
        }
)
public class S3CreateDownloadLinkOperationTest extends BaseS3 {

    @EndpointInject("mock:result")
    private MockEndpoint result;

    @SuppressWarnings("unchecked")
    @Test
    public void sendIn() throws Exception {
        result.expectedMessageCount(1);

        template.send("direct:addObject", ExchangePattern.InOnly, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "CamelUnitTest2");
                exchange.getIn().setBody("This is my bucket content.");
                exchange.getIn().removeHeader(AWS2S3Constants.S3_OPERATION);
            }
        });

        Exchange ex1 = template.request("direct:createDownloadLink", new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "CamelUnitTest2");
                exchange.getIn().setHeader(AWS2S3Constants.BUCKET_NAME, "mycamel2");
                exchange.getIn().setHeader(AWS2S3Constants.S3_OPERATION, AWS2S3Operations.createDownloadLink);
            }
        });

        Exchange ex2 = template.request("direct:createDownloadLinkWithoutCredentials", new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(AWS2S3Constants.KEY, "CamelUnitTest2");
                exchange.getIn().setHeader(AWS2S3Constants.BUCKET_NAME, "mycamel2");
                exchange.getIn().setHeader(AWS2S3Constants.S3_OPERATION, AWS2S3Operations.createDownloadLink);
            }
        });

        assertNotNull(ex1.getMessage().getBody());
        assertNull(ex2.getMessage().getBody());
        assertMockEndpointsSatisfied();
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
                    String awsEndpoint = "aws2-s3://mycamel2?autoCreateBucket=true";

                    from("direct:addObject").to(awsEndpoint);

                    from("direct:createDownloadLinkWithoutCredentials").to(awsEndpoint).to("mock:result");

                    from("direct:createDownloadLink").to(awsEndpoint + "&accessKey=xxx&secretKey=yyy&region=eu-west-1")
                            .to("mock:result");
                }
            };
        }
    }


}
