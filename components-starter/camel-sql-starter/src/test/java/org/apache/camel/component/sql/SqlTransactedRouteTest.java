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
package org.apache.camel.component.sql;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Registry;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.camel.support.SimpleRegistry;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SqlTransactedRouteTest.class,
                SqlTransactedRouteTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class SqlTransactedRouteTest extends BaseSql {

    private JdbcTemplate jdbc;

    private static String startEndpoint = "direct:start";
    private static String sqlEndpoint = "sql:overriddenByTheHeader?dataSource=#testdb";

    @Autowired
    private DataSource ds;

    @Autowired
    private PlatformTransactionManager txMgr;

    @BeforeEach
    public void setUp() throws Exception {
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE CUSTOMER (ID VARCHAR(15) NOT NULL PRIMARY KEY, NAME VARCHAR(100))");
    }

    @Test
    public void testCommit() throws Exception {
        Exchange exchange = template.send(startEndpoint, e ->
            e.getIn().setHeader(SqlConstants.SQL_QUERY, "insert into customer values('cust1','cmueller')")

        );

        assertFalse(exchange.isFailed());

        long count = jdbc.queryForObject("select count(*) from customer", Long.class);
        assertEquals(2, count);

        Map<String, Object> map = jdbc.queryForMap("select * from customer where id = 'cust1'");
        assertEquals(2, map.size());
        assertEquals("cust1", map.get("ID"));
        assertEquals("cmueller", map.get("NAME"));

        map = jdbc.queryForMap("select * from customer where id = 'cust2'");
        assertEquals(2, map.size());
        assertEquals("cust2", map.get("ID"));
        assertEquals("muellerc", map.get("NAME"));
    }

    @Test
    public void testRollbackAfterAnException() throws Exception {
        Exchange exchange = template.send("direct:start2", new Processor() {
            @Override
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(SqlConstants.SQL_QUERY, "insert into customer values('cust1','cmueller')");
            }
        });

        assertTrue(exchange.isFailed());

        long count = jdbc.queryForObject("select count(*) from customer", Long.class);
        assertEquals(0, count);
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean("testdb")
        public DataSource dataSource() {
            return initEmptyDb();
        }

        @Bean(name = "required")
        public SpringTransactionPolicy transactionManager(PlatformTransactionManager platformTransactionManager) {
            SpringTransactionPolicy txPolicy = new SpringTransactionPolicy();
            txPolicy.setTransactionManager(platformTransactionManager);
            txPolicy.setPropagationBehaviorName("PROPAGATION_REQUIRED");
            return txPolicy;
        }

        @Bean(name = "txManager")
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start").routeId("commit")
                            .transacted("required")
                            .to(sqlEndpoint)
                            .process(e -> e.getIn().setHeader(SqlConstants.SQL_QUERY,
                                            "insert into customer values('cust2','muellerc')")
                            )
                            .to(sqlEndpoint);

                    from("direct:start2").routeId("rollback2")
                            .transacted("required")
                            .to(sqlEndpoint)
                            .process(new Processor() {
                                @Override
                                public void process(Exchange exchange) throws Exception {
                                    throw new Exception("forced Exception");
                                }
                            });
                }
            };
        }
    }
}
