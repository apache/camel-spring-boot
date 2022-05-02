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
package org.apache.camel.component.validator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ContextTestSupport {

	protected MockEndpoint validEndpoint;
	protected MockEndpoint finallyEndpoint;
	protected MockEndpoint invalidEndpoint;

	protected volatile NotifyBuilder oneExchangeDone;

	@Autowired
	protected CamelContext context;

	@Autowired
	protected ProducerTemplate template;

	protected boolean testDirectoryCleaned;

	@AfterEach
	public void tearDown() throws Exception {
		testDirectoryCleaned = false;
	}

	protected Path testFile(String dir) {
		return testDirectory().resolve(dir);
	}

	protected String fileUri() {
		return "file:" + testDirectory();
	}

	protected String fileUri(String query) {
		return "file:" + testDirectory() + (query.startsWith("?") ? "" : "/") + query;
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

	public void deleteTestDirectory() {
		if (!testDirectoryCleaned) {
			deleteDirectory(testDirectory().toFile());
			testDirectoryCleaned = true;
		}
	}

	public static void deleteDirectory(String file) {
		deleteDirectory(new File(file));
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

	protected NotifyBuilder event() {
		return new NotifyBuilder(context);
	}

	public static <T> T assertIsInstanceOf(Class<T> expectedType, Object value) {
		Assertions.assertNotNull(value, "Expected an instance of type: " + expectedType.getName() + " but was null");
		assertTrue(expectedType.isInstance(value), "object should be a " + expectedType.getName() + " but was: " + value
				+ " with type: " + value.getClass().getName());
		return expectedType.cast(value);
	}

}
