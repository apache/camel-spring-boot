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
package org.apache.camel.spring.boot.vault;

import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.Assert;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = { AwsVaultConfigurationTest.class }, properties = { "camel.vault.aws.accessKey=myAccessKey",
        "camel.vault.aws.secretKey=mySecretKey", "camel.vault.aws.region=myRegion",
        "camel.vault.aws.defaultCredentialsProvider=false", "camel.vault.aws.refreshPeriod=60000",
        "camel.vault.aws.refreshEnabled=false", "camel.vault.aws.secrets=supersecret",
        "camel.vault.aws.profile-credentials-provider=true", "camel.vault.aws.profile-name=test", "camel.vault.aws.use-sqs-notification=true", "camel.vault.aws.sqs-queue-url=http://sqs-2", "camel.vault.aws.override-endpoint=true", "camel.vault.aws.uri-endpoint-override=http://localhost:8080" })
public class AwsVaultConfigurationTest {

    @Autowired
    private CamelContext camelContext;

    @Test
    public void testAwsVault() throws Exception {
        Assertions.assertEquals("myAccessKey", camelContext.getVaultConfiguration().aws().getAccessKey());
        Assertions.assertEquals("mySecretKey", camelContext.getVaultConfiguration().aws().getSecretKey());
        Assertions.assertEquals("myRegion", camelContext.getVaultConfiguration().aws().getRegion());
        Assertions.assertEquals(false, camelContext.getVaultConfiguration().aws().isDefaultCredentialsProvider());
        Assertions.assertEquals(false, camelContext.getVaultConfiguration().aws().isRefreshEnabled());
        Assertions.assertEquals(60000, camelContext.getVaultConfiguration().aws().getRefreshPeriod());
        Assertions.assertEquals("supersecret", camelContext.getVaultConfiguration().aws().getSecrets());
        Assertions.assertEquals("test", camelContext.getVaultConfiguration().aws().getProfileName());
        Assertions.assertEquals(true, camelContext.getVaultConfiguration().aws().isProfileCredentialsProvider());
        Assertions.assertEquals(true, camelContext.getVaultConfiguration().aws().isUseSqsNotification());
        Assertions.assertEquals("http://sqs-2", camelContext.getVaultConfiguration().aws().getSqsQueueUrl());
        Assertions.assertTrue(camelContext.getVaultConfiguration().aws().isOverrideEndpoint());
        Assertions.assertEquals("http://localhost:8080", camelContext.getVaultConfiguration().aws().getUriEndpointOverride());
    }
}
