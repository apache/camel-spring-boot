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
import org.apache.camel.component.file.remote.mina.MinaSftpEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that verifies endpoint options work correctly with Spring Boot auto-configuration.
 */
@DirtiesContext
@SpringBootTest(classes = { CamelAutoConfiguration.class, MinaSftpEndpointOptionsTest.class })
@EnabledIf("org.apache.camel.component.file.remote.mina.springboot.MinaSftpEmbeddedService#hasRequiredAlgorithms")
public class MinaSftpEndpointOptionsTest extends BaseMinaSftp {

    @BeforeEach
    public void setUp() throws Exception {
        service.getFtpRootDir().toFile().mkdirs();
    }

    @Test
    public void testPreferredAuthenticationsOption() {
        String preferredAuthentications = "password,publickey";
        String uri = "mina-sftp://localhost:" + getPort() + "/" + getRootDir()
                + "?username=admin&password=admin&preferredAuthentications=password,publickey"
                + "&strictHostKeyChecking=no&useUserKnownHostsFile=false";

        template.sendBodyAndHeader(uri, "Hello World", Exchange.FILE_NAME, "hello.txt");

        File file = ftpFile("hello.txt").toFile();
        assertTrue(file.exists(), "File should exist: " + file);
        assertEquals("Hello World", context.getTypeConverter().convertTo(String.class, file));

        // Verify the endpoint option was set correctly
        MinaSftpEndpoint endpoint = context.getEndpoint(uri, MinaSftpEndpoint.class);
        assertEquals(preferredAuthentications, endpoint.getConfiguration().getPreferredAuthentications());
    }

    @Test
    public void testCiphersOption() {
        String uri = "mina-sftp://localhost:" + getPort() + "/" + getRootDir()
                + "?username=admin&password=admin&ciphers=aes256-ctr"
                + "&strictHostKeyChecking=no&useUserKnownHostsFile=false";

        template.sendBodyAndHeader(uri, "Test with cipher", Exchange.FILE_NAME, "cipher.txt");

        File file = ftpFile("cipher.txt").toFile();
        assertTrue(file.exists(), "File should exist: " + file);

        // Verify the endpoint option was set correctly (ciphers is returned with brackets as a list)
        MinaSftpEndpoint endpoint = context.getEndpoint(uri, MinaSftpEndpoint.class);
        assertTrue(endpoint.getConfiguration().getCiphers().contains("aes256-ctr"),
                "Ciphers should contain aes256-ctr");
    }

    @Test
    public void testConnectTimeoutOption() {
        String uri = "mina-sftp://localhost:" + getPort() + "/" + getRootDir()
                + "?username=admin&password=admin&connectTimeout=30000"
                + "&strictHostKeyChecking=no&useUserKnownHostsFile=false";

        template.sendBodyAndHeader(uri, "Test with timeout", Exchange.FILE_NAME, "timeout.txt");

        File file = ftpFile("timeout.txt").toFile();
        assertTrue(file.exists(), "File should exist: " + file);

        // Verify the endpoint option was set correctly
        MinaSftpEndpoint endpoint = context.getEndpoint(uri, MinaSftpEndpoint.class);
        assertEquals(30000, endpoint.getConfiguration().getConnectTimeout());
    }

    @Test
    public void testStepwiseOption() {
        String uri = "mina-sftp://localhost:" + getPort() + "/" + getRootDir()
                + "?username=admin&password=admin&stepwise=false"
                + "&strictHostKeyChecking=no&useUserKnownHostsFile=false";

        template.sendBodyAndHeader(uri, "Test with stepwise=false", Exchange.FILE_NAME, "stepwise.txt");

        File file = ftpFile("stepwise.txt").toFile();
        assertTrue(file.exists(), "File should exist: " + file);

        // Verify the endpoint option was set correctly
        MinaSftpEndpoint endpoint = context.getEndpoint(uri, MinaSftpEndpoint.class);
        assertEquals(false, endpoint.getConfiguration().isStepwise());
    }
}
