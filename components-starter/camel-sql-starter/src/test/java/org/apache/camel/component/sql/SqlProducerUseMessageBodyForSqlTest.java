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

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SqlProducerUseMessageBodyForSqlTest.class,
                SqlProducerUseMessageBodyForSqlTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class SqlProducerUseMessageBodyForSqlTest extends BaseSql {

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @EndpointInject("mock:resultInsert")
    private MockEndpoint resultInsertEndpoint;

    @BeforeEach
    public void resetMock() throws Exception {
        resultEndpoint.reset();
        resultInsertEndpoint.reset();
    }

    @Test
    public void testUseMessageBodyForSqlAndHeaderParams() throws Exception {
        resultEndpoint.reset();
        resultEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader("direct:start", null, "lic", "ASF");

        List<?> received = assertInstanceOf(List.class, resultEndpoint.getReceivedExchanges().get(0).getIn().getBody());
        assertEquals(2, received.size());
        Map<?, ?> row = assertInstanceOf(Map.class, received.get(0));
        assertEquals("Camel", row.get("PROJECT"));

        row = assertInstanceOf(Map.class, received.get(1));
        assertEquals("AMQ", row.get("PROJECT"));
    }


    @Test
    @SuppressWarnings({ "unchecked", "deprecated" })
    public void testUseMessageBodyForSqlAndCamelSqlParametersBatch() throws Exception {

        resultInsertEndpoint.expectedMessageCount(1);

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 200);
        row.put("project", "MyProject1");
        row.put("lic", "OPEN1");
        rows.add(row);
        row = new HashMap<>();
        row.put("id", 201);
        row.put("project", "MyProject2");
        row.put("lic", "OPEN1");
        rows.add(row);
        template.sendBodyAndHeader("direct:insert", null, SqlConstants.SQL_PARAMETERS, rows);

        String origSql = assertInstanceOf(String.class, resultInsertEndpoint.getReceivedExchanges().get(0).getIn().getBody());
        assertEquals("insert into projects(id, project, license) values(:?id,:?project,:?lic)", origSql);

        assertEquals(null, resultInsertEndpoint.getReceivedExchanges().get(0).getOut().getBody());

        // Clear and then use route2 to verify result of above insert select
        context.removeRoute(context.getRoutes().get(0).getId());

        resultEndpoint.reset();
        resultEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader("direct:start", null, "lic", "OPEN1");

        List<?> received = assertInstanceOf(List.class, resultEndpoint.getReceivedExchanges().get(0).getIn().getBody());
        assertEquals(2, received.size());
        row = assertInstanceOf(Map.class, received.get(0));
        assertEquals("MyProject1", row.get("PROJECT"));

        row = assertInstanceOf(Map.class, received.get(1));
        assertEquals("MyProject2", row.get("PROJECT"));
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public DataSource dataSource() {
            return initDb();
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                            .setBody(constant("select * from projects where license = :?lic order by id"))
                            .to("sql://query?useMessageBodyForSql=true")
                            .to("mock:result");


                    from("direct:insert").routeId("baz")
                            .setBody(constant("insert into projects(id, project, license) values(:?id,:?project,:?lic)"))
                            .to("sql://query?useMessageBodyForSql=true&batch=true")
                            .to("mock:resultInsert");
                }
            };
        }
    }
}
