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
package org.apache.camel.spring.boot.util;

import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Verifies that {@link CamelPropertiesHelper#LENIENT_CONFIGURATION_BINDING} restores the pre Camel 4.23 behaviour of
 * ignoring options that cannot be set on the Camel bean being configured.
 */
@CamelSpringBootTest
@DirtiesContext
@SpringBootApplication
@SpringBootTest(
                classes = { CamelPropertiesHelperLenientBindingTest.class },
                properties = {
                        "camel.springboot.lenient-configuration-binding = true",
                        "camel.test.my-config.no-such-option-on-the-target = bar" })
public class CamelPropertiesHelperLenientBindingTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    CamelContext camelContext;

    @Test
    public void testConfiguredOptionThatCannotBeSetIsIgnoredWhenLenient() {
        CamelPropertiesHelperTest.MyClass target = new CamelPropertiesHelperTest.MyClass();

        CamelPropertiesHelperTest.MyDriftedConfiguration config = new CamelPropertiesHelperTest.MyDriftedConfiguration();
        config.setName("Donald Duck");
        config.setNoSuchOptionOnTheTarget("bar");

        CamelPropertiesHelper.copyConfigurationProperties(camelContext, applicationContext,
                CamelPropertiesHelperTest.PREFIX, config, target);

        Assertions.assertEquals("Donald Duck", target.getName());
    }

}
