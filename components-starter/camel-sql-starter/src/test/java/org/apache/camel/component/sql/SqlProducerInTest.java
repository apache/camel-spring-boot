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

import org.apache.camel.Configuration;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.annotation.DirtiesContext;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SqlProducerInTest.class,
                SqlProducerInTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class SqlProducerInTest extends BaseSql {

    @EndpointInject("mock:query")
    private MockEndpoint queryEndpoint;

    @Test
    public void testQueryInString() throws InterruptedException {
        queryEndpoint.expectedMessageCount(1);

        template.requestBodyAndHeader("direct:query", "Hi there!", "names", "Camel,AMQ");

        assertMockEndpointsSatisfied();

        List list = queryEndpoint.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertEquals(2, list.size());
        Map row = (Map) list.get(0);
        assertEquals("Camel", row.get("PROJECT"));
        row = (Map) list.get(1);
        assertEquals("AMQ", row.get("PROJECT"));
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public DataSource dataSource() {
            return initDb(EmbeddedDatabaseType.HSQL, "sql/createAndPopulateDatabase.sql");
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:query")
                            .to("sql:classpath:sql/selectProjectsIn.sql")
                            .to("log:query")
                            .to("mock:query");
                }
            };
        }
    }
}
