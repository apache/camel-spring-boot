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
package org.apache.camel.component.micrometer.springboot.metrics;

import org.apache.camel.spi.Metadata;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.metrics")
public class CamelMetricsConfiguration {

    /**
     * Whether HTTP uri tags should be enabled or not in captured metrics. If disabled then the uri tag, is likely not
     * able to be resolved and will be marked as UNKNOWN.
     */
    private boolean uriTagEnabled = true;

    /**
     * Whether to use static or dynamic values for HTTP uri tags in captured metrics.
     *
     * When using dynamic tags, then a REST service with base URL: /users/{id} will capture metrics with uri tag with
     * the actual dynamic value such as: /users/123. However, this can lead to many tags as the URI is dynamic, so use
     * this with care.
     */
    private boolean uriTagDynamic;

    /**
     * Set whether to enable the MicrometerRoutePolicyFactory for capturing metrics on route processing times.
     */
    private boolean enableRoutePolicy = true;

    /**
     * Sets the level of information to capture. Possible values are all,route,context.
     *
     * all = both context and routes. route = routes only. context = camel context only.
     */
    private String routePolicyLevel = "all";

    /**
     * Pattern to exclude routes (by id) to capture. Multiple route ids can be separated by comma.
     */
    private String routePolicyExcludePattern;

    /**
     * Controls the name style to use for metrics. Default = uses micrometer naming convention. Legacy = uses the
     * classic naming style (camelCase)
     */
    private String namingStrategy = "default";

    /**
     * Set whether to enable the MicrometerMessageHistoryFactory for capturing metrics on individual route node
     * processing times.
     *
     * Depending on the number of configured route nodes, there is the potential to create a large volume of metrics.
     * Therefore, this option is disabled by default.
     */
    private boolean enableMessageHistory;

    /**
     * Set whether to enable the MicrometerExchangeEventNotifier for capturing metrics on exchange processing times.
     */
    private boolean enableExchangeEventNotifier = true;

    /**
     * Set whether to enable the MicrometerRouteEventNotifier for capturing metrics on the total number of routes and
     * total number of routes running.
     */
    private boolean enableRouteEventNotifier = true;

    /**
     * Set whether to gather performance information about Camel Thread Pools by injecting an
     * InstrumentedThreadPoolFactory.
     */
    private boolean enableInstrumentedThreadPoolFactory;

    public boolean isUriTagEnabled() {
        return uriTagEnabled;
    }

    public void setUriTagEnabled(boolean uriTagEnabled) {
        this.uriTagEnabled = uriTagEnabled;
    }

    public boolean isUriTagDynamic() {
        return uriTagDynamic;
    }

    public void setUriTagDynamic(boolean uriTagDynamic) {
        this.uriTagDynamic = uriTagDynamic;
    }

    public boolean isEnableRoutePolicy() {
        return enableRoutePolicy;
    }

    public void setEnableRoutePolicy(boolean enableRoutePolicy) {
        this.enableRoutePolicy = enableRoutePolicy;
    }

    public String getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(String namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public String getRoutePolicyLevel() {
        return routePolicyLevel;
    }

    public void setRoutePolicyLevel(String routePolicyLevel) {
        this.routePolicyLevel = routePolicyLevel;
    }

    public String getRoutePolicyExcludePattern() {
        return routePolicyExcludePattern;
    }

    public void setRoutePolicyExcludePattern(String routePolicyExcludePattern) {
        this.routePolicyExcludePattern = routePolicyExcludePattern;
    }

    public boolean isEnableMessageHistory() {
        return enableMessageHistory;
    }

    public void setEnableMessageHistory(boolean enableMessageHistory) {
        this.enableMessageHistory = enableMessageHistory;
    }

    public boolean isEnableExchangeEventNotifier() {
        return enableExchangeEventNotifier;
    }

    public void setEnableExchangeEventNotifier(boolean enableExchangeEventNotifier) {
        this.enableExchangeEventNotifier = enableExchangeEventNotifier;
    }

    public boolean isEnableRouteEventNotifier() {
        return enableRouteEventNotifier;
    }

    public void setEnableRouteEventNotifier(boolean enableRouteEventNotifier) {
        this.enableRouteEventNotifier = enableRouteEventNotifier;
    }

    public boolean isEnableInstrumentedThreadPoolFactory() {
        return enableInstrumentedThreadPoolFactory;
    }

    public void setEnableInstrumentedThreadPoolFactory(boolean enableInstrumentedThreadPoolFactory) {
        this.enableInstrumentedThreadPoolFactory = enableInstrumentedThreadPoolFactory;
    }
}
