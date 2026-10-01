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
package org.apache.camel.springboot.cli.connector;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringBootTest;
import org.apache.camel.test.spring.junit6.DisableJmx;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The connector uses the JDK client when asked to, even with the Spring WebSocket client available.
 */
@DirtiesContext
@CamelSpringBootTest
// the status sent to the tool is built from the JMX management layer
@DisableJmx(false)
@SpringBootTest(classes = {
        CamelAutoConfiguration.class, CliConnectorAutoConfiguration.class,
        CliConnectorWebSocketTestSupport.Routes.class },
                properties = {
                        "camel.cli.transport=websocket",
                        "camel.cli.websocket.snapshot-interval=200",
                 "camel.cli.websocket.client=jdk" })
class CliConnectorWebSocketJdkClientTest extends CliConnectorWebSocketTestSupport {

    @Override
    String expectedClient() {
        return "jdk";
    }
}
