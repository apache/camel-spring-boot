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

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.health")
public class CamelHealthCheckConfigurationProperties {

    /**
     * Global option to enable/disable Camel health check.
     */
    private boolean enabled = true;

    /**
     * Option to enable/disable context health-check.
     */
    private boolean contextEnabled = true;

    /**
     * Option to enable/disable routes health-check.
     */
    private boolean routesEnabled = true;

    /**
     * Option to enable/disable registry health-check.
     */
    private boolean registryEnabled = true;

    /**
     * Extended configuration for routes, registry or custom health checks
     */
    private Map<String, String> parameters;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isContextEnabled() {
        return contextEnabled;
    }

    public void setContextEnabled(boolean contextEnabled) {
        this.contextEnabled = contextEnabled;
    }

    public boolean isRoutesEnabled() {
        return routesEnabled;
    }

    public void setRoutesEnabled(boolean routesEnabled) {
        this.routesEnabled = routesEnabled;
    }

    public boolean isRegistryEnabled() {
        return registryEnabled;
    }

    public void setRegistryEnabled(boolean registryEnabled) {
        this.registryEnabled = registryEnabled;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
