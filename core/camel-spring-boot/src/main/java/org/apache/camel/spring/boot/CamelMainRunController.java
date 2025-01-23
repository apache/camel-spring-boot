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

import org.apache.camel.CamelContext;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainShutdownStrategy;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * Controller to keep the main running and perform graceful shutdown when the JVM is stopped.
 */
public class CamelMainRunController {

    private final CamelSpringBootApplicationController controller;
    private final Thread daemon;

    public CamelMainRunController(ApplicationContext applicationContext, CamelContext camelContext) {
        controller = new CamelSpringBootApplicationController(applicationContext);
        controller.setCamelContext(camelContext);

        // setup main listeners eager on controller
        final Map<String, MainListener> listeners = applicationContext.getBeansOfType(MainListener.class);
        for (MainListener listener : listeners.values()) {
            controller.getMain().addMainListener(listener);
        }

        daemon = new Thread(new DaemonTask(), "CamelMainRunController");
    }

    public void start() {
        // avoid starting twice such as suspending and resuming a camel context
        if (!daemon.isAlive()) {
            daemon.start();
        }
    }

    public MainShutdownStrategy getMainShutdownStrategy() {
        return controller.getMainShutdownStrategy();
    }

    public Runnable getMainCompleteTask() {
        return controller.getMainCompletedTask();
    }

    private final class DaemonTask implements Runnable {
        @Override
        public void run() {
            controller.run();
        }
    }
}
