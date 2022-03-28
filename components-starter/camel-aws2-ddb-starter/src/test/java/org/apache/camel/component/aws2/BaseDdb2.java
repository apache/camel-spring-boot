package org.apache.camel.component.aws2;

import org.apache.camel.CamelContext;
import org.apache.camel.Configuration;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.apache.camel.test.infra.aws.common.services.AWSService;
import org.apache.camel.test.infra.aws2.services.AWSServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class BaseDdb2 {

    @Autowired
    protected CamelContext context;

    @Autowired
    protected ProducerTemplate template;

    @RegisterExtension
    public static AWSService service = AWSServiceFactory.createDynamodbService();

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public DynamoDbClient dynamnoDbClient() {
            return AWSSDKClientUtils.newDynamoDBClient();
        }
    }
}
