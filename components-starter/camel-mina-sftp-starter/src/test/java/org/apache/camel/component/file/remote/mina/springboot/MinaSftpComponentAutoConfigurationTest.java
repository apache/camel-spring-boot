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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that verifies MinaSftpComponent is properly auto-configured in Spring Boot.
 */
@DirtiesContext
@SpringBootTest(classes = { CamelAutoConfiguration.class, MinaSftpComponentAutoConfigurationTest.class })
@EnabledIf("org.apache.camel.component.file.remote.mina.springboot.MinaSftpEmbeddedService#hasRequiredAlgorithms")
public class MinaSftpComponentAutoConfigurationTest extends BaseMinaSftp {

    @Test
    public void testMinaSftpComponentIsAutoConfigured() {
        // Verify the component is registered in the CamelContext
        MinaSftpComponent component = context.getComponent("mina-sftp", MinaSftpComponent.class);
        assertNotNull(component, "MinaSftpComponent should be auto-configured");
    }

    @Test
    public void testMinaSftpComponentType() {
        // Verify the component is of the correct type
        Object component = context.getComponent("mina-sftp");
        assertNotNull(component, "mina-sftp component should exist");
        assertTrue(component instanceof MinaSftpComponent,
                "Component should be instance of MinaSftpComponent");
    }

    @Test
    public void testCamelContextCanResolveMinaSftpComponent() {
        // Verify the component can be resolved from the context (components are lazily registered)
        Object component = context.getComponent("mina-sftp");
        assertNotNull(component, "CamelContext should be able to resolve mina-sftp component");
    }
}
