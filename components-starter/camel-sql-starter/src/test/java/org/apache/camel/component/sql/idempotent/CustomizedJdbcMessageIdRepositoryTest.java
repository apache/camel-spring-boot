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
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                CustomizedJdbcMessageIdRepositoryTest.class,
                CustomizedJdbcMessageIdRepositoryTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class CustomizedJdbcMessageIdRepositoryTest extends BaseSql {

    protected static final String SELECT_ALL_STRING
            = "SELECT messageId FROM CUSTOMIZED_MESSAGE_REPOSITORY WHERE processorName = ?";
    protected static final String PROCESSOR_NAME = "myProcessorName";

    protected JdbcTemplate jdbcTemplate;

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject("mock:error")
    protected MockEndpoint errorEndpoint;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void setUp() throws Exception {
        jdbcTemplate = new JdbcTemplate(dataSource);
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
                    repo.setTableExistsString("SELECT 1 FROM CUSTOMIZED_MESSAGE_REPOSITORY WHERE 1 = 0");
                    repo.setCreateString("CREATE TABLE CUSTOMIZED_MESSAGE_REPOSITORY (processorName VARCHAR(255), messageId VARCHAR(100), createdAt TIMESTAMP)");
                    repo.setQueryString("SELECT COUNT(*) FROM CUSTOMIZED_MESSAGE_REPOSITORY WHERE processorName = ? AND messageId = ?");
                    repo.setInsertString("INSERT INTO CUSTOMIZED_MESSAGE_REPOSITORY (processorName, messageId, createdAt) VALUES (?, ?, ?)");
                    repo.setDeleteString("DELETE FROM CUSTOMIZED_MESSAGE_REPOSITORY WHERE processorName = ? AND messageId = ?");

                    from("direct:start")
                            .idempotentConsumer(header("messageId"), repo)
                            .to("mock:result");
                }
            };
        }
    }
}
