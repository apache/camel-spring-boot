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
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                ApplicationRoutesAutoConfiguration.class,
                ApplicationShutdownAutoConfiguration.class,
                ApplicationPropertiesTest.class
        }
)
public class ApplicationPropertiesTest {

    @Autowired
    private CamelContext camelContext;
    @Autowired
    private Environment env;

    /**
     * Ensure that properties are available both from Camel's {@link PropertiesComponent} as
     * well as from Spring Boot's {@link Environment}.
     */
    @ParameterizedTest
    @CsvSource(value = {
            "app.property,app.value",
            "my.override,default-value",
            "cm-flat,cm-flat-value",
            "secret-flat,secret-flat-value",
    })
    public void testProperties(String name, String value) throws Exception {
        PropertiesComponent component = camelContext.getPropertiesComponent();

        assertThat(component.resolveProperty("{{" + name + "}}"))
                .isPresent()
                .containsInstanceOf(String.class)
                .contains(value);

        assertThat(env.getProperty(name))
                .isEqualTo(value);
    }
}
