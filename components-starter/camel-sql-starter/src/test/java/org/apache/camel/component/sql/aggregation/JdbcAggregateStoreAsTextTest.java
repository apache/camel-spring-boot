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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                JdbcAggregateStoreAsTextTest.class,
                JdbcAggregateStoreAsTextTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class JdbcAggregateStoreAsTextTest extends BaseSql {
    protected JdbcTemplate jdbcTemplate;

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Autowired
    private DataSource ds;

    @BeforeEach
    public void setUp() throws Exception {
        jdbcTemplate = new JdbcTemplate(ds);
        jdbcTemplate.afterPropertiesSet();
    }

    @Test
    public void testStoreBodyAsTextAndCompanyNameHeaderAndAccountNameHeader() throws Exception {
        resultEndpoint.expectedBodiesReceived("ABCDE");



        Map<String, Object> headers = new HashMap<>();
        headers.put("id", 123);
        headers.put("companyName", "Acme");
        headers.put("accountName", "Alan");

        template.sendBodyAndHeaders("direct:start", "A", headers);
        assertEquals("A", getAggregationRepositoryBody(123));
        assertEquals("Acme", getAggregationRepositoryCompanyName(123));
        assertEquals("Alan", getAggregationRepositoryAccountName(123));

        template.sendBodyAndHeaders("direct:start", "B", headers);
        assertEquals("AB", getAggregationRepositoryBody(123));
        assertEquals("Acme", getAggregationRepositoryCompanyName(123));
        assertEquals("Alan", getAggregationRepositoryAccountName(123));

        template.sendBodyAndHeaders("direct:start", "C", headers);
        assertEquals("ABC", getAggregationRepositoryBody(123));
        assertEquals("Acme", getAggregationRepositoryCompanyName(123));
        assertEquals("Alan", getAggregationRepositoryAccountName(123));

        template.sendBodyAndHeaders("direct:start", "D", headers);
        assertEquals("ABCD", getAggregationRepositoryBody(123));
        assertEquals("Acme", getAggregationRepositoryCompanyName(123));
        assertEquals("Alan", getAggregationRepositoryAccountName(123));

        template.sendBodyAndHeaders("direct:start", "E", headers);

        assertMockEndpointsSatisfied();
    }

    public String getAggregationRepositoryBody(int id) {
        return getAggregationRepositoryColumn(id, "body");
    }

    public String getAggregationRepositoryCompanyName(int id) {
        return getAggregationRepositoryColumn(id, "companyName");
    }

    public String getAggregationRepositoryAccountName(int id) {
        return getAggregationRepositoryColumn(id, "accountName");
    }

    public String getAggregationRepositoryColumn(int id, String columnName) {
        return jdbcTemplate.queryForObject("SELECT " + columnName + " from aggregationRepo3 where id = ?", String.class, id);
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public DataSource dataSource() {
            return initDb("sql/init3.sql");
        }

        @Bean
        public JdbcAggregationRepository jdbcAggregationRepository(DataSource dataSource, PlatformTransactionManager transactionManager) {
            JdbcAggregationRepository repo = new JdbcAggregationRepository(transactionManager, "aggregationRepo3", dataSource);

            repo.setStoreBodyAsText(true);
            repo.setHeadersToStoreAsText(Arrays.asList("companyName", "accountName"));

            return repo;
        }

        @Bean
        public RouteBuilder routeBuilder(JdbcAggregationRepository repo) {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                            .aggregate(header("id"), new MyAggregationStrategy())
                            .aggregationRepository(repo)
                            .completionSize(5)
                            .to("log:output?showHeaders=true")
                            .to("mock:result")
                            .end();
                }
            };
        }
    }
}
