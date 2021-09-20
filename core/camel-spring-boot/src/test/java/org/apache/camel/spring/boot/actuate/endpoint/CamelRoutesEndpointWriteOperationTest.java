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
package org.apache.camel.spring.boot.actuate.endpoint;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.actuate.endpoint.CamelRoutesEndpoint.TimeInfo;
import org.apache.camel.spring.boot.actuate.endpoint.CamelRoutesEndpoint.WriteAction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

/*
 * Test for the {@link CamelRoutesEndpoint} actuator endpoint.
 */
@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootApplication
@SpringBootTest(
    classes = {CamelAutoConfiguration.class, CamelRoutesEndpointAutoConfiguration.class, ActuatorTestRoute.class},
    properties = {"management.endpoint.camelroutes.read-only = false"})
public class CamelRoutesEndpointWriteOperationTest {

    @Autowired
    CamelRoutesEndpoint endpoint;

    @Autowired
    CamelContext camelContext;

    @Test
    public void testWriteOperation() throws Exception {
        AbstractCamelContext acontext = camelContext.getExtension(AbstractCamelContext.class);
        ServiceStatus status = acontext.getRouteStatus("foo-route");
        Assertions.assertTrue(status.isStarted());
        TimeInfo timeInfo = new TimeInfo();
        timeInfo.setAbortAfterTimeout(true);
        timeInfo.setTimeout(10L);
        endpoint.doWriteAction("foo-route", WriteAction.STOP, timeInfo);
        status = acontext.getRouteStatus("foo-route");
        Assertions.assertTrue(status.isStopped());
    }
}
