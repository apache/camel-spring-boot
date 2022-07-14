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

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(
        classes = {
                HashicorpVaultConfigurationTest.class},
        properties = {
                "camel.vault.hashicorp.token=myToken",
                "camel.vault.hashicorp.engine=myEngine",
                "camel.vault.hashicorp.host=myHost",
                "camel.vault.hashicorp.port=myPort",
                "camel.vault.hashicorp.scheme=myScheme"
        }
)
public class HashicorpVaultConfigurationTest {

    @Autowired
    private CamelContext camelContext;

    @Test
    public void testAwsVault() throws Exception {
        Assertions.assertEquals("myToken", camelContext.getVaultConfiguration().hashicorp().getToken());
        Assertions.assertEquals("myEngine", camelContext.getVaultConfiguration().hashicorp().getEngine());
        Assertions.assertEquals("myHost", camelContext.getVaultConfiguration().hashicorp().getHost());
        Assertions.assertEquals("myPort", camelContext.getVaultConfiguration().hashicorp().getPort());
        Assertions.assertEquals("myScheme", camelContext.getVaultConfiguration().hashicorp().getScheme());
    }
}
