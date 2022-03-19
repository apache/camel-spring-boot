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
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import javax.sql.DataSource;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SqlProducerOutputTypeStreamListTest.class,
                SqlProducerOutputTypeStreamListTest.TestConfiguration.class,
                BaseSql.TestConfiguration.class
        }
)
public class SqlProducerOutputTypeStreamListTest extends BaseSql {


    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @Test
    public void testSplit() throws Exception {
        resultEndpoint.expectedMessageCount(3);

        template.sendBody("direct:withSplit", "testmsg");

        assertMockEndpointsSatisfied();
        assertThat(resultBodyAt(resultEndpoint, 0), instanceOf(Map.class));
        assertThat(resultBodyAt(resultEndpoint, 1), instanceOf(Map.class));
        assertThat(resultBodyAt(resultEndpoint, 2), instanceOf(Map.class));
    }

    private Object resultBodyAt(MockEndpoint result, int index) {
        return result.assertExchangeReceived(index).getIn().getBody();
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
                    from("direct:withSplit")
                            .to("sql:select * from projects order by id?outputType=StreamList")
                            .to("log:stream")
                            .split(body()).streaming()
                            .to("log:row")
                            .to("mock:result")
                            .end();
                }
            };
        }
    }
}
