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
package org.apache.camel.itest.springboot.command;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.camel.itest.springboot.Command;
import org.apache.camel.itest.springboot.ITestConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
import static org.junit.platform.engine.discovery.ClassNameFilter.*;

import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A command that executes all unit tests contained in the module.
 */
@Component("unittest")
public class UnitTestCommand extends AbstractTestCommand implements Command {

	Logger logger = LoggerFactory.getLogger(UnitTestCommand.class);

	@Override
	public UnitTestResult executeTest(final ITestConfig config, String component) throws Exception {

		logger.info("Spring-Boot test configuration {}", config);

		logger.info("Scanning the classpath for test classes");
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(selectDirectory(config.getModuleBasePath()), selectPackage(config.getUnitTestBasePackage()))
				.filters(
						includeClassNamePatterns(config.getUnitTestInclusionPattern()),
						excludeClassNamePatterns(config.getUnitTestExclusionPattern()),
						new IsAdmissableFilter(),
						new HasSpringbootAnnotationFilter()
				)
				.build();

		// Add classes to the Request
		Launcher launcher = LauncherFactory.create();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(listener);
		if (!config.getJmxDisabledNames().isEmpty()) {
			launcher.registerTestExecutionListeners(new TestExecutionListener() {
				@Override
				public void executionStarted(TestIdentifier testId) {
					if (testId.isTest()) {
						try {
							disableJmx(config.getJmxDisabledNames());
						} catch (Exception e) {
							logger.error("Exception disabling JMX for test " + testId.getDisplayName(), e);
						}
					}
				}
			});
		}
		TestPlan testPlan = launcher.discover(request);
		long nbTests = testPlan.countTestIdentifiers((t) -> {
			return t.isTest();
		});
		if (nbTests > 0) {
			logger.info("Found {} JUnit5 tests", nbTests);
			launcher.execute(testPlan);

			TestExecutionSummary result = listener.getSummary();
			boolean testSucceeded = result.getTestsFailedCount() == 0;

			logger.info(config.getModuleName() + " unit tests. "
					+ "Success: " + testSucceeded + " - Test Run: " + result.getTestsStartedCount()
					+ " - Failures: " + result.getTestsFailedCount()
					+ " - Skipped Tests: " + result.getTestsSkippedCount());


                StringBuilder failureString = new StringBuilder();
                Throwable failureException = null;
                for (Failure f : result.getFailures()) {
                    failureString.append(f.getTestIdentifier().getDisplayName() + " - "
                        + f.getException().getMessage() + "\n");
                    logger.warn("Failed test description: {}", f.getTestIdentifier());
                    logger.warn("Message: {}", f.getException().getMessage());
                    if (f.getException() != null) {
                        logger.error("Exception thrown from test", f.getException());
                        failureException = f.getException();
                    }
                }

                if (!testSucceeded) {
                    if (failureException != null) {
                        Assertions.fail("Some unit tests failed (" + result.getTestsFailedCount() + "/" + result.getTestsStartedCount() + ") : " + failureString.toString(), failureException);
                    } else {
                        Assertions.fail("Some unit tests failed (" + result.getTestsFailedCount() + "/" + result.getTestsStartedCount() + ") : " + failureString.toString());
                    }
                }

			if (result.getTestsStartedCount() == 0 && config.getUnitTestsExpectedNumber() == null) {
				Assertions.fail("No tests have been found");
			}

			Integer expectedTests = config.getUnitTestsExpectedNumber();
			if (expectedTests != null && expectedTests != result.getTestsStartedCount()) {
				Assertions.fail("Wrong number of tests: expected " + expectedTests + " found " + result.getTestsStartedCount());
			}

			return new UnitTestResult(result);
		} else {
			logger.warn("No JUnit5 tests found for component {}", component);
			return null;
		}
	}

	private void disableJmx(Set<String> disabledJmx) throws Exception {
		logger.info("Disabling JMX names: {}", disabledJmx);
		for (MBeanServer server : getMBeanServers()) {
			for (String jmxName : disabledJmx) {
				logger.info("Disabling JMX query {}", jmxName);

				ObjectName oName = new ObjectName(jmxName);
				Set<ObjectName> names = new HashSet<>(server.queryNames(oName, null));
				for (ObjectName name : names) {
					logger.info("Disabled JMX name {}", name);
					server.unregisterMBean(name);
				}
			}
		}
	}

	private List<MBeanServer> getMBeanServers() {
		List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		if (servers == null) {
			servers = Collections.emptyList();
		}
		return servers;
	}

	private class HasSpringbootAnnotationFilter implements PostDiscoveryFilter {

		@Override
		public FilterResult apply(TestDescriptor testDescriptor) {
			Optional<TestSource> source = testDescriptor.getSource();
			Class<?> testClass = null;
			if (source.isPresent() && source.get() instanceof MethodSource) {
				if (source.get() instanceof MethodSource) {
					testClass = ((MethodSource) source.get()).getJavaClass();
				} else if (source.get() instanceof ClassSource) {
					testClass = ((ClassSource) source.get()).getJavaClass();
				}
			}
			if (testClass != null) {
				logger.debug("Checking class " + testClass.getName());
				for (Annotation annotation : testClass.getAnnotations()) {
					if (annotation.toString().contains("org.apache.camel.test.spring.junit5.CamelSpringBootTest")
							|| annotation.toString().contains("org.springframework.boot.test.context.SpringBootTest")) {
						return FilterResult.excluded("Not admissable");
					}
				}
			}

			return FilterResult.included("");
		}
	}

	private class IsAdmissableFilter implements PostDiscoveryFilter {

		@Override
		public FilterResult apply(TestDescriptor testDescriptor) {
			Optional<TestSource> source = testDescriptor.getSource();
			if (source.isPresent() && source.get() instanceof ClassSource) {
				Class<?> testClass = ((ClassSource) source.get()).getJavaClass();
				logger.debug("Checking class " + testClass.getName());
				if (!isAdmissible(testClass)) {
					return FilterResult.excluded("Not admissable");
				} else {
					return FilterResult.included("Admissable");
				}
			} else {
				return FilterResult.included("");
			}
		}
	}

	private boolean isAdmissible(Class<?> testClass) {

		if (testClass.getPackage().getName().startsWith("org.apache.camel.itest.springboot")) {
			// no tests from the integration test suite
			return false;
		}

		URL location = testClass.getResource("/" + testClass.getName().replace(".", "/") + ".class");
		if (location != null) {
			int firstLevel = location.toString().indexOf("!/");
			int lastLevel = location.toString().lastIndexOf("!/");
			if (firstLevel >= 0 && lastLevel >= 0 && firstLevel != lastLevel) {
				// test class is in a nested jar, skipping
				return false;
			}
		}

		return true;
	}
}
