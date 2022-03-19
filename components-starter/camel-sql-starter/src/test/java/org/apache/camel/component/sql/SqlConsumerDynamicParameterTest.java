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
import org.apache.camel.Exchange;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SqlConsumerDynamicParameterTest.class,
                SqlConsumerDynamicParameterTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class SqlConsumerDynamicParameterTest extends BaseSql {

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @Test
    public void testDynamicConsumer() throws Exception {
        MyIdGenerator idGenerator = new MyIdGenerator();
        context.getRegistry().bind("myIdGenerator", idGenerator);
        resultEndpoint.expectedMessageCount(3);

        context.getRouteController().startRoute("foo");

        assertMockEndpointsSatisfied();

        List<Exchange> exchanges = resultEndpoint.getReceivedExchanges();

        assertEquals(1, exchanges.get(0).getIn().getBody(Map.class).get("ID"));
        assertEquals("Camel", exchanges.get(0).getIn().getBody(Map.class).get("PROJECT"));
        assertEquals(2, exchanges.get(1).getIn().getBody(Map.class).get("ID"));
        assertEquals("AMQ", exchanges.get(1).getIn().getBody(Map.class).get("PROJECT"));
        assertEquals(3, exchanges.get(2).getIn().getBody(Map.class).get("ID"));
        assertEquals("Linux", exchanges.get(2).getIn().getBody(Map.class).get("PROJECT"));

        // and the bean id should be > 1
        assertTrue(idGenerator.getId() > 1, "Id counter should be > 1");
    }

    public static class MyIdGenerator {

        private int id = 1;

        public int nextId() {
            return id++;
        }

        public int getId() {
            return id;
        }
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
                    from("sql:select * from projects where id = :#${bean:myIdGenerator.nextId}?initialDelay=0&delay=50")
                            .routeId("foo").noAutoStartup()
                            .to("mock:result");
                }
            };
        }
    }
}
