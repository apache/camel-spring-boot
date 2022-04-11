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
package org.apache.camel.component.sql.aggregation;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sql.BaseSql;
import org.apache.camel.processor.aggregate.jdbc.JdbcAggregationRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                JdbcAggregateRecoverDeadLetterChannelTest.class,
                JdbcAggregateRecoverDeadLetterChannelTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class JdbcAggregateRecoverDeadLetterChannelTest extends BaseSql {

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject("mock:aggregated")
    protected MockEndpoint aggregatedEndpoint;

    @EndpointInject("mock:dead")
    protected MockEndpoint deadEndpoint;

    @Test
    public void testJdbcAggregateRecoverDeadLetterChannel() throws Exception {
        // should fail all times
        resultEndpoint.expectedMessageCount(0);
        aggregatedEndpoint.expectedMessageCount(4);
        deadEndpoint.expectedBodiesReceived("ABCDE");
        deadEndpoint.message(0).header(Exchange.REDELIVERED).isEqualTo(Boolean.TRUE);
        deadEndpoint.message(0).header(Exchange.REDELIVERY_COUNTER).isEqualTo(3);

        template.sendBodyAndHeader("direct:start", "A", "id", 123);
        template.sendBodyAndHeader("direct:start", "B", "id", 123);
        template.sendBodyAndHeader("direct:start", "C", "id", 123);
        template.sendBodyAndHeader("direct:start", "D", "id", 123);
        template.sendBodyAndHeader("direct:start", "E", "id", 123);

        assertMockEndpointsSatisfied();
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public DataSource dataSource() {
            return initDb("sql/init.sql");
        }

        @Bean
        public JdbcAggregationRepository jdbcAggregationRepository(DataSource dataSource, PlatformTransactionManager transactionManager) {
            JdbcAggregationRepository repo = new JdbcAggregationRepository(transactionManager, "aggregationRepo1", dataSource);

            // enable recovery
            repo.setUseRecovery(true);
            // exhaust after at most 3 attempts
            repo.setMaximumRedeliveries(3);
            // and move to this dead letter channel
            repo.setDeadLetterUri("mock:dead");
            // check faster
            repo.setRecoveryInterval(500, TimeUnit.MILLISECONDS);

            return repo;
        }

        @Bean
        public RouteBuilder routeBuilder(JdbcAggregationRepository repo) {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    deadLetterChannel("mock:error");

                    from("direct:start")
                            .aggregate(header("id"), new MyAggregationStrategy())
                            .completionSize(5).aggregationRepository(repo)
                            .log("aggregated exchange id ${exchangeId} with ${body}")
                            .to("mock:aggregated")
                            .throwException(new IllegalArgumentException("Damn"))
                            .to("mock:result")
                            .end();
                }
            };
        }
    }
}
