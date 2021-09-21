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
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(properties = {
        "camel.component.salesforce.refresh-token=myToken",
        "camel.component.salesforce.client-secret=mySecret",
        "camel.component.salesforce.client-id=myClient",
        "camel.component.salesforce.lazy-login=true",
        "camel.component.salesforce.http-client-properties.requestBufferSize=12345",
        "camel.component.salesforce.http-client-properties.bar=yes",
})
public class SalesforceComponentTest {

    @Autowired
    private CamelContext context;

    @Test
    public void testSalesforceComponent() {
        SalesforceComponent sf = context.getComponent("salesforce", SalesforceComponent.class);

        Assertions.assertNotNull(sf);
        Assertions.assertNotNull(sf.getHttpClientProperties());
        Assertions.assertEquals("12345", sf.getHttpClientProperties().get("requestBufferSize"));
        Assertions.assertEquals("yes", sf.getHttpClientProperties().get("bar"));
    }
}
