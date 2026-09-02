/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.httpstarter;

import javax.net.ssl.HostnameVerifier;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stands in for an application class that happens to have a field of a type HttpComponentConverter registers itself
 * for. Deliberately outside the org.apache.camel packages.
 */
@ConfigurationProperties(prefix = "thirdparty.http")
public class ThirdPartyHttpProperties {

    private HostnameVerifier verifier;

    public HostnameVerifier getVerifier() {
        return verifier;
    }

    public void setVerifier(HostnameVerifier verifier) {
        this.verifier = verifier;
    }
}
