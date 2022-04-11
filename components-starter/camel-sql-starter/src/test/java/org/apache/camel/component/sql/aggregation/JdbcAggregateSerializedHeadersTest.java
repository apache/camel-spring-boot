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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sql.BaseSql;
import org.apache.camel.processor.aggregate.jdbc.JdbcAggregationRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.lob.AbstractLobHandler;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                JdbcAggregateSerializedHeadersTest.class,
                JdbcAggregateSerializedHeadersTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class JdbcAggregateSerializedHeadersTest extends BaseSql {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcAggregateSerializedHeadersTest.class);
    private static final int SIZE = 500;

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Test
    public void testLoadTestJdbcAggregate() throws Exception {
        resultEndpoint.expectedMinimumMessageCount(1);
        resultEndpoint.setResultWaitTime(50 * 1000);

        LOG.info("Staring to send " + SIZE + " messages.");

        for (int i = 0; i < SIZE; i++) {
            final int value = 1;
            HeaderDto headerDto = new HeaderDto("org", "company", 1);
            LOG.debug("Sending {} with id {}", value, headerDto);
            template.sendBodyAndHeader("seda:start?size=" + SIZE, value, "id", headerDto);
        }

        LOG.info("Sending all " + SIZE + " message done. Now waiting for aggregation to complete.");

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

            repo.setAllowSerializedHeaders(true);

            return repo;
        }

        @Bean
        public RouteBuilder routeBuilder(JdbcAggregationRepository repo) {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("seda:start?size=" + SIZE)
                            .to("log:input?groupSize=500")
                            .aggregate(header("id"), new MyAggregationStrategy())
                            .aggregationRepository(repo)
                            .completionSize(SIZE)
                            .to("log:output?showHeaders=true")
                            .to("mock:result")
                            .end();
                }
            };
        }
    }
}
