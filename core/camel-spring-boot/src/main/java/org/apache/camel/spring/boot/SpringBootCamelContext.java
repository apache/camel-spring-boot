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

import org.apache.camel.main.MainListener;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * The {@link org.apache.camel.CamelContext} created by Spring Boot.
 */
public class SpringBootCamelContext extends SpringCamelContext {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootCamelContext.class);

    private final StopWatch stopWatch = new StopWatch();
    private final boolean warnOnEarlyShutdown;
    private final CamelSpringBootApplicationController controller;

    public SpringBootCamelContext(ApplicationContext applicationContext, boolean warnOnEarlyShutdown,
                                  CamelSpringBootApplicationController controller) {
        super(applicationContext);
        this.warnOnEarlyShutdown = warnOnEarlyShutdown;
        this.controller = controller;
    }

    @Override
    protected void doStart() throws Exception {
        var listeners = controller.getMain().getMainListeners();
        if (!listeners.isEmpty()) {
            for (MainListener listener : listeners) {
                listener.beforeStart(controller.getMain());
            }
        }

        stopWatch.restart();
        super.doStart();

        if (!listeners.isEmpty()) {
            for (MainListener listener : listeners) {
                listener.afterStart(controller.getMain());
            }
        }
    }

    @Override
    protected synchronized void doStop() throws Exception {
        var listeners = controller.getMain().getMainListeners();
        if (!listeners.isEmpty()) {
            for (MainListener listener : listeners) {
                listener.beforeStop(controller.getMain());
            }
        }

        // if we are stopping very quickly then its likely because the user may not have either spring-boot-web
        // or enabled Camel's main controller, so lets log a WARN about this.
        long taken = stopWatch.taken();
        if (warnOnEarlyShutdown && taken < 1200) { // give it a bit of slack
            String cp = System.getProperty("java.class.path");
            boolean junit = cp != null && cp.contains("junit-");
            boolean starterWeb = cp != null && cp.contains("spring-boot-starter-web");
            if (!junit && !starterWeb) {
                LOG.warn(
                        "CamelContext has only been running for less than a second. If you intend to run Camel for a longer time "
                                + "then you can set the property camel.springboot.main-run-controller=true in application.properties"
                                + " or add spring-boot-starter-web JAR to the classpath.");
            }
        }
        super.doStop();

        if (!listeners.isEmpty()) {
            for (MainListener listener : listeners) {
                listener.afterStop(controller.getMain());
            }
        }
    }
}
