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
package org.apache.camel.spring.boot;

import org.apache.camel.spi.Metadata;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.startupcondition")
public class CamelStartupConditionConfigurationProperties {

    /**
     * To enable using startup conditions
     */
    private boolean enabled;

    /**
     * Interval in millis between checking conditions.
     */
    private int interval = 500;

    /**
     * Total timeout (in millis) for all startup conditions.
     */
    private int timeout = 20000;

    /**
     * What action, to do on timeout.
     *
     * fail = do not startup, and throw an exception causing camel to fail stop = do not startup, and stop camel ignore
     * = log a WARN and continue to startup
     */
    @Metadata(defaultValue = "stop", enums = "fail,stop,ignore")
    private String onTimeout = "stop";

    /**
     * Wait for an environment variable with the given name to exists before continuing
     */
    private String environmentVariableExists;

    /**
     * Wait for a file with the given name to exists before continuing
     */
    private String fileExists;

    /**
     * A list of custom class names (FQN). Multiple classes can be separated by comma.
     */
    private String customClassNames;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getOnTimeout() {
        return onTimeout;
    }

    public void setOnTimeout(String onTimeout) {
        this.onTimeout = onTimeout;
    }

    public String getEnvironmentVariableExists() {
        return environmentVariableExists;
    }

    public void setEnvironmentVariableExists(String environmentVariableExists) {
        this.environmentVariableExists = environmentVariableExists;
    }

    public String getFileExists() {
        return fileExists;
    }

    public void setFileExists(String fileExists) {
        this.fileExists = fileExists;
    }

    public String getCustomClassNames() {
        return customClassNames;
    }

    public void setCustomClassNames(String customClassNames) {
        this.customClassNames = customClassNames;
    }
}
