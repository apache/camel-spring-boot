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
package org.apache.camel.language.typesafeai.springboot;

import org.apache.camel.spring.boot.LanguageConfigurationPropertiesCommon;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Evaluate a Noul question with the TypeSafe AI decision API
 */
@ConfigurationProperties(prefix = "camel.language.typesafe-ai")
public class TypeSafeAiLanguageConfiguration
        extends
            LanguageConfigurationPropertiesCommon {

    /**
     * Whether to enable auto configuration of the typesafe-ai language. This is
     * enabled by default.
     */
    private Boolean enabled;
    /**
     * The name of the language to use.
     */
    private String language;

    /**
     * Target endpoint URI for TypeSafe AI processing.
     */
    private String endpoint;
    /**
     * Minimum confidence threshold for the TypeSafe AI language. Must be within [0,1].
     */
    private Double threshold;
    /**
     * Half-width of the inclusive uncertainty band for the TypeSafe AI language.
     */
    private Double uncertainty;
    /**
     * Action for the TypeSafe AI language within the uncertainty band.
     */
    private String uncertaintyPolicy;
    /**
     * Simple expression selecting state for the TypeSafe AI language.
     */
    private String state;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Double getUncertainty() {
        return uncertainty;
    }

    public void setUncertainty(Double uncertainty) {
        this.uncertainty = uncertainty;
    }

    public String getUncertaintyPolicy() {
        return uncertaintyPolicy;
    }

    public void setUncertaintyPolicy(String uncertaintyPolicy) {
        this.uncertaintyPolicy = uncertaintyPolicy;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
