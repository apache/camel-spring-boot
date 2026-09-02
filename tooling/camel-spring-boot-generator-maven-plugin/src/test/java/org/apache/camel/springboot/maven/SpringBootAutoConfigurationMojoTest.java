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
 * Tests for the code that {@link SpringBootAutoConfigurationMojo} generates into the starters.
 */
class SpringBootAutoConfigurationMojoTest {

    @Test
    @DisplayName("Generated converters report unresolvable values instead of converting them to null")
    void testConvertBodyFailsClosed() {
        String body = SpringBootAutoConfigurationMojo.createConvertBody("camel.component.netty-http");

        assertThat(body).isEqualTo(
                "return BeanReferenceHelper.resolveBeanReference(applicationContext, source, targetType,"
                                     + " \"camel.component.netty-http\");\n");
        // the old generated body silently returned null for anything that did not start with #
        assertThat(body).doesNotContain("return null");
        assertThat(body).doesNotContain("startsWith(\"#\")");
    }

    @Test
    @DisplayName("Generated component customizer does not silently drop options")
    void testComponentBodyBindsStrictly() {
        String body = SpringBootAutoConfigurationMojo.createComponentBody("NettyHttpComponent", "netty-http");

        assertThat(body).contains("CamelPropertiesHelper.copyConfigurationProperties(target.getCamelContext(),"
                                  + " applicationContext,\n                \"camel.component.netty-http\","
                                  + " configuration, target);");
        assertThat(body).doesNotContain("CamelPropertiesHelper.copyProperties(");
        assertThat(body).contains("\"camel.component.netty-http.customizer\"");
    }

    @Test
    @DisplayName("Generated data format customizer does not silently drop options")
    void testDataFormatBodyBindsStrictly() {
        String body = SpringBootAutoConfigurationMojo.createDataFormatBody("JacksonDataFormat", "jackson");

        assertThat(body).contains("CamelPropertiesHelper.copyConfigurationProperties(camelContextProvider.getObject(),"
                                  + " applicationContext,\n                \"camel.dataformat.jackson\","
                                  + " configuration, target);");
        assertThat(body).doesNotContain("CamelPropertiesHelper.copyProperties(");
    }

    @Test
    @DisplayName("Generated language customizer does not silently drop options")
    void testLanguageBodyBindsStrictly() {
        String body = SpringBootAutoConfigurationMojo.createLanguageBody("XPathLanguage", "xpath");

        assertThat(body).contains("CamelPropertiesHelper.copyConfigurationProperties(cca.getCamelContext(),"
                                  + " applicationContext,\n                    \"camel.language.xpath\","
                                  + " configuration, target);");
        assertThat(body).doesNotContain("CamelPropertiesHelper.copyProperties(");
    }

}
