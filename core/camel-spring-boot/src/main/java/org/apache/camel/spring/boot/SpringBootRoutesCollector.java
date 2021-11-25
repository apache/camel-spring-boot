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
package org.apache.camel.spring.boot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.LambdaRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.DefaultRoutesCollector;
import org.apache.camel.util.AntPathMatcher;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StopWatch;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * Spring Boot {@link org.apache.camel.main.RoutesCollector}.
 */
public class SpringBootRoutesCollector extends DefaultRoutesCollector {

    private final ApplicationContext applicationContext;

    public SpringBootRoutesCollector(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<RoutesBuilder> collectRoutesFromRegistry(final CamelContext camelContext, final String excludePattern, final String includePattern) {
        final List<RoutesBuilder> routes = new ArrayList<>();

        Set<LambdaRouteBuilder> lrbs = camelContext.getRegistry().findByType(LambdaRouteBuilder.class);
        for (LambdaRouteBuilder lrb : lrbs) {
            RouteBuilder rb = new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    lrb.accept(this);
                }
            };
            routes.add(rb);
        }

        final AntPathMatcher matcher = new AntPathMatcher();
        for (RoutesBuilder routesBuilder : applicationContext.getBeansOfType(RoutesBuilder.class, true, true).values()) {
            // filter out abstract classes
            boolean abs = Modifier.isAbstract(routesBuilder.getClass().getModifiers());
            if (!abs) {
                String name = routesBuilder.getClass().getName();
                // make name as path so we can use ant path matcher
                name = name.replace('.', '/');

                boolean match = !"false".equals(includePattern);

                // special support for testing with @ExcludeRoutes annotation with camel-test modules
                String exclude = camelContext.adapt(ExtendedCamelContext.class).getTestExcludeRoutes();
                // exclude take precedence over include
                if (match && ObjectHelper.isNotEmpty(exclude)) {
                    // this property is a comma separated list of FQN class names, so we need to make
                    // name as path so we can use ant patch matcher
                    exclude = exclude.replace('.', '/');
                    // there may be multiple separated by comma
                    String[] parts = exclude.split(",");
                    for (String part : parts) {
                        // must negate when excluding, and hence !
                        match = !matcher.match(part, name);
                        log.trace("Java RoutesBuilder: {} exclude filter: {} -> {}", name, part, match);
                        if (!match) {
                            break;
                        }
                    }
                }
                // exclude take precedence over include
                if (match && ObjectHelper.isNotEmpty(excludePattern)) {
                    // there may be multiple separated by comma
                    String[] parts = excludePattern.split(",");
                    for (String part : parts) {
                        // must negate when excluding, and hence !
                        match = !matcher.match(part, name);
                        log.trace("Java RoutesBuilder: {} exclude filter: {} -> {}", name, part, match);
                        if (!match) {
                            break;
                        }
                    }
                }
                if (match && ObjectHelper.isNotEmpty(includePattern)) {
                    // there may be multiple separated by comma
                    String[] parts = includePattern.split(",");
                    for (String part : parts) {
                        match = matcher.match(part, name);
                        log.trace("Java RoutesBuilder: {} include filter: {} -> {}", name, part, match);
                        if (match) {
                            break;
                        }
                    }
                }
                log.debug("Java RoutesBuilder: {} accepted by include/exclude filter: {}", name, match);
                if (match) {
                    routes.add(routesBuilder);
                }
            }
        }

        return routes;
    }

    @Override
    public Collection<RoutesBuilder> collectRoutesFromDirectory(
            CamelContext camelContext,
            String excludePattern,
            String includePattern) {

        final ExtendedCamelContext ecc = camelContext.adapt(ExtendedCamelContext.class);
        final List<RoutesBuilder> answer = new ArrayList<>();
        final String[] includes = includePattern != null ? includePattern.split(",") : null;
        final String[] excludes = excludePattern != null ? excludePattern.split(",") : null;

        if (includes == null) {
            log.debug("Include pattern is empty, no routes will be discovered from resources");
            return answer;
        }

        StopWatch watch = new StopWatch();

        if (ObjectHelper.equal("false", includePattern)) {
            return answer;
        }

        for (String include : includes) {
            log.debug("Loading additional RoutesBuilder from: {}", include);
            try {
                for (Resource resource : applicationContext.getResources(include)) {
                    if (!"false".equals(excludePattern) && AntPathMatcher.INSTANCE.anyMatch(excludes, resource.getFilename())) {
                        continue;
                    }

                    Collection<RoutesBuilder> builders = ecc.getRoutesLoader().findRoutesBuilders(new SpringResource(resource));
                    if (builders.isEmpty()) {
                        continue;
                    }

                    log.debug("Found {} route builder from location: {}", builders.size(), include);
                    answer.addAll(builders);
                }
            } catch (FileNotFoundException e) {
                log.debug("No RoutesBuilder found in {}. Skipping detection.", include, e);
            } catch (Exception e) {
                throw RuntimeCamelException.wrapRuntimeException(e);
            }
            if (!answer.isEmpty()) {
                log.debug("Loaded {} ({} millis) additional RoutesBuilder from: {}, pattern: {}", answer.size(), watch.taken(),
                        include,
                        includePattern);
            } else {
                log.debug("No additional RoutesBuilder discovered from: {}", includePattern);
            }
        }

        return answer;
    }

    static class SpringResource implements org.apache.camel.spi.Resource {
        private final Resource resource;

        public SpringResource(Resource resource) throws IOException {
            this.resource = resource;
        }

        @Override
        public String getLocation() {
            return resource.getFilename();
        }

        @Override
        public boolean exists() {
            return resource.exists();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return resource.getInputStream();
        }

    }
}
