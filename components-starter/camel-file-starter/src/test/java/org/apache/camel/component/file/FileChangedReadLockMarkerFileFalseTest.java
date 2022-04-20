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
package org.apache.camel.component.file;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.io.OutputStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Isolated("Does not play well with parallel unit test execution")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileChangedReadLockMarkerFileFalseTest.class,
                FileChangedReadLockMarkerFileFalseTest.TestConfiguration.class
        }
)
public class FileChangedReadLockMarkerFileFalseTest extends BaseFile {

    private static final Logger LOG = LoggerFactory.getLogger(FileChangedReadLockMarkerFileFalseTest.class);

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        testDirectory("in", true);
    }

    @Test
    public void testChangedReadLock() throws Exception {
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedFileExists(testFile("out/slowfile.dat"));
        resultEndpoint.expectedHeaderReceived(Exchange.FILE_LENGTH, expectedFileLength());

        writeSlowFile();

        assertMockEndpointsSatisfied();

        String content = new String(Files.readAllBytes(testFile("out/slowfile.dat")));
        String[] lines = content.split(LS);
        assertEquals(20, lines.length, "There should be 20 lines in the file");
        for (int i = 0; i < 20; i++) {
            assertEquals("Line " + i, lines[i]);
        }
    }

    private void writeSlowFile() throws Exception {
        LOG.debug("Writing slow file...");
        try (OutputStream fos = Files.newOutputStream(testFile("in/slowfile.dat"))) {
            for (int i = 0; i < 20; i++) {
                fos.write(("Line " + i + LS).getBytes());
                LOG.debug("Writing line " + i);
                Thread.sleep(50);
            }
            fos.flush();
        }
        LOG.debug("Writing slow file DONE...");
    }

    long expectedFileLength() {
        long length = 0;
        for (int i = 0; i < 20; i++) {
            length += ("Line " + i + LS).getBytes().length;
        }
        return length;
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(fileUri("in?initialDelay=0&delay=10&readLock=changed&readLockCheckInterval=100&readLockMarkerFile=false"))
                            .to(fileUri("out"),
                                    "mock:result");
                }
            };
        }
    }
}
