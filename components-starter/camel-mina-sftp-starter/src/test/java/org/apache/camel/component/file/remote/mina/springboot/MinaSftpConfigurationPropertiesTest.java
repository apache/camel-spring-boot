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
package org.apache.camel.component.file.remote.mina.springboot;

import org.apache.camel.component.file.remote.mina.MinaSftpComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that verifies Spring Boot configuration properties are properly applied to the component.
 */
@DirtiesContext
@EnableAutoConfiguration
@SpringBootTest(
        classes = { CamelAutoConfiguration.class, MinaSftpComponentAutoConfiguration.class, MinaSftpConfigurationPropertiesTest.class },
        properties = {
                "camel.component.mina-sftp.lazy-start-producer=true",
                "camel.component.mina-sftp.bridge-error-handler=true"
        }
)
@EnabledIf("org.apache.camel.component.file.remote.mina.springboot.MinaSftpEmbeddedService#hasRequiredAlgorithms")
public class MinaSftpConfigurationPropertiesTest extends BaseMinaSftp {

    @Test
    public void testLazyStartProducerProperty() {
        MinaSftpComponent component = context.getComponent("mina-sftp", MinaSftpComponent.class);
        assertNotNull(component, "MinaSftpComponent should be configured");
        assertTrue(component.isLazyStartProducer(), "lazyStartProducer should be true from properties");
    }

    @Test
    public void testBridgeErrorHandlerProperty() {
        MinaSftpComponent component = context.getComponent("mina-sftp", MinaSftpComponent.class);
        assertNotNull(component, "MinaSftpComponent should be configured");
        assertTrue(component.isBridgeErrorHandler(), "bridgeErrorHandler should be true from properties");
    }

    @Test
    public void testAutowiredEnabledDefault() {
        MinaSftpComponent component = context.getComponent("mina-sftp", MinaSftpComponent.class);
        assertNotNull(component, "MinaSftpComponent should be configured");
        // autowiredEnabled defaults to true
        assertTrue(component.isAutowiredEnabled(), "autowiredEnabled should default to true");
    }
}
