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

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.impl.DefaultModelReifierFactory;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Customize routes related logic.
 */
public class ApplicationRoutesCustomizer implements CamelContextCustomizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRoutesCustomizer.class);

    private final ApplicationConfiguration config;

    public ApplicationRoutesCustomizer(ApplicationConfiguration config) {
        this.config = config;
    }

    @Override
    public void configure(CamelContext camelContext) {
        camelContext.getCamelContextExtension().getContextPlugin(Model.class)
                .setModelReifierFactory(new ApplicationModelReifierFactory(config));
    }

    public static class ApplicationModelReifierFactory extends DefaultModelReifierFactory {
        private final ApplicationConfiguration config;

        public ApplicationModelReifierFactory(ApplicationConfiguration config) {
            this.config = config;
        }

        @Override
        public Route createRoute(CamelContext camelContext, Object routeDefinition) {

            if (routeDefinition instanceof RouteDefinition) {
                override((RouteDefinition) routeDefinition);
            }

            return super.createRoute(camelContext, routeDefinition);
        }

        public void override(RouteDefinition definition) {
            if (config.getRoutes().getOverrides().isEmpty()) {
                return;
            }

            final String id = definition.getRouteId();
            final FromDefinition from = definition.getInput();

            for (ApplicationConfiguration.RouteOverride override : config.getRoutes().getOverrides()) {
                final String overrideRouteId = override.getId();
                final String overrideRouteFrom = override.getInput().getFrom();

                if (ObjectHelper.isEmpty(overrideRouteId) && ObjectHelper.isEmpty(overrideRouteFrom)) {
                    continue;
                }
                if (ObjectHelper.isNotEmpty(overrideRouteId) && !Objects.equals(overrideRouteId, id)) {
                    continue;
                }
                if (ObjectHelper.isNotEmpty(overrideRouteFrom)
                        && !Objects.equals(from.getEndpointUri(), overrideRouteFrom)) {
                    continue;
                }

                LOGGER.debug("Replace '{}' --> '{}' for route {}", from.getEndpointUri(), override.getInput().getWith(),
                        definition.getRouteId());

                from.setUri(override.getInput().getWith());

                break;
            }
        }
    }
}
