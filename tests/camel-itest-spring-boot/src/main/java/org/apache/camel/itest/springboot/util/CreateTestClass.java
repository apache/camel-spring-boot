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
package org.apache.camel.itest.springboot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates a skeleton integration test class for a Camel Spring Boot starter.
 * <p>
 * Usage via Maven:
 * <pre>
 * mvn -Pgenerate -Dstarter=activemq6 -pl tests/camel-itest-spring-boot
 * </pre>
 * <p>
 * Examples:
 * <ul>
 *   <li>{@code camel-activemq6-starter} -> {@code CamelActivemq6IT.java}</li>
 *   <li>{@code camel-xslt-saxon-starter} -> {@code CamelXsltSaxonIT.java}</li>
 *   <li>{@code camel-aws2-s3-starter} -> {@code CamelAws2S3IT.java}</li>
 * </ul>
 */
public final class CreateTestClass {

    private static final Logger LOG = LoggerFactory.getLogger(CreateTestClass.class);

    private static final String PACKAGE = "org.apache.camel.itest.springboot";

    private static final String TEMPLATE = """
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
            package %s;

            import %s.common.AbstractSpringBootBaseTestSupport;
            import org.junit.jupiter.api.Test;

            public class %s extends AbstractSpringBootBaseTestSupport {

                @Test
                void componentTest() {
                    assertComponent(inferComponentName(getClass()));
                }
            }
            """;

    private CreateTestClass() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            LOG.error("Usage: CreateTestClass <starter-name>");
            LOG.error("  e.g. CreateTestClass camel-activemq6-starter");
            System.exit(1);
        }

        String starter = args[0];
        if ("camel-REQUIRED-starter".equals(starter)) {
            LOG.error("Usage: mvn -Pgenerate -Dstarter=<component> -pl tests/camel-itest-spring-boot");
            LOG.error("  e.g. mvn -Pgenerate -Dstarter=activemq6 -pl tests/camel-itest-spring-boot");
            System.exit(1);
        }
        String className = toClassName(starter);
        String baseDir = System.getProperty("project.basedir", ".");
        Path targetDir = Path.of(baseDir, "src/test/java", PACKAGE.replace('.', '/'));
        Path targetFile = targetDir.resolve(className + ".java");

        if (Files.exists(targetFile)) {
            LOG.error("File already exists: {}", targetFile);
            System.exit(1);
        }

        String content = String.format(TEMPLATE, PACKAGE, PACKAGE, className);

        try {
            Files.createDirectories(targetDir);
            Files.writeString(targetFile, content);
            LOG.info("Created {}", targetFile);
        } catch (IOException e) {
            LOG.error("Failed to write file: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Converts a starter name to a PascalCase test class name.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code camel-activemq6-starter} -> {@code CamelActivemq6IT}</li>
     *   <li>{@code camel-xslt-saxon-starter} -> {@code CamelXsltSaxonIT}</li>
     *   <li>{@code camel-aws2-s3-starter} -> {@code CamelAws2S3IT}</li>
     * </ul>
     */
    static String toClassName(String starter) {
        // Strip -starter suffix if present
        String module = starter.endsWith("-starter")
                ? starter.substring(0, starter.length() - "-starter".length())
                : starter;

        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < module.length(); i++) {
            char c = module.charAt(i);
            if (c == '-') {
                capitalizeNext = true;
            } else {
                sb.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        sb.append("IT");
        return sb.toString();
    }
}
