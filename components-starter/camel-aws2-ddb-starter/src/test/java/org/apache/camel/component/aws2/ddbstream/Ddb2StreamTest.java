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
package org.apache.camel.component.aws2.ddbstream;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.BaseDdb2;
import org.apache.camel.component.aws2.ddb.Ddb2Constants;
import org.apache.camel.component.aws2.ddb.Ddb2Operations;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.StreamSpecification;
import software.amazon.awssdk.services.dynamodb.model.StreamViewType;

import static org.awaitility.Awaitility.await;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                Ddb2StreamTest.class,
                Ddb2StreamTest.TestConfiguration.class
        }
)
class Ddb2StreamTest extends BaseDdb2{

    private final static String tableName = "TestTable";

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @BeforeAll
    protected static void setupResources() throws Exception {
        DynamoDbClient ddbClient = AWSSDKClientUtils.newDynamoDBClient();

        CreateTableRequest createTableRequest = createTableRequest(tableName, "key").build();
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

    private static CreateTableRequest.Builder createTableRequest(String tableName, String keyColumn) {
        CreateTableRequest.Builder builder = CreateTableRequest.builder()
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(keyColumn)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(keyColumn)
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(10L)
                        .build())
                .streamSpecification(StreamSpecification.builder()
                    .streamEnabled(true)
                    .streamViewType(StreamViewType.NEW_AND_OLD_IMAGES)
                    .build());

        return builder.tableName(tableName);
    }

    @Test
    public void stream() throws InterruptedException {
        final String key1 = "key-" + UUID.randomUUID().toString().replace("-", "");
        final String msg1 = "val" + UUID.randomUUID().toString().replace("-", "");
        
        //try periodically receive stream event. We do not know, when the consumer is started, therefore we try it several times
        //if one event is returned, stream consumer works
        await().pollInterval(2, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).until(() -> {
            boolean res = !resultEndpoint.getReceivedExchanges().isEmpty();
            if(!res) {
                resultEndpoint.reset();
                //insert new item for the test
                insertItem(key1, msg1);
            }
            return res;
        });
    }

    private void insertItem(String key1, String msg1) {
        final Map<String, AttributeValue> item = new HashMap<>() {
            {
                put("key", AttributeValue.builder()
                        .s(key1).build());
                put("value", AttributeValue.builder()
                        .s(msg1).build());
            }
        };

        template.sendBodyAndHeaders(
                "aws2-ddb://" + tableName + "?operation=" + Ddb2Operations.PutItem,
                null,
                new HashMap<>() {
                    {
                        put(
                                Ddb2Constants.CONSISTENT_READ,
                                true);
                        put(
                                Ddb2Constants.ITEM,
                                item);
                    }
                });
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration  extends BaseDdb2.TestConfiguration{

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {

                @Override
                public void configure() {
                    //{aws.secret.key=secretkey, aws.region=us-east-1, aws.access.key=accesskey, aws.host=localhost:49242, aws.protocol=http}
                    String auth = service.getConnectionProperties().entrySet().stream()
                            .map(e1 -> {
                                switch (String.valueOf(e1.getKey())) {
                                    case "aws.secret.key":
                                        return "secretKey=" + e1.getValue();
                                    case "aws.region":
                                        return "region=" + e1.getValue();
                                    case "aws.access.key":
                                        return "accessKey=" + e1.getValue();
                                    case "aws.host":
                                        return "overrideEndpoint=true&uriEndpointOverride=http://" + e1.getValue();
                                    default: return "";
                                }})
                            .filter(e -> !"".equals(e))
                            .collect(Collectors.joining("&"));

                    from("aws2-ddbstream://" + tableName + "?" + auth).to("mock:result");
                }
            };
        }
    }
}
