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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.springboot.maven.UpdateStarterDocPageMojo.SBProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateStarterDocPageMojoTest {

    // a '|' not preceded by a backslash starts a new AsciiDoc table cell
    private static final Pattern CELL_SEPARATOR = Pattern.compile("(?<!\\\\)\\|");

    @Test
    @DisplayName("Default values containing '|' are escaped so the options table keeps 4 columns")
    void pipeInDefaultValueIsEscaped() {
        // real default of camel.component.debezium-*.custom-sanitize-pattern, a regex alternation;
        // left unescaped, Asciidoctor splits it into extra cells and the camel-website build fails with
        // "dropping cells from incomplete row detected end of table"
        String regex = "\\.jaas.config$|.*basic.auth.user.info|.*credentials.json$";
        String row = generateRow(new SBProperty("camel.component.debezium-db2.custom-sanitize-pattern",
                "Regular expression identifying configuration keys whose values should be masked.",
                "java.lang.String", regex));

        assertThat(countCells(row)).isEqualTo(4);
        assertThat(row).contains(" | \\.jaas.config$\\|.*basic.auth.user.info\\|.*credentials.json$ | String");
    }

    @Test
    @DisplayName("Default values containing '{' are escaped so they are not resolved as AsciiDoc attributes")
    void braceInDefaultValueIsEscaped() {
        String row = generateRow(new SBProperty("camel.component.jms.error-handler-logging-level",
                "Allows to configure the default errorHandler logging message.",
                "java.lang.String", "Poison JMS message due to ${exception.message}"));

        assertThat(row).contains(" | Poison JMS message due to $\\{exception.message} | String");
    }

    @Test
    @DisplayName("Non-string and missing default values are rendered unchanged")
    void nonStringAndNullDefaultValues() {
        String booleanRow = generateRow(new SBProperty("camel.component.jms.enabled", "Whether to enable.",
                "java.lang.Boolean", Boolean.TRUE));
        String nullRow = generateRow(new SBProperty("camel.component.jms.client-id", "Sets the JMS client ID.",
                "java.lang.String", null));

        assertThat(booleanRow).endsWith(" | true | Boolean");
        assertThat(nullRow).endsWith(" |  | String");
        assertThat(countCells(booleanRow)).isEqualTo(4);
        assertThat(countCells(nullRow)).isEqualTo(4);
    }

    private static String generateRow(SBProperty property) {
        String page = new UpdateStarterDocPageMojo().generatePage("camel-test-starter", "test", "Test", null,
                List.of(), List.of(property), null, null, null, null);
        return page.lines()
                .filter(line -> line.startsWith("| " + property.name() + " "))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No table row for " + property.name() + " in:\n" + page));
    }

    private static int countCells(String row) {
        Matcher m = CELL_SEPARATOR.matcher(row);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }
}
