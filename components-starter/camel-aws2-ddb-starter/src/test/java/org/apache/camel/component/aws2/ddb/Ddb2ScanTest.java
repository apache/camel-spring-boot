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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.BaseDdb2;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                Ddb2ScanTest.class,
                Ddb2ScanTest.TestConfiguration.class
        }
)
@DisabledIfSystemProperty(named = "ci.env.name", matches = "github.com", disabledReason = "Disabled on GH Action due to Docker limit")
public class Ddb2ScanTest extends BaseDdb2 {

    @EndpointInject("direct:start")
    private ProducerTemplate template;

    private final static String attributeName = "clave";
    private final static String secondaryAttributeName = "secondary_attribute";
    private final static String tableName = "TestTableScan";
    private final String retrieveValue = "retrieve";
    private final String notRetrieveValue = "ignore";

    @BeforeAll
    protected static void setupResources() throws Exception {
        DynamoDbClient ddbClient = AWSSDKClientUtils.newDynamoDBClient();

        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName(attributeName)
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName(secondaryAttributeName)
                                .keyType(KeyType.RANGE)
                                .build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeType(ScalarAttributeType.S)
                        .attributeName(secondaryAttributeName)
                        .build(),
                        AttributeDefinition.builder()
                                .attributeType(ScalarAttributeType.S)
                                .attributeName(attributeName)
                                .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                .build();
        CreateTableResponse res = ddbClient.createTable(createTableRequest);
    }

    @AfterAll
    protected static void cleanupResources() {
        DynamoDbClient ddbClient = AWSSDKClientUtils.newDynamoDBClient();

        DeleteTableRequest deleteTableRequest = DeleteTableRequest.builder()
                .tableName(tableName)
                .build();
        ddbClient.deleteTable(deleteTableRequest);
    }

    @Test
    public void scan() {

        putItem(notRetrieveValue, "0");
        putItem(notRetrieveValue, "4");

        putItem(retrieveValue, "1");
        putItem(retrieveValue, "2");
        putItem(retrieveValue, "3");

        Exchange exchange = template.send("direct:start", e -> {
            e.getIn().setHeader(Ddb2Constants.OPERATION, Ddb2Operations.Scan);
            e.getIn().setHeader(Ddb2Constants.CONSISTENT_READ, true);
            Map<String, Condition> keyConditions = new HashMap<>();
            keyConditions.put(attributeName, Condition.builder().comparisonOperator(
                    ComparisonOperator.EQ.toString())
                    .attributeValueList(AttributeValue.builder().s(retrieveValue).build())
                    .build());
            e.getIn().setHeader(Ddb2Constants.SCAN_FILTER, keyConditions);
        });

        assertNotNull(exchange.getIn().getHeader(Ddb2Constants.ITEMS));
        assertEquals(3, exchange.getIn().getHeader(Ddb2Constants.COUNT));
    }

    private void putItem(String value1, String value2) {
        final Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put(attributeName, AttributeValue.builder().s(value1).build());
        attributeMap.put(secondaryAttributeName, AttributeValue.builder().s(value2).build());

        template.send("direct:start", e -> {
            e.getIn().setHeader(Ddb2Constants.OPERATION, Ddb2Operations.PutItem);
            e.getIn().setHeader(Ddb2Constants.CONSISTENT_READ, "true");
            e.getIn().setHeader(Ddb2Constants.RETURN_VALUES, "ALL_OLD");
            e.getIn().setHeader(Ddb2Constants.ITEM, attributeMap);
            e.getIn().setHeader(Ddb2Constants.ATTRIBUTE_NAMES, attributeMap.keySet());
        });
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
                    from("direct:start").to("aws2-ddb://" + tableName);
                }
            };
        }
    }
}
