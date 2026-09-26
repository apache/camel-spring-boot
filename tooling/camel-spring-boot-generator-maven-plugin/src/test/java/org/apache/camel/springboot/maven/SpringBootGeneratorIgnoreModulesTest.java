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
package org.apache.camel.springboot.maven;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractSpringBootGenerator#isIgnore(String)} on generator mojos.
 */
class SpringBootGeneratorIgnoreModulesTest {

    @Test
    @DisplayName("Hand-crafted and core Spring Boot modules are ignored by generator mojos")
    void ignoredModulesAreSkipped() {
        AbstractSpringBootGenerator[] mojos = {
                new SpringBootStarterMojo(),
                new SpringBootAutoConfigurationMojo(),
                new PrepareCatalogSpringBootMojo(),
                new UpdateStarterDocPageMojo()
        };

        for (AbstractSpringBootGenerator mojo : mojos) {
            assertThat(mojo.isIgnore("camel-spring-boot-xml"))
                    .as("%s should ignore camel-spring-boot-xml", mojo.getClass().getSimpleName())
                    .isTrue();
            assertThat(mojo.isIgnore("camel-spring-boot-engine"))
                    .as("%s should ignore camel-spring-boot-engine", mojo.getClass().getSimpleName())
                    .isTrue();
            assertThat(mojo.isIgnore("camel-http"))
                    .as("%s should not ignore camel-http", mojo.getClass().getSimpleName())
                    .isFalse();
        }

        assertThat(new PrepareCatalogSpringBootMojo().isIgnore("camel-typesafe-ai"))
                .as("PrepareCatalogSpringBootMojo should not ignore camel-typesafe-ai")
                .isFalse();
        assertThat(new UpdateStarterDocPageMojo().isIgnore("camel-typesafe-ai"))
                .as("UpdateStarterDocPageMojo should not ignore camel-typesafe-ai")
                .isFalse();
    }

}
