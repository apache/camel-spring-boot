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
package org.apache.camel.spring.boot.k;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(classes = { CamelAutoConfiguration.class, ApplicationRoutesAutoConfiguration.class,
        ApplicationShutdownAutoConfiguration.class, ApplicationRoutesTest.class }, properties = {
                "camel.main.routes-include-pattern=classpath:camel-k/sources/test-route-001.yaml",
                // camel-k
                "camel.k.routes.overrides[0].input.from=direct:r1",
                "camel.k.routes.overrides[0].input.with=direct:r1override", "camel.k.routes.overrides[1].id=r2invalid",
                "camel.k.routes.overrides[1].input.from=direct:r2",
                "camel.k.routes.overrides[1].input.with=direct:r2override", "camel.k.routes.overrides[2].id=r3",
                "camel.k.routes.overrides[2].input.from=direct:r3invalid",
                "camel.k.routes.overrides[2].input.with=direct:r3override", "camel.k.routes.overrides[3].id=r4",
                "camel.k.routes.overrides[3].input.from=direct:r4",
                "camel.k.routes.overrides[3].input.with=direct:r4override",
                "camel.k.routes.overrides[4].input.with=direct:r5invalid", "camel.k.routes.overrides[5].id=r5",
                "camel.k.routes.overrides[5].input.with=direct:r5override" })
public class ApplicationRoutesTest {

    @Autowired
    private CamelContext camelContext;

    @ParameterizedTest
    @CsvSource({ "r1,direct://r1override", "r2,direct://r2", "r3,direct://r3", "r4,direct://r4override",
            "r5,direct://r5override", })
    public void testOverrides(String id, String expected) throws Exception {
        Route route = camelContext.getRoute(id);

        assertThat(route.getRouteId()).isEqualTo(id);
        assertThat(route.getEndpoint().getEndpointUri()).isEqualTo(expected);
    }
}
