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
package org.apache.camel.micrometer.observability.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.micrometer.observability")
public class MicrometerObservabilityConfigurationProperties {

    /**
     * Sets exclude pattern(s) that will disable tracing for Camel processors that matches the pattern. Multiple patterns
     * can be separated by comma.
     */
    private String excludePatterns;
    /**
     * Setting this to true will create new telemetry spans for each Camel custom Processors. Use the excludePattern
     * property to filter out Processors.
     */
    private Boolean traceProcessors;
    /**
     * Disable any inner core processors (any core DSL processor provided in the route, for example `bean`, `log`, ...).
     */
    private boolean disableCoreProcessors;

    public String getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(String excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public boolean isTraceProcessors() {
        return traceProcessors;
    }

    public void setTraceProcessors(boolean traceProcessors) {
        this.traceProcessors = traceProcessors;
    }

    public boolean isDisableCoreProcessors() {
        return disableCoreProcessors;
    }

    public void setDisableCoreProcessors(Boolean disableCoreProcessors) {
        this.disableCoreProcessors = disableCoreProcessors;
    }
}
