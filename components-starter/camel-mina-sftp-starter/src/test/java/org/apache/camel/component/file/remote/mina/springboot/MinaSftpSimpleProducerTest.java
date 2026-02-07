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
package org.apache.camel.component.file.remote.mina.springboot;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that verifies basic SFTP producer operations work with Spring Boot auto-configuration.
 */
@DirtiesContext
@SpringBootTest(classes = { CamelAutoConfiguration.class, MinaSftpSimpleProducerTest.class })
@EnabledIf("org.apache.camel.component.file.remote.mina.springboot.MinaSftpEmbeddedService#hasRequiredAlgorithms")
public class MinaSftpSimpleProducerTest extends BaseMinaSftp {

    @BeforeEach
    public void setUp() throws Exception {
        // Ensure the root directory exists
        service.getFtpRootDir().toFile().mkdirs();
    }

    @Test
    public void testSimpleProduceFile() throws Exception {
        String uri = "mina-sftp://localhost:" + getPort() + "/" + getRootDir()
                + "?username=admin&password=admin&strictHostKeyChecking=no&useUserKnownHostsFile=false";

        template.sendBodyAndHeader(uri, "Hello World", Exchange.FILE_NAME, "hello.txt");

        File file = ftpFile("hello.txt").toFile();
        assertTrue(file.exists(), "File should exist: " + file);
        assertEquals("Hello World", context.getTypeConverter().convertTo(String.class, file));
    }

    @Test
    public void testProduceFileWithPath() throws Exception {
        String uri = "mina-sftp://localhost:" + getPort() + "/" + getRootDir() + "/subdir"
                + "?username=admin&password=admin&strictHostKeyChecking=no&useUserKnownHostsFile=false";

        template.sendBodyAndHeader(uri, "Nested File Content", Exchange.FILE_NAME, "nested.txt");

        File file = ftpFile("subdir/nested.txt").toFile();
        assertTrue(file.exists(), "File should exist: " + file);
        assertEquals("Nested File Content", context.getTypeConverter().convertTo(String.class, file));
    }

    @Test
    public void testProduceMultipleFiles() throws Exception {
        String uri = "mina-sftp://localhost:" + getPort() + "/" + getRootDir()
                + "?username=admin&password=admin&strictHostKeyChecking=no&useUserKnownHostsFile=false";

        template.sendBodyAndHeader(uri, "File 1", Exchange.FILE_NAME, "file1.txt");
        template.sendBodyAndHeader(uri, "File 2", Exchange.FILE_NAME, "file2.txt");
        template.sendBodyAndHeader(uri, "File 3", Exchange.FILE_NAME, "file3.txt");

        assertTrue(ftpFile("file1.txt").toFile().exists(), "file1.txt should exist");
        assertTrue(ftpFile("file2.txt").toFile().exists(), "file2.txt should exist");
        assertTrue(ftpFile("file3.txt").toFile().exists(), "file3.txt should exist");
    }
}
