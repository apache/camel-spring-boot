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
package org.apache.camel.component.jasypt.springboot;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.apache.camel.ExchangePattern.InOut;
import static org.apache.camel.component.jasypt.springboot.Constants.MOCK_URI;
import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_UNENCRYPTED_PROPS_IN_CC;
import static org.apache.camel.component.jasypt.springboot.Constants.START_URI_TEST_UNENCRYPTED_PROPS_OUT_CC;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class EncryptedPropertiesTestBase {


    @EndpointInject(MOCK_URI)
    protected MockEndpoint mock;

    @Produce
    protected ProducerTemplate producer;

    @Autowired
    protected ApplicationContext context;


    @Test
    public void testUnencryptedPropsInsideCamelContext() {
        testEncryption(START_URI_TEST_UNENCRYPTED_PROPS_IN_CC, "unEncrYpteD");
    }

    @Test
    public void testUnencryptedPropsOutsideCamelcontext() {
        testEncryption(START_URI_TEST_UNENCRYPTED_PROPS_OUT_CC, "unEncrYpteD");
    }


    public void testEncryption(String uri, String expected){
        Object o =  producer.sendBody(uri, InOut,"Hi from Camel!");
        assertEquals(expected, mock.assertExchangeReceived(0).getIn().getBody());
        mock.reset();
    }


}
