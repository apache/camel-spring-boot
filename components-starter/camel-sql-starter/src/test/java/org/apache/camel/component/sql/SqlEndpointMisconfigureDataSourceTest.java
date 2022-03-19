package org.apache.camel.component.sql;/*
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

import org.apache.camel.FailedToCreateRouteException;
import org.apache.camel.PropertyBindingException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SqlEndpointMisconfigureDataSourceTest.class,
                SqlEndpointMisconfigureDataSourceTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class SqlEndpointMisconfigureDataSourceTest extends BaseSql {


    @Test
    public void testFail() {
        RouteBuilder rb = new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .to("sql:foo?dataSource=myDataSource")
                        .to("mock:result");
            }
        };

        FailedToCreateRouteException e = assertThrows(FailedToCreateRouteException.class, () -> context.addRoutes(rb),
                "Should throw exception");

        PropertyBindingException pbe = (PropertyBindingException) e.getCause().getCause();
        assertEquals("dataSource", pbe.getPropertyName());
        assertEquals("myDataSource", pbe.getValue());
    }

    @Test
    public void testOk() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .to("sql:foo?dataSource=#myDataSource")
                        .to("mock:result");
            }
        });
        assertDoesNotThrow(() -> context.start());
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean(name = "myDataSource")
        public DataSource dataSource() {
            return initDb();
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:query").to("sql:select max(id) from projects?outputType=SelectOne&outputHeader=MaxProjectID")
                            .to("mock:query");
                }
            };
        }
    }

}
