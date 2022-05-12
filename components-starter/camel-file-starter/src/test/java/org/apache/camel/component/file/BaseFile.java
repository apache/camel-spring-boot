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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RouteConfigurationsBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.model.ModelCamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseFile {

    @Autowired
    protected CamelContext context;

    @Autowired
    protected ProducerTemplate template;

    NotifyBuilder oneExchangeDone;

    protected void assertMockEndpointsSatisfied() throws InterruptedException {
        MockEndpoint.assertIsSatisfied(this.context);
    }

    @BeforeEach
    public void setUp() {
        oneExchangeDone = new NotifyBuilder(context).whenDone(1).create();
    }

    //-------------------- assertions from testSupport ------------------------------

    /**
     * To be used to check is a file is found in the file system
     */
    public static void  assertFileExists(Path file, String content) throws IOException {
        assertTrue(Files.exists(file), "File " + file + " should exist");
        assertTrue(Files.isRegularFile(file), "File " + file + " should be a file");
        assertEquals(content, new String(Files.readAllBytes(file)), "File " + file + " has unexpected content");
    }

    /**
     * To be used to check is a file is found in the file system
     */
    public static void assertFileExists(Path file) {
        assertTrue(Files.exists(file), "File " + file + " should exist");
        assertTrue(Files.exists(file), "File " + file + " should be a file");
    }

    /**
     * To be used to check is a file is <b>not</b> found in the file system
     */
    public static void assertFileNotExists(Path file) {
        assertFalse(Files.exists(file), "File " + file + " should not exist");
    }

    /**
     * To be used for folder/directory comparison that works across different platforms such as Window, Mac and Linux.
     */
    public static void assertDirectoryEquals(String expected, String actual) {
        assertDirectoryEquals(null, expected, actual);
    }

    /**
     * To be used for folder/directory comparison that works across different platforms such as Window, Mac and Linux.
     */
    public static void assertDirectoryEquals(String message, String expected, String actual) {
        // must use single / as path separators
        String expectedPath = expected.replace('\\', '/');
        String actualPath = actual.replace('\\', '/');

        if (message != null) {
            assertEquals(expectedPath, actualPath, message);
        } else {
            assertEquals(expectedPath, actualPath);
        }
    }


    //--------------------- from TestSupport ------------------------------------

    protected static final String LS = System.lineSeparator();
    protected boolean testDirectoryCleaned;

    public void deleteTestDirectory() {
        if (!testDirectoryCleaned) {
            deleteDirectory(testDirectory().toFile());
            testDirectoryCleaned = true;
        }
    }

    protected Path testDirectory() {
        return testDirectory(false);
    }

    protected Path testDirectory(boolean create) {
        Class<?> testClass = getClass();
        if (create) {
            deleteTestDirectory();
        }
        return testDirectory(testClass, create);
    }

    public static Path testDirectory(Class<?> testClass, boolean create) {
        Path dir = Paths.get("target", "data", testClass.getSimpleName());
        if (create) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create test directory: " + dir, e);
            }
        }
        return dir;
    }

    protected Path testFile(String dir) {
        return testDirectory().resolve(dir);
    }


    protected Path testDirectory(String dir) {
        return testDirectory(dir, false);
    }

    protected Path testDirectory(String dir, boolean create) {
        Path f = testDirectory().resolve(dir);
        if (create) {
            try {
                Files.createDirectories(f);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create test directory: " + dir, e);
            }
        }
        return f;
    }

    /**
     * Recursively delete a directory, useful to zapping test data
     *
     * @param file the directory to be deleted
     */
    public static void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteDirectory(child);
                }
            }
        }

        file.delete();
    }

    protected String fileUri() {
        return "file:" + testDirectory();
    }

    protected String fileUri(String query) {
        return "file:" + testDirectory() + (query.startsWith("?") ? "" : "/") + query;
    }
}
