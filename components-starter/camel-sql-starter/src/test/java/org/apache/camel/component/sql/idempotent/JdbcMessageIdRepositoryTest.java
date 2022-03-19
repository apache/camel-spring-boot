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

import org.apache.camel.Configuration;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sql.BaseSql;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                JdbcMessageIdRepositoryTest.class,
                JdbcMessageIdRepositoryTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class JdbcMessageIdRepositoryTest extends BaseSql {

    protected static final String SELECT_ALL_STRING = "SELECT messageId FROM CAMEL_MESSAGEPROCESSED WHERE processorName = ?";
    protected static final String CLEAR_STRING = "DELETE FROM CAMEL_MESSAGEPROCESSED WHERE processorName = ?";
    protected static final String PROCESSOR_NAME = "myProcessorName";

    private JdbcTemplate jdbcTemplate;

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
    }

    @Test
    public void testDuplicateMessagesAreFilteredOut() throws Exception {
        resultEndpoint.expectedBodiesReceived("one", "two", "three");
        errorEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeader("direct:start", "one", "messageId", "1");
        template.sendBodyAndHeader("direct:start", "two", "messageId", "2");
        template.sendBodyAndHeader("direct:start", "one", "messageId", "1");
        template.sendBodyAndHeader("direct:start", "two", "messageId", "2");
        template.sendBodyAndHeader("direct:start", "one", "messageId", "1");
        template.sendBodyAndHeader("direct:start", "three", "messageId", "3");

        assertMockEndpointsSatisfied();

        // all 3 messages should be in jdbc repo
        List<String> receivedMessageIds = jdbcTemplate.queryForList(SELECT_ALL_STRING, String.class, PROCESSOR_NAME);

        assertEquals(3, receivedMessageIds.size());
        assertTrue(receivedMessageIds.contains("1"));
        assertTrue(receivedMessageIds.contains("2"));
        assertTrue(receivedMessageIds.contains("3"));
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
        public RouteBuilder routeBuilder(DataSource dataSource) {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    deadLetterChannel("mock:error");

                    JdbcMessageIdRepository repo = new JdbcMessageIdRepository(dataSource, PROCESSOR_NAME);
                    from("direct:start")
                            .idempotentConsumer(header("messageId"), repo)
                            .to("mock:result");
                }
            };
        }
    }
}
