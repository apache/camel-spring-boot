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

import java.io.Serializable;

import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * Contains the result of the execution of a test suite.
 * Can be rebuilt at the other classloader as it does not contain reference to internal classes.
 */
public class UnitTestResult implements Serializable {

    private static final long serialVersionUID = -5015959334755321719L;

    private int runCount;

    private int failureCount;

    private long runTime;

    private int ignoreCount;

    private boolean successful;

    public UnitTestResult() {
    }

    public UnitTestResult(TestExecutionSummary jr) {
        this.runCount = (int) jr.getTestsStartedCount();
        this.failureCount = (int) jr.getTestsFailedCount();
        this.runTime = jr.getTimeFinished()-jr.getTimeStarted();
        this.ignoreCount = (int) jr.getTestsSkippedCount();
        this.successful = jr.getTestsFailedCount()==0;
    }

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public long getRunTime() {
        return runTime;
    }

    public void setRunTime(long runTime) {
        this.runTime = runTime;
    }

    public int getIgnoreCount() {
        return ignoreCount;
    }

    public void setIgnoreCount(int ignoreCount) {
        this.ignoreCount = ignoreCount;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
