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
package org.apache.camel.spring.boot.trace;

import org.apache.camel.LoggingLevel;
import org.apache.camel.spi.Metadata;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.trace")
public class CamelTraceConfigurationProperties {

    /**
     * Enables tracer in your Camel application.
     */
    private boolean enabled;

    /**
     * To set the tracer in standby mode, where the tracer will be installed by not automatic enabled. The tracer can
     * then later be enabled explicit from Java, JMX or tooling.
     */
    private boolean standby;

    /**
     * Defines how many of the last messages to keep in the tracer.
     */
    @Metadata(label = "advanced", defaultValue = "1000")
    private int backlogSize = 1000;

    /**
     * Whether all traced messages should be removed when the tracer is dumping. By default, the messages are removed,
     * which means that dumping will not contain previous dumped messages.
     */
    @Metadata(label = "advanced", defaultValue = "true")
    private boolean removeOnDump = true;

    /**
     * To limit the message body to a maximum size in the traced message. Use 0 or negative value to use unlimited size.
     */
    @Metadata(label = "advanced", defaultValue = "131072")
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
    @Metadata(defaultValue = "true")
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
     * Whether to trace routes that is created from Rest DSL.
     */
    @Metadata(label = "advanced")
    private boolean traceRests;

    /**
     * Whether to trace routes that is created from route templates or kamelets.
     */
    @Metadata(label = "advanced")
    private boolean traceTemplates;

    /**
     * Filter for tracing by route or node id
     */
    private String tracePattern;

    /**
     * Filter for tracing messages
     */
    private String traceFilter;

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

    public int getBacklogSize() {
        return backlogSize;
    }

    public void setBacklogSize(int backlogSize) {
        this.backlogSize = backlogSize;
    }

    public boolean isRemoveOnDump() {
        return removeOnDump;
    }

    public void setRemoveOnDump(boolean removeOnDump) {
        this.removeOnDump = removeOnDump;
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

    public boolean isTraceRests() {
        return traceRests;
    }

    public void setTraceRests(boolean traceRests) {
        this.traceRests = traceRests;
    }

    public boolean isTraceTemplates() {
        return traceTemplates;
    }

    public void setTraceTemplates(boolean traceTemplates) {
        this.traceTemplates = traceTemplates;
    }

    public String getTracePattern() {
        return tracePattern;
    }

    public void setTracePattern(String tracePattern) {
        this.tracePattern = tracePattern;
    }

    public String getTraceFilter() {
        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {
        this.traceFilter = traceFilter;
    }
}
