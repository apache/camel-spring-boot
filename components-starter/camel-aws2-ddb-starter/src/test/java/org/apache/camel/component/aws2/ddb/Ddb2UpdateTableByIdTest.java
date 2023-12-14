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
package org.apache.camel.component.aws2.ddb;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.BaseDdb2;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                Ddb2UpdateTableByIdTest.class,
                Ddb2UpdateTableByIdTest.TestConfiguration.class
        }
)
@DisabledIfSystemProperty(named = "ci.env.name", matches = "github.com", disabledReason = "Disabled on GH Action due to Docker limit")
public class Ddb2UpdateTableByIdTest extends BaseDdb2 {

    @EndpointInject("direct:start")
    private ProducerTemplate template;

    private final String attributeName = "clave";
    private final String tableName = "TestTableUpdate";

    @Test
    public void updateTable() {

        Exchange exchange = template.send("direct:start", new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setHeader(Ddb2Constants.OPERATION, Ddb2Operations.UpdateTable);
                exchange.getIn().setHeader(Ddb2Constants.CONSISTENT_READ, true);
                exchange.getIn().setHeader(Ddb2Constants.READ_CAPACITY, 20L);
            }
        });

        assertEquals(Long.valueOf(20), exchange.getIn().getHeader(Ddb2Constants.READ_CAPACITY));
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends BaseDdb2.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start").to(
                            "aws2-ddb://" + tableName + "?keyAttributeName=" + attributeName + "&keyAttributeType=" + KeyType.HASH
                                    + "&keyScalarType=" + ScalarAttributeType.S
                                    + "&readCapacity=1&writeCapacity=1");
                }
            };
        }
    }
}
