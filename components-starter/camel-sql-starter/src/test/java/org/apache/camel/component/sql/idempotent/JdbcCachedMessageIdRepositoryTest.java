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
package org.apache.camel.component.sql.idempotent;

import org.apache.camel.CamelContext;
import org.apache.camel.Configuration;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sql.BaseSql;
import org.apache.camel.processor.idempotent.jdbc.JdbcCachedMessageIdRepository;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import javax.sql.DataSource;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                JdbcCachedMessageIdRepositoryTest.class,
                JdbcCachedMessageIdRepositoryTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class JdbcCachedMessageIdRepositoryTest extends BaseSql {

    private static final String INSERT_STRING
            = "INSERT INTO CAMEL_MESSAGEPROCESSED (processorName, messageId, createdAt) VALUES (?, ?, ?)";
    private static final String PROCESSOR_NAME = "myProcessorName";

    private JdbcTemplate jdbcTemplate;
    private DataSource dataSource;
    private JdbcCachedMessageIdRepository repository;

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @EndpointInject("mock:error")
    private MockEndpoint errorEndpoint;

    @Autowired
    private DataSource ds;

    @BeforeEach
    public void setUp() throws Exception {
        jdbcTemplate = new JdbcTemplate(ds);
        jdbcTemplate.afterPropertiesSet();
        jdbcTemplate.update(INSERT_STRING, PROCESSOR_NAME, "1", new Timestamp(System.currentTimeMillis()));
        jdbcTemplate.update(INSERT_STRING, PROCESSOR_NAME, "2", new Timestamp(System.currentTimeMillis()));
        repository = context.getRegistry().lookupByNameAndType(PROCESSOR_NAME, JdbcCachedMessageIdRepository.class);
        repository.reload();
    }

    @Test
    public void testCacheHit() throws Exception {
        resultEndpoint.expectedBodiesReceived("three");
        errorEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeader("direct:start", "one", "messageId", "1");
        template.sendBodyAndHeader("direct:start", "two", "messageId", "2");
        template.sendBodyAndHeader("direct:start", "three", "messageId", "3");
        template.sendBodyAndHeader("direct:start", "one", "messageId", "1");
        template.sendBodyAndHeader("direct:start", "two", "messageId", "2");
        template.sendBodyAndHeader("direct:start", "three", "messageId", "3");

        assertMockEndpointsSatisfied();

        assertEquals(5, repository.getHitCount());
        assertEquals(1, repository.getMissCount());
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public DataSource dataSource() {
            return initEmptyDb();
        }

        @Bean
        public RouteBuilder routeBuilder(DataSource dataSource, CamelContext context) {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    deadLetterChannel("mock:error");

                    JdbcMessageIdRepository repo = new JdbcCachedMessageIdRepository(dataSource, PROCESSOR_NAME);
                    context.getRegistry().bind(PROCESSOR_NAME, repo);
                    from("direct:start")
                            .idempotentConsumer(header("messageId"), repo)
                            .to("mock:result");
                }
            };
        }
    }
}
