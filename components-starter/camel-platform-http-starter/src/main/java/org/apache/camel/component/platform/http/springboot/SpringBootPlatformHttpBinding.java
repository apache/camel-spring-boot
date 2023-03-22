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
package org.apache.camel.component.platform.http.springboot;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.Message;
import org.apache.camel.component.platform.http.PlatformHttpEndpoint;
import org.apache.camel.http.base.HttpHelper;
import org.apache.camel.http.common.DefaultHttpBinding;

public class SpringBootPlatformHttpBinding extends DefaultHttpBinding {

    protected void populateRequestParameters(HttpServletRequest request, Message message) {
        super.populateRequestParameters(request, message);
        String path = request.getRequestURI();
        // skip leading slash
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path != null) {
            PlatformHttpEndpoint endpoint = (PlatformHttpEndpoint) message.getExchange().getFromEndpoint();
            String consumerPath = endpoint.getPath();
            if (consumerPath != null && consumerPath.startsWith("/")) {
                consumerPath = consumerPath.substring(1);
            }
            if (useRestMatching(consumerPath)) {
                HttpHelper.evalPlaceholders(message.getHeaders(), path, consumerPath);
            }
        }
    }

    private boolean useRestMatching(String path) {
        return path.indexOf('{') > -1;
    }

}
