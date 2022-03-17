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
package org.apache.camel.converter.jaxb.springboot;

import static org.apache.camel.test.junit5.TestSupport.deleteDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FromFileBase {
    
    private boolean testDirectoryCleaned;
    
    protected String fileUri() {
        return "file:" + testDirectory();
    }
    
    protected String fileUri(String query) {
        return "file:" + testDirectory() + (query.startsWith("?") ? "" : "/") + query;
    }
    
    protected Path testDirectory() {
        return testDirectory(false);
    }

    protected Path testDirectory(boolean create) {
        Class<?> testClass = getClass();
        return testDirectory(testClass, create);
    }

    
    
    protected static Path testDirectory(Class<?> testClass, boolean create) {
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
    
    public void deleteTestDirectory() {
        if (!testDirectoryCleaned) {
            deleteDirectory(testDirectory());
            testDirectoryCleaned = true;
        }
    }
    

}
