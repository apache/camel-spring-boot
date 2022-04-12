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
package org.apache.camel.component.aws2.cw;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse;

import java.time.Instant;

//Based on CwComponentIT
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                CwComponentTest.class,
                CwComponentTest.TestConfiguration.class
        }
)
public class CwComponentTest extends BaseCw {

    @EndpointInject("mock:result")
    private MockEndpoint mock;

    @Test
    public void sendInOnly() throws Exception {
        mock.expectedMessageCount(1);

        Exchange e = template.send("direct:start", ExchangePattern.InOnly, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(Cw2Constants.METRIC_NAME, "ExchangesCompleted");
                exchange.getIn().setHeader(Cw2Constants.METRIC_DIMENSION_NAME, "dimension");
                exchange.getIn().setHeader(Cw2Constants.METRIC_VALUE, "2.0");
                exchange.getIn().setHeader(Cw2Constants.METRIC_UNIT, "Count");
            }
        });

        assertMockEndpointsSatisfied();

        CloudWatchClient client = AWSSDKClientUtils.newCloudWatchClient();
        ListMetricsResponse resp = client.listMetrics( ListMetricsRequest.builder().namespace("http://camel.apache.org/aws-cw").build());

        Assertions.assertEquals(1, resp.metrics().size());
        Assertions.assertEquals("ExchangesCompleted", resp.metrics().get(0).metricName());
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends BaseCw.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start").to("aws2-cw://http://camel.apache.org/aws-cw")
                            .to("mock:result");
                }
            };
        }
    }
}
