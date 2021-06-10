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
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        GlobalOptionsTest.TestConfiguration.class
    },
    properties = {
        "camel.springboot.global-options[foo] = 123",
        "camel.springboot.global-options[bar] = true",
        "camel.springboot.globalOptions[baz] = 999",
        "camel.springboot.globalOptions[cheese] = Gauda",
        "camel.springboot.global-options[drink] = Wine",
    }
)
public class GlobalOptionsTest {
    @Autowired
    private CamelContext context;

    @Test
    public void testGlobalOptions() throws Exception {
        assertNotNull(context);

        assertEquals(5, context.getGlobalOptions().size());
        assertEquals("123", context.getGlobalOptions().get("foo"));
        assertEquals("true", context.getGlobalOptions().get("bar"));
        assertEquals("Gauda", context.getGlobalOptions().get("cheese"));
        assertEquals("Wine", context.getGlobalOptions().get("drink"));
        assertEquals("999", context.getGlobalOptions().get("baz"));
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                        .to("mock:result");
                }
            };
        }
    }
}
