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
package org.apache.camel.spring.boot.errorregistry;

import org.apache.camel.spi.Metadata;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.errorregistry")
public class CamelErrorRegistryConfigurationProperties {

    /**
     * Whether the error registry is enabled to capture errors during message routing.
     */
    private boolean enabled;

    /**
     * The maximum number of error entries to keep in the registry. When the limit is exceeded, the oldest entries are
     * evicted.
     */
    @Metadata(defaultValue = "100")
    private int maximumEntries = 100;

    /**
     * The time-to-live in seconds for error entries. Entries older than this are evicted. The default value is 0
     * (disabled).
     */
    @Metadata(defaultValue = "0")
    private int timeToLiveSeconds;

    /**
     * To limit the message body to a maximum size in the captured error data. Use 0 or negative value to use unlimited
     * size.
     */
    @Metadata(label = "advanced", defaultValue = "32768")
    private int bodyMaxChars = 32 * 1024;

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
     * Whether to include the exchange properties in the captured error data.
     */
    @Metadata(defaultValue = "true")
    private boolean includeExchangeProperties = true;

    /**
     * Whether to include the exchange variables in the captured error data.
     */
    @Metadata(defaultValue = "true")
    private boolean includeExchangeVariables = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaximumEntries() {
        return maximumEntries;
    }

    public void setMaximumEntries(int maximumEntries) {
        this.maximumEntries = maximumEntries;
    }

    public int getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds(int timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
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
}
