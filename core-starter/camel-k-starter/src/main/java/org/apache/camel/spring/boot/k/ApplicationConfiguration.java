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
package org.apache.camel.spring.boot.k;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "camel.k")
public class ApplicationConfiguration {
    /**
     * Global option to enable/disable Camel K.
     */
    private boolean enabled;

    @NestedConfigurationProperty
    private final ShutdownProperties shutdown = new ShutdownProperties();

    @NestedConfigurationProperty
    private final RoutesProperties routes = new RoutesProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ShutdownProperties getShutdown() {
        return shutdown;
    }

    public RoutesProperties getRoutes() {
        return routes;
    }

    public static class RoutesProperties {
        /**
         * To define some rules to im-place replace some aspect of the loaded routes.
         */
        @NestedConfigurationProperty
        private final List<RouteOverride> overrides = new ArrayList<>();

        public List<RouteOverride> getOverrides() {
            return overrides;
        }
    }

    public static class ShutdownProperties {

        /**
         * To specify how many messages to process by Camel before automatic terminating the Application or the Camel
         * Context. If there are inflight messages, the shutdown is delayed till all the exchanges have been completed.
         */
        private int maxMessages = 0;

        /**
         * Controls whether the Camel application should terminate the Application or the CamelContext when the
         * maxMessages threshold has been reached. Default is {@link ShutdownStrategy#APPLICATION}
         * <p/>
         * Note that this is meant to be used mainly for test. In real applications Camel K expects the application to
         * be stopped.
         */
        private ShutdownStrategy strategy = ShutdownStrategy.APPLICATION;

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }

        public ShutdownStrategy getStrategy() {
            return strategy;
        }

        public void setStrategy(ShutdownStrategy strategy) {
            this.strategy = strategy;
        }
    }

    public enum ShutdownStrategy {
        APPLICATION, CAMEL
    }

    public static class RouteOverride {
        /**
         * Identifies the route to be amended.
         */
        private String id;

        /**
         * Override for the {@link org.apache.camel.model.FromDefinition} of a
         * {@link org.apache.camel.model.RouteDefinition}.
         */
        private RouteInputOverride input;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public RouteInputOverride getInput() {
            return input;
        }

        public void setInput(RouteInputOverride input) {
            this.input = input;
        }
    }

    public static class RouteInputOverride {
        /**
         * The optional endpoint that should be replaced.
         */
        private String from;

        /**
         * The value that should replace the endpoint.
         */
        private String with;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getWith() {
            return with;
        }

        public void setWith(String with) {
            this.with = with;
        }
    }
}
