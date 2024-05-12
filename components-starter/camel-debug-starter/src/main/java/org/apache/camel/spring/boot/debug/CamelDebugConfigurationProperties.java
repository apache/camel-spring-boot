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
package org.apache.camel.spring.boot.debug;

import org.apache.camel.LoggingLevel;
import org.apache.camel.spi.Metadata;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.debug")
public class CamelDebugConfigurationProperties {

    /**
     * Enables Debugger in your Camel application.
     */
    private boolean enabled;

    /**
     * To set the debugger in standby mode, where the debugger will be installed by not automatic enabled. The debugger
     * can then later be enabled explicit from Java, JMX or tooling.
     */
    private boolean standby;

    /**
     * Whether the debugger should suspend on startup, and wait for a remote debugger to attach. This is what the IDEA
     * and VSCode tooling is using.
     */
    private boolean waitForAttach;

    /**
     * The debugger logging level to use when logging activity.
     */
    @Metadata(defaultValue = "INFO")
    private LoggingLevel loggingLevel = LoggingLevel.INFO;

    /**
     * Allows to pre-configure breakpoints (node ids) to use with debugger on startup. Multiple ids can be separated by
     * comma. Use special value _all_routes_ to add a breakpoint for the first node for every route, in other words this
     * makes it easy to debug from the beginning of every route without knowing the exact node ids.
     */
    private String breakpoints;

    /**
     * In single step mode, then when the exchange is created and completed, then simulate a breakpoint at start and
     * end, that allows to suspend and watch the incoming/complete exchange at the route (you can see message body as
     * response, failed exception etc).
     */
    private boolean singleStepIncludeStartEnd;

    /**
     * To limit the message body to a maximum size in the traced message. Use 0 or negative value to use unlimited size.
     */
    @Metadata(defaultValue = "131072")
    private int bodyMaxChars = 128 * 1024;

    /**
     * Whether to include the message body of stream based messages. If enabled then beware the stream may not be
     * re-readable later. See more about Stream Caching.
     */
    private boolean bodyIncludeStreams;

    /**
     * Whether to include the message body of file based messages. The overhead is that the file content has to be read
     * from the file.
     */
    private boolean bodyIncludeFiles = true;

    /**
     * Whether to include the exchange properties in the traced message
     */
    private boolean includeExchangeProperties = true;

    /**
     * Whether to include the exchange variables in the traced message
     */
    private boolean includeExchangeVariables = true;

    /**
     * Trace messages to include exception if the message failed
     */
    private boolean includeException = true;

    /**
     * Fallback Timeout in seconds (300 seconds as default) when block the message processing in Camel. A timeout used
     * for waiting for a message to arrive at a given breakpoint.
     */
    @Metadata(label = "advanced", defaultValue = "300")
    private long fallbackTimeout = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStandby() {
        return standby;
    }

    public void setStandby(boolean standby) {
        this.standby = standby;
    }

    public boolean isWaitForAttach() {
        return waitForAttach;
    }

    public void setWaitForAttach(boolean waitForAttach) {
        this.waitForAttach = waitForAttach;
    }

    public LoggingLevel getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(LoggingLevel loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public String getBreakpoints() {
        return breakpoints;
    }

    public void setBreakpoints(String breakpoints) {
        this.breakpoints = breakpoints;
    }

    public boolean isSingleStepIncludeStartEnd() {
        return singleStepIncludeStartEnd;
    }

    public void setSingleStepIncludeStartEnd(boolean singleStepIncludeStartEnd) {
        this.singleStepIncludeStartEnd = singleStepIncludeStartEnd;
    }

    public int getBodyMaxChars() {
        return bodyMaxChars;
    }

    public void setBodyMaxChars(int bodyMaxChars) {
        this.bodyMaxChars = bodyMaxChars;
    }

    public boolean isBodyIncludeStreams() {
        return bodyIncludeStreams;
    }

    public void setBodyIncludeStreams(boolean bodyIncludeStreams) {
        this.bodyIncludeStreams = bodyIncludeStreams;
    }

    public boolean isBodyIncludeFiles() {
        return bodyIncludeFiles;
    }

    public void setBodyIncludeFiles(boolean bodyIncludeFiles) {
        this.bodyIncludeFiles = bodyIncludeFiles;
    }

    public boolean isIncludeExchangeProperties() {
        return includeExchangeProperties;
    }

    public void setIncludeExchangeProperties(boolean includeExchangeProperties) {
        this.includeExchangeProperties = includeExchangeProperties;
    }

    public boolean isIncludeExchangeVariables() {
        return includeExchangeVariables;
    }

    public void setIncludeExchangeVariables(boolean includeExchangeVariables) {
        this.includeExchangeVariables = includeExchangeVariables;
    }

    public boolean isIncludeException() {
        return includeException;
    }

    public void setIncludeException(boolean includeException) {
        this.includeException = includeException;
    }

    public long getFallbackTimeout() {
        return fallbackTimeout;
    }

    public void setFallbackTimeout(long fallbackTimeout) {
        this.fallbackTimeout = fallbackTimeout;
    }
}
