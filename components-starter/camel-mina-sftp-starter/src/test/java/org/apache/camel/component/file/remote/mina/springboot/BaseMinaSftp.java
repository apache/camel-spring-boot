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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.language.SimpleExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Base test class for MINA SFTP Spring Boot tests.
 */
public abstract class BaseMinaSftp {

    @RegisterExtension
    static MinaSftpEmbeddedService service = new MinaSftpEmbeddedService();

    @Autowired
    protected CamelContext context;

    @Autowired
    protected ProducerTemplate template;

    protected int getPort() {
        return service.getPort();
    }

    protected String getRootDir() {
        return service.getFtpRootDir().toString();
    }

    protected Path ftpFile(String file) {
        return service.getFtpRootDir().resolve(file);
    }

    protected String getMinaSftpUrl(String user, String password) {
        StringBuilder url = new StringBuilder("mina-sftp://");
        url.append(user == null ? "" : user + "@");
        url.append("localhost:" + service.getPort() + "/");
        url.append(password == null ? "" : "?password=" + password);
        url.append("&strictHostKeyChecking=no&useUserKnownHostsFile=false");
        return url.toString();
    }

    protected void assertMockEndpointsSatisfied() throws InterruptedException {
        MockEndpoint.assertIsSatisfied(this.context);
    }

    protected void sendFile(String url, Object body, String fileName) {
        template.sendBodyAndHeader(url, body, Exchange.FILE_NAME, new SimpleExpression(fileName));
    }

    protected static void assertFileExists(String filename) {
        File file = new File(filename);
        Assertions.assertTrue(file.exists(), "File " + filename + " should exist");
        Assertions.assertTrue(file.isFile(), "File " + filename + " should be a file");
    }

    protected static void assertFileNotExists(Path file) {
        Assertions.assertFalse(Files.exists(file, new LinkOption[0]), "File " + file + " should not exist");
    }

    protected static void assertFileExists(Path file, String content) throws IOException {
        Assertions.assertTrue(Files.exists(file, new LinkOption[0]), "File " + file + " should exist");
        Assertions.assertTrue(Files.isRegularFile(file, new LinkOption[0]), "File " + file + " should be a file");
        Assertions.assertEquals(content, new String(Files.readAllBytes(file)),
                "File " + file + " has unexpected content");
    }

    protected Path testFile(String dir) {
        return this.testDirectory().resolve(dir);
    }

    protected Path testDirectory() {
        return this.testDirectory(false);
    }

    protected Path testDirectory(boolean create) {
        Class<?> testClass = this.getClass();
        return testDirectory(testClass, create);
    }

    public static Path testDirectory(Class<?> testClass, boolean create) {
        Path dir = Paths.get("target", "data", testClass.getSimpleName());
        if (create) {
            try {
                Files.createDirectories(dir);
            } catch (IOException var4) {
                throw new IllegalStateException("Unable to create test directory: " + dir, var4);
            }
        }
        return dir;
    }

    protected String fileUri(String query) {
        Path var10000 = this.testDirectory();
        return "file:" + var10000 + (query.startsWith("?") ? "" : "/") + query;
    }

    @Configuration
    public static class TestConfiguration {
    }
}
