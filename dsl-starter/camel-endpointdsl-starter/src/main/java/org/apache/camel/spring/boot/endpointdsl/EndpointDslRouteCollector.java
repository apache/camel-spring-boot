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
package org.apache.camel.spring.boot.endpointdsl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.LambdaEndpointRouteBuilder;
import org.apache.camel.main.DefaultRoutesCollector;

/**
 * Enhanced {@link org.apache.camel.main.RoutesCollector} that supports Endpoint DSL with the
 * lambda style {@link LambdaEndpointRouteBuilder}.
 */
public class EndpointDslRouteCollector extends DefaultRoutesCollector {

    @Override
    protected Collection<RoutesBuilder> collectAdditionalRoutesFromRegistry(CamelContext camelContext, String excludePattern, String includePattern) {
        final List<RoutesBuilder> routes = new ArrayList<>();

        Collection<LambdaEndpointRouteBuilder> lrbs = findByType(camelContext, LambdaEndpointRouteBuilder.class);
        for (LambdaEndpointRouteBuilder lrb : lrbs) {
            EndpointRouteBuilder rb = new EndpointRouteBuilder() {
                @Override
                public void configure() throws Exception {
                    lrb.accept(this);
                }
            };
            routes.add(rb);
        }

        return routes;
    }

}
