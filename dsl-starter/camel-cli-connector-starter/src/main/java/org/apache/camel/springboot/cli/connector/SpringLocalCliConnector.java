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
package org.apache.camel.springboot.cli.connector;

import org.apache.camel.cli.connector.CliConnectorTransport;
import org.apache.camel.cli.connector.LocalCliConnector;
import org.apache.camel.spi.CliConnectorFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.Profiles;

public class SpringLocalCliConnector extends LocalCliConnector {

    private final AbstractApplicationContext applicationContext;

    public SpringLocalCliConnector(CliConnectorFactory cliConnectorFactory,
            AbstractApplicationContext applicationContext) {
        super(cliConnectorFactory);
        this.applicationContext = applicationContext;
    }

    @Override
    protected CliConnectorTransport createTransport(String name) {
        // Camel refuses it with the Camel prod profile (camel.main.profile), which Spring profiles do not set
        if ("websocket".equalsIgnoreCase(name)
                && applicationContext.getEnvironment().acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "The Camel CLI connector websocket transport gives the connected tool full control of this"
                                            + " application and cannot be used with the Spring prod profile."
                                            + " Remove camel.cli.transport=websocket, or use another profile.");
        }
        return super.createTransport(name);
    }

    @Override
    public void sigterm() {
        try {
            // starts a thread that stops Camel, while close() below stops it too (as stop() did): Camel stops a context
            // under a lock, so whichever comes second waits for the first one, then finds it stopped
            super.sigterm();
        } finally {
            // close, not only stop: a stopped context keeps the JVM running (for example the embedded web server),
            // while the Camel CLI (camel stop) and the websocket tools expect the application to exit
            applicationContext.close();
        }
    }
}
