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

import org.apache.camel.spi.Metadata;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.health")
public class CamelHealthCheckConfigurationProperties {

    /**
     * Whether health check is enabled globally.
     * <p>
     * Is default enabled
     */
    private Boolean enabled;

    /**
     * Whether context health check is enabled.
     * <p>
     * Is default enabled
     */
    private Boolean contextEnabled;

    /**
     * Whether routes health check is enabled.
     * <p>
     * Is default enabled
     */
    private Boolean routesEnabled;

    /**
     * Whether consumers health check is enabled.
     * <p>
     * Is default enabled
     */
    private Boolean consumersEnabled;

    /**
     * Whether registry health check is enabled.
     * <p>
     * Is default enabled
     */
    private Boolean registryEnabled;

    /**
     * Pattern to exclude health checks from being invoked by Camel when checking healths. Multiple patterns can be
     * separated by comma.
     */
    private String excludePattern;

    /**
     * Sets the level of details to exposure as result of invoking health checks. There are the following levels: full,
     * default, oneline
     *
     * The full level will include all details and status from all the invoked health checks.
     *
     * The default level will report UP if everything is okay, and only include detailed information for health checks
     * that was DOWN.
     *
     * The oneline level will only report either UP or DOWN.
     */
    @Metadata(enums = "full,default,oneline", defaultValue = "default")
    private String exposureLevel = "default";

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

    public Boolean getConsumersEnabled() {
        return consumersEnabled;
    }

    public void setConsumersEnabled(Boolean consumersEnabled) {
        this.consumersEnabled = consumersEnabled;
    }

    public Boolean getRegistryEnabled() {
        return registryEnabled;
    }

    public void setRegistryEnabled(Boolean registryEnabled) {
        this.registryEnabled = registryEnabled;
    }

    public String getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
    }

    public String getExposureLevel() {
        return exposureLevel;
    }

    public void setExposureLevel(String exposureLevel) {
        this.exposureLevel = exposureLevel;
    }
}



