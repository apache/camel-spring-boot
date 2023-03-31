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
package org.apache.camel.spring.boot.actuate.console;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.console.DevConsole;
import org.apache.camel.console.DevConsoleRegistry;
import org.apache.camel.util.json.JsonObject;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.HashMap;
import java.util.Map;

/*
 * Camel Developer Console
 */
@Endpoint(id = "camel")
public class CamelDevConsoleEndpoint {

    private CamelContext camelContext;

    public CamelDevConsoleEndpoint(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @ReadOperation
    public JsonObject getConsoles() {
        DevConsoleRegistry dcr = camelContext.getCamelContextExtension().getContextPlugin(DevConsoleRegistry.class);
        if (dcr == null || !dcr.isEnabled()) {
            return null;
        }

        JsonObject root = new JsonObject();
        dcr.stream().forEach(c -> {
            JsonObject jo = new JsonObject();
            jo.put("id", c.getId());
            jo.put("displayName", c.getDisplayName());
            jo.put("description", c.getDescription());
            root.put(c.getId(), jo);
        });

        return root;
    }

    @ReadOperation
    public JsonObject getConsoleById(@Selector String id) {
        DevConsoleRegistry dcr = camelContext.getCamelContextExtension().getContextPlugin(DevConsoleRegistry.class);
        if (dcr == null || !dcr.isEnabled()) {
            return null;
        }

        Map<String, Object> params = new HashMap<>();
        params.put(Exchange.HTTP_PATH, id);
        JsonObject root = new JsonObject();

        // sort according to index by given id
        dcr.stream().sorted((o1, o2) -> {
            int p1 = id.indexOf(o1.getId());
            int p2 = id.indexOf(o2.getId());
            return Integer.compare(p1, p2);
        }).forEach(c -> {
            boolean include = "all".equals(id) || id.contains(c.getId());
            if (include && c.supportMediaType(DevConsole.MediaType.JSON)) {
                Object out = c.call(DevConsole.MediaType.JSON, params);
                if (out != null) {
                    root.put(c.getId(), out);
                }
            }
        });

        return root;
    }

}
