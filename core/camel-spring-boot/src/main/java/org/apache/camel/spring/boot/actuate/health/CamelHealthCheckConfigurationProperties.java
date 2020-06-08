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
package org.apache.camel.spring.boot.actuate.health;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.health.HealthCheckConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.health")
public class CamelHealthCheckConfigurationProperties {

    /**
     * Whether health check is enabled globally
     */
    private Boolean enabled;

    /**
     * Whether context health check is enabled
     *
     * Is default enabled
     */
    private Boolean contextEnabled;

    /**
     * Whether routes health check is enabled
     *
     * Is default enabled
     */
    private Boolean routesEnabled;

    /**
     * Whether registry health check is enabled
     *
     * Is default enabled
     */
    private Boolean registryEnabled;

    /**
     * Additional health check properties for fine grained configuration of health checks.
     */
    private Map<String, HealthCheckConfigurationProperties> config = new HashMap<>();

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getContextEnabled() {
        return contextEnabled;
    }

    public void setContextEnabled(Boolean contextEnabled) {
        this.contextEnabled = contextEnabled;
    }

    public Boolean getRoutesEnabled() {
        return routesEnabled;
    }

    public void setRoutesEnabled(Boolean routesEnabled) {
        this.routesEnabled = routesEnabled;
    }

    public Boolean getRegistryEnabled() {
        return registryEnabled;
    }

    public void setRegistryEnabled(Boolean registryEnabled) {
        this.registryEnabled = registryEnabled;
    }

    public Map<String, HealthCheckConfigurationProperties> getConfig() {
        return config;
    }

    public void setConfig(Map<String, HealthCheckConfigurationProperties> config) {
        this.config = config;
    }

    @ConfigurationProperties(prefix = "camel.health.config")
    public static class HealthCheckConfigurationProperties {

        /**
         * The id of the health check such as routes or registry (can use * as wildcard)
         */
        private String parent;

        /**
         * Set if the check associated to this configuration is enabled or not.
         *
         * Is default enabled.
         */
        private Boolean enabled;

        /**
         * Set the check interval in milli seconds.
         */
        private Long interval;

        /**
         * Set the number of failure before reporting the service as un-healthy.
         */
        private Integer failureThreshold;

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Long getInterval() {
            return interval;
        }

        public void setInterval(Long interval) {
            this.interval = interval;
        }

        public Integer getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(Integer failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public HealthCheckConfiguration toHealthCheckConfiguration() {
            HealthCheckConfiguration answer = new HealthCheckConfiguration();
            answer.setParent(parent);
            if (enabled != null) {
                answer.setEnabled(enabled);
            }
            if (interval != null) {
                answer.setInterval(interval);
            }
            if (failureThreshold != null) {
                answer.setFailureThreshold(failureThreshold);
            }
            return answer;
        }
    }
}


