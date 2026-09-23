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
package org.apache.camel.component.salesforce.springboot;

import org.apache.camel.CamelContext;
import org.apache.camel.component.salesforce.SalesforceComponent;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Verifies that the Spring Boot context loads without a BindException when the
 * salesforce component is on the classpath and Jackson 2.18+ is in use.
 *
 * Jackson 2.18+ changed JacksonFeatureSet so it is no longer traversable by
 * Spring Boot's @ConfigurationProperties binding.  Without @NestedConfigurationProperty
 * on the objectMapper and config fields of SalesforceComponentConfiguration,
 * Spring Boot tries to bind camel.component.salesforce.config.object-mapper.*
 * and fails at startup even when no such properties are explicitly set.
 */
@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(properties = {
        "camel.component.salesforce.refresh-token=myToken",
        "camel.component.salesforce.client-secret=mySecret",
        "camel.component.salesforce.client-id=myClient",
        "camel.component.salesforce.lazy-login=true" })
public class SalesforceObjectMapperBindingTest {

    @Autowired
    private CamelContext context;

    @Test
    public void contextLoadsWithoutObjectMapperBindException() {
        // The mere fact that the context started without a BindException is the
        // assertion.  We also confirm the component is resolvable to rule out
        // any auto-configuration being silently skipped.
        SalesforceComponent sf = context.getComponent("salesforce", SalesforceComponent.class);
        Assertions.assertNotNull(sf, "SalesforceComponent should be present in the context");
    }
}
