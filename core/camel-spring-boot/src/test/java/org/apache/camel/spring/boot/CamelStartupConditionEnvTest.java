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
package org.apache.camel.spring.boot;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.startup.EnvStartupCondition;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.atomic.AtomicInteger;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(properties = {"camel.startupcondition.enabled=true", "camel.startupcondition.interval=10", "camel.startupcondition.customClassNames=org.apache.camel.spring.boot.CamelStartupConditionEnvTest$MyEnvCondition"})
public class CamelStartupConditionEnvTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private CamelStartupConditionEnvTest() {
    }

    public static CamelStartupConditionEnvTest createCamelStartupConditionEnvTest() {
        return new CamelStartupConditionEnvTest();
    }

    @Configuration
    static class Config {
        @Bean
        RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:foo").to("mock:foo");
                }
            };
        }

    }

    @Autowired
    CamelContext camelContext;

    @Autowired
    ProducerTemplate producerTemplate;

    @Test
    public void testCustomCondition() throws Exception {
        Assertions.assertEquals(3, COUNTER.get());
    }

    public static class MyEnvCondition extends EnvStartupCondition {

        public MyEnvCondition() {
            super("MY_ENV");
        }

        @Override
        protected String lookupEnvironmentVariable(String env) {
            if (COUNTER.incrementAndGet() < 3) {
                return null;
            }
            return "FOO";
        }
    }

}
