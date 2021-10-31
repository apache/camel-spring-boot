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
package org.apache.camel.springboot.springdoc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mapping settings for the Camel Open API
 */
@ConfigurationProperties(prefix = "camel.springdoc")
public class SpringdocConfiguration {

    /**
     * Enables Camel Rest DSL to automatic register its OpenAPI (eg swagger doc) in Spring Boot
     * which allows tooling such as SpringDoc to integrate with Camel.
     */
    private Boolean enabled = true;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
