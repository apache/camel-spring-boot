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
package org.apache.camel.spring.boot.routecontroller;

import org.apache.camel.spi.Metadata;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.routecontroller")
public class SupervisingRouteControllerConfiguration {

    /**
     * To enable using supervising route controller which allows Camel to startup and then the controller takes care of
     * starting the routes in a safe manner.
     *
     * This can be used when you want to startup Camel despite a route may otherwise fail fast during startup and cause
     * Camel to fail to startup as well. By delegating the route startup to the supervising route controller then it
     * manages the startup using a background thread. The controller allows to be configured with various settings to
     * attempt to restart failing routes.
     */
    boolean enabled;

    /**
     * The number of threads used by the route controller scheduled thread pool that are used for restarting routes. The
     * pool uses 1 thread by default, but you can increase this to allow the controller to concurrently attempt to
     * restart multiple routes in case more than one route has problems starting.
     */
    @Metadata(label = "advanced", defaultValue = "1")
    int threadPoolSize = 1;

    /**
     * Initial delay in milli seconds before the route controller starts, after CamelContext has been started.
     */
    long initialDelay;

    /**
     * Backoff delay in millis when restarting a route that failed to startup.
     */
    @Metadata(defaultValue = "2000")
    long backOffDelay = 2000;

    /**
     * Backoff maximum delay in millis when restarting a route that failed to startup.
     */
    long backOffMaxDelay;

    /**
     * Backoff maximum elapsed time in millis, after which the backoff should be considered exhausted and no more
     * attempts should be made.
     */
    long backOffMaxElapsedTime;

    /**
     * Backoff maximum number of attempts to restart a route that failed to startup. When this threshold has been
     * exceeded then the controller will give up attempting to restart the route, and the route will remain as stopped.
     */
    long backOffMaxAttempts;

    /**
     * Backoff multiplier to use for exponential backoff. This is used to extend the delay between restart attempts.
     */
    @Metadata(defaultValue = "1.0")
    double backOffMultiplier = 1.0;

    /**
     * Pattern for filtering routes to be excluded as supervised.
     *
     * The pattern is matching on route id, and endpoint uri for the route. Multiple patterns can be separated by comma.
     *
     * For example to exclude all JMS routes, you can say <tt>jms:*</tt>. And to exclude routes with specific route ids
     * <tt>mySpecialRoute,myOtherSpecialRoute</tt>. The pattern supports wildcards and uses the matcher from
     * org.apache.camel.support.PatternHelper#matchPattern.
     */
    String includeRoutes;

    /**
     * Pattern for filtering routes to be included as supervised.
     *
     * The pattern is matching on route id, and endpoint uri for the route. Multiple patterns can be separated by comma.
     *
     * For example to include all kafka routes, you can say <tt>kafka:*</tt>. And to include routes with specific route
     * ids <tt>myRoute,myOtherRoute</tt>. The pattern supports wildcards and uses the matcher from
     * org.apache.camel.support.PatternHelper#matchPattern.
     */
    String excludeRoutes;

    /**
     * Whether to mark the route as unhealthy (down) when all restarting attempts (backoff) have failed and the route is
     * not successfully started and the route manager is giving up.
     *
     * If setting this to false will make health checks ignore this problem and allow to report the Camel application as
     * UP.
     */
    @Metadata(label = "advanced", defaultValue = "true")
    private boolean unhealthyOnExhausted = true;

    /**
     * Whether to mark the route as unhealthy (down) when the route failed to initially start, and is being controlled
     * for restarting (backoff).
     *
     * If setting this to false will make health checks ignore this problem and allow to report the Camel application as
     * UP.
     */
    @Metadata(label = "advanced", defaultValue = "true")
    private boolean unhealthyOnRestarting = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public long getBackOffDelay() {
        return backOffDelay;
    }

    public void setBackOffDelay(long backOffDelay) {
        this.backOffDelay = backOffDelay;
    }

    public long getBackOffMaxDelay() {
        return backOffMaxDelay;
    }

    public void setBackOffMaxDelay(long backOffMaxDelay) {
        this.backOffMaxDelay = backOffMaxDelay;
    }

    public long getBackOffMaxElapsedTime() {
        return backOffMaxElapsedTime;
    }

    public void setBackOffMaxElapsedTime(long backOffMaxElapsedTime) {
        this.backOffMaxElapsedTime = backOffMaxElapsedTime;
    }

    public long getBackOffMaxAttempts() {
        return backOffMaxAttempts;
    }

    public void setBackOffMaxAttempts(long backOffMaxAttempts) {
        this.backOffMaxAttempts = backOffMaxAttempts;
    }

    public double getBackOffMultiplier() {
        return backOffMultiplier;
    }

    public void setBackOffMultiplier(double backOffMultiplier) {
        this.backOffMultiplier = backOffMultiplier;
    }

    public String getIncludeRoutes() {
        return includeRoutes;
    }

    public void setIncludeRoutes(String includeRoutes) {
        this.includeRoutes = includeRoutes;
    }

    public String getExcludeRoutes() {
        return excludeRoutes;
    }

    public void setExcludeRoutes(String excludeRoutes) {
        this.excludeRoutes = excludeRoutes;
    }

    public boolean isUnhealthyOnExhausted() {
        return unhealthyOnExhausted;
    }

    public void setUnhealthyOnExhausted(boolean unhealthyOnExhausted) {
        this.unhealthyOnExhausted = unhealthyOnExhausted;
    }

    public boolean isUnhealthyOnRestarting() {
        return unhealthyOnRestarting;
    }

    public void setUnhealthyOnRestarting(boolean unhealthyOnRestarting) {
        this.unhealthyOnRestarting = unhealthyOnRestarting;
    }
}
