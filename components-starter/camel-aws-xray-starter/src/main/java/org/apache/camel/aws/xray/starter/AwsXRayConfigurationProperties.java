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
package org.apache.camel.aws.xray.starter;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.aws-xray")
public class AwsXRayConfigurationProperties {

    /**
     * Global option to enable/disable AWS XRay integration, default is true.
     */
    private boolean enabled = true;
    /**
     * Sets exclude pattern(s) that will disable tracing for Camel messages that matches the pattern. Multiple patterns
     * can be separated by comma.
     */
    private Set<String> excludePatterns;

    /**
     * Tracing strategy used. Defaults to {@link org.apache.camel.component.aws.xray.TraceAnnotatedTracingStrategy}
     */
    private TracingStrategy tracingStrategy = TracingStrategy.DEFAULT;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(Set<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public TracingStrategy getTracingStrategy() {
        return tracingStrategy;
    }

    public void setTracingStrategy(TracingStrategy tracingStrategy) {
        this.tracingStrategy = tracingStrategy;
    }


    public enum TracingStrategy {
        DEFAULT, NOOP }

}
