package org.apache.camel.component.aws2.sqs;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.infra.aws.common.services.AWSService;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.infra.aws2.services.AWSServiceFactory;
import org.apache.camel.test.infra.common.SharedNameGenerator;
import org.apache.camel.test.infra.common.TestEntityNameGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.UUID;

public class BaseSqs {

    @Autowired
    protected CamelContext context;

    @Autowired
    protected ProducerTemplate producerTemplate;

    @Autowired
    protected ConsumerTemplate consumerTemplate;

    @RegisterExtension
    public static AWSService service = AWSServiceFactory.createDynamodbService();

    @RegisterExtension
    public static SharedNameGenerator sharedNameGenerator = new TestEntityNameGenerator();

    protected void assertMockEndpointsSatisfied() throws InterruptedException {
        MockEndpoint.assertIsSatisfied(this.context);
    }

    @AfterAll
    private static void closeClient() {
        service.close();
    }

    String sendSingleMessageToQueue(String queueName) {
        final String msg = "sqs" + UUID.randomUUID().toString().replace("-", "");
        return producerTemplate.requestBody("aws2-sqs://" + queueName, msg, String.class);
    }

    String receiveMessageFromQueue(String queueName, boolean deleteMessage) {
        return consumerTemplate.receiveBody(String.format("aws2-sqs://%s?deleteAfterRead=%s&deleteIfFiltered=%s&defaultVisibilityTimeout=0", queueName, deleteMessage, deleteMessage),
                10000,
                String.class);
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public SqsClient sqsClient(CamelContext context) {
            return AWSSDKClientUtils.newSQSClient();
        }
    }
}
