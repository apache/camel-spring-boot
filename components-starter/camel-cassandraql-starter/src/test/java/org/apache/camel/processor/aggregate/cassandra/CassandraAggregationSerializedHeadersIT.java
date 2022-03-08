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
package org.apache.camel.processor.aggregate.cassandra;


import org.apache.camel.AggregationStrategy;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cassandra.springboot.BaseCassandra;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.aggregate.util.HeaderDto;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CassandraAggregationSerializedHeadersIT.class,
        CassandraAggregationSerializedHeadersIT.TestConfiguration.class
    }
)
public class CassandraAggregationSerializedHeadersIT extends BaseCassandra {

    CassandraAggregationRepository aggregationRepository;

    
    @EndpointInject("mock:output")
    MockEndpoint mockOutput;

    @BeforeEach
    protected void doPreSetup() throws Exception {
        aggregationRepository = new NamedCassandraAggregationRepository(getSession(), "ID");
        aggregationRepository.setTable("NAMED_CAMEL_AGGREGATION");
        aggregationRepository.setAllowSerializedHeaders(true);
        aggregationRepository.start();

    }

    
    @AfterEach
    public void tearDown() throws Exception {
    
        aggregationRepository.stop();
    }

    private void send(HeaderDto aggregationId, String body) {
        template.sendBodyAndHeader("direct:input", body, "aggregationId", aggregationId);
    }

    @Test
    public void testAggregationRoute() throws Exception {
        // Given
        
        mockOutput.expectedMessageCount(2);
        mockOutput.expectedBodiesReceivedInAnyOrder("A,C,E", "B,D");
        HeaderDto dto1 = new HeaderDto("org", "company", 1);
        HeaderDto dto2 = new HeaderDto("org", "company", 2);
        // When
        send(dto1, "A");
        send(dto2, "B");
        send(dto1, "C");
        send(dto2, "D");
        send(dto1, "E");
        // Then
        mockOutput.assertIsSatisfied(4000L);

    }
    

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    AggregationStrategy aggregationStrategy = new AggregationStrategy() {
                        @Override
                        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                            if (oldExchange == null) {
                                return newExchange;
                            }
                            String oldBody = oldExchange.getIn().getBody(String.class);
                            String newBody = newExchange.getIn().getBody(String.class);
                            oldExchange.getIn().setBody(oldBody + "," + newBody);
                            return oldExchange;
                        }
                    };
                    from("direct:input").aggregate(header("aggregationId"), aggregationStrategy).completionSize(3)
                            .completionTimeout(3000L).aggregationRepository(aggregationRepository)
                            .to("mock:output");
                }
            };
        }
    }
}
