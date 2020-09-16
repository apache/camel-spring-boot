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

import org.apache.camel.builder.RouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.apache.camel.component.jasypt.springboot.Constants.*;

@Configuration
public class Routes {

    @Bean
    public RouteBuilder encryptedPropertiesTestRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(START_URI_TEST_ENCRYPTED_PROPS_IN_CC)
                        .routeId("encrypted-properties-route-test-inside-camel-context")
                        .log("test properties decryption inside camel context ...")
                        .setBody(simple("{{encrypted.password}}"))
                        .to(MOCK_URI);

                from(START_URI_TEST_ENCRYPTED_PROPS_OUT_CC)
                        .routeId("encrypted-properties-route-test-outside-camel-context")
                        .log("test properties decryption  outside camel context ...")
                        .to("bean:encryptedPropertiesBean?method=testEncryptedProperty")
                        .to(MOCK_URI);

                from(START_URI_TEST_UNENCRYPTED_PROPS_IN_CC)
                        .routeId("unencrypted-properties-route-test-inside-camel-context")
                        .log("test unencrypted properties inside camel context ...")
                        .setBody(simple("{{unencrypted.property}}"))
                        .to(MOCK_URI);

                from(START_URI_TEST_UNENCRYPTED_PROPS_OUT_CC)
                        .routeId("unecrypted-properties-route-test-inside-camel-context")
                        .log("test unencrypted properties outside camel context ...")
                        .to("bean:encryptedPropertiesBean?method=testUnencryptedProperty")
                        .to(MOCK_URI);
            }
        };
    }
}
