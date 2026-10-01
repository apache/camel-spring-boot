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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for Camel CLI Connector
 */
@ConfigurationProperties(prefix = "camel.cli")
public class CliConnectorConfiguration {

    /**
     * Whether CLI connector is enabled.
     */
    private Boolean enabled = true;
    /**
     * How the connector talks to the tooling: file (exchanges files in ~/.camel with the Camel CLI) or websocket (dials
     * out to a developer tool over a WebSocket, see camel.cli.websocket).
     */
    private String transport = "file";
    /**
     * The WebSocket transport (camel.cli.transport=websocket). It gives the connected tool full control of the
     * application, it is for development only and refused with the prod profile.
     */
    private Websocket websocket = new Websocket();

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public Websocket getWebsocket() {
        return websocket;
    }

    public void setWebsocket(Websocket websocket) {
        this.websocket = websocket;
    }

    /**
     * Options of the WebSocket transport. Camel reads them from the Spring environment: they are declared here so they
     * are documented and completed by IDEs.
     */
    public static class Websocket {

        /**
         * The ws:// or wss:// URL of the tool. Required when camel.cli.transport=websocket.
         */
        private String url;
        /**
         * Sent as Authorization: Bearer token when connecting. Required unless the URL is a loopback address. Prefer
         * the CAMEL_CLI_WEBSOCKET_TOKEN environment variable to keep it out of the process list.
         */
        private String token;
        /**
         * How often snapshots are sent, in millis.
         */
        private Long snapshotInterval = 1000L;
        /**
         * How often the connection is checked with a ping, in millis. The connection is re-established when the tool
         * does not answer for three intervals.
         */
        private Long heartbeatInterval = 10000L;
        /**
         * Delay before reconnecting, in millis. It doubles after each failed attempt (with some random jitter).
         */
        private Long reconnectDelay = 1000L;
        /**
         * Maximum delay before reconnecting, in millis.
         */
        private Long reconnectMaxDelay = 30000L;
        /**
         * Allow ws:// (unencrypted) to a host that is not a loopback address. Prefer wss:// or a tunnel.
         */
        private Boolean allowInsecure = false;
        /**
         * The WebSocket client: auto uses the Spring WebSocket client when the application has spring-websocket and a
         * Jakarta WebSocket implementation (such as with spring-boot-starter-websocket), otherwise the JDK client; jdk
         * always uses the JDK client.
         */
        private String client = "auto";
        /**
         * The name of an SSL bundle (spring.ssl.bundle) to trust the tool with wss:// when using the Spring WebSocket
         * client.
         */
        private String sslBundle;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Long getSnapshotInterval() {
            return snapshotInterval;
        }

        public void setSnapshotInterval(Long snapshotInterval) {
            this.snapshotInterval = snapshotInterval;
        }

        public Long getHeartbeatInterval() {
            return heartbeatInterval;
        }

        public void setHeartbeatInterval(Long heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
        }

        public Long getReconnectDelay() {
            return reconnectDelay;
        }

        public void setReconnectDelay(Long reconnectDelay) {
            this.reconnectDelay = reconnectDelay;
        }

        public Long getReconnectMaxDelay() {
            return reconnectMaxDelay;
        }

        public void setReconnectMaxDelay(Long reconnectMaxDelay) {
            this.reconnectMaxDelay = reconnectMaxDelay;
        }

        public Boolean getAllowInsecure() {
            return allowInsecure;
        }

        public void setAllowInsecure(Boolean allowInsecure) {
            this.allowInsecure = allowInsecure;
        }

        public String getClient() {
            return client;
        }

        public void setClient(String client) {
            this.client = client;
        }

        public String getSslBundle() {
            return sslBundle;
        }

        public void setSslBundle(String sslBundle) {
            this.sslBundle = sslBundle;
        }
    }
}
