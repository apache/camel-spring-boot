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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.websocket.server.WsSci;
import org.springframework.util.FileSystemUtils;

/**
 * Plays the tool: a WebSocket server (embedded Tomcat) the connector dials out to.
 */
class ToolServer implements AutoCloseable {

    final BlockingQueue<JsonObject> frames = new LinkedBlockingQueue<>();
    final List<Session> sessions = new CopyOnWriteArrayList<>();
    /**
     * When set, a handshake without Authorization: Bearer requiredToken is rejected with HTTP 401.
     */
    volatile String requiredToken;
    /**
     * When set, sent to every connection once open.
     */
    volatile String greeting;
    int port;
    private Path baseDir;
    private Tomcat tomcat;

    ToolServer start() throws Exception {
        tomcat = new Tomcat();
        baseDir = Files.createTempDirectory("tool-server");
        tomcat.setBaseDir(baseDir.toString());
        tomcat.setPort(0);
        tomcat.getConnector();
        Context ctx = tomcat.addContext("", null);
        Tomcat.addServlet(ctx, "default", new HttpServlet() {
        });
        ctx.addServletMappingDecoded("/*", "default");

        FilterDef auth = new FilterDef();
        auth.setFilterName("auth");
        auth.setFilter((request, response, chain) -> {
            String header = ((HttpServletRequest) request).getHeader("Authorization");
            if (requiredToken != null && !("Bearer " + requiredToken).equals(header)) {
                ((HttpServletResponse) response).sendError(401);
            } else {
                chain.doFilter(request, response);
            }
        });
        ctx.addFilterDef(auth);
        FilterMap authMap = new FilterMap();
        authMap.setFilterName("auth");
        authMap.addURLPattern("/*");
        ctx.addFilterMap(authMap);

        ctx.addServletContainerInitializer(new WsSci(), null);
        ctx.addServletContainerInitializer((classes, servletContext) -> {
            ServerContainer container = (ServerContainer) servletContext.getAttribute(ServerContainer.class.getName());
            // the status snapshot is larger than the default 8 KB
            container.setDefaultMaxTextMessageBufferSize(1024 * 1024);
            try {
                container.addEndpoint(ServerEndpointConfig.Builder.create(ToolEndpoint.class, "/connect")
                        .configurator(new ServerEndpointConfig.Configurator() {
                            @Override
                            @SuppressWarnings("unchecked")
                            public <T> T getEndpointInstance(Class<T> endpointClass) {
                                return (T) new ToolEndpoint();
                            }
                        }).build());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }, null);
        tomcat.start();
        port = tomcat.getConnector().getLocalPort();
        return this;
    }

    String url() {
        return "ws://127.0.0.1:" + port + "/connect?executionId=it-1";
    }

    void send(JsonObject frame) throws IOException {
        sessions.get(sessions.size() - 1).getBasicRemote().sendText(frame.toJson());
    }

    /**
     * Waits for a frame matching the predicate, skipping the others (mostly snapshots).
     */
    JsonObject awaitFrame(Predicate<JsonObject> predicate) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < deadline) {
            JsonObject frame = frames.poll(100, TimeUnit.MILLISECONDS);
            if (frame != null && predicate.test(frame)) {
                return frame;
            }
        }
        throw new AssertionError("No matching frame received within 20 seconds");
    }

    JsonObject awaitResult(String requestId) throws InterruptedException {
        return awaitFrame(f -> "result".equals(f.getString("type")) && requestId.equals(f.getString("requestId")));
    }

    @Override
    public void close() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
            tomcat = null;
        }
        if (baseDir != null) {
            FileSystemUtils.deleteRecursively(baseDir);
            baseDir = null;
        }
    }

    class ToolEndpoint extends Endpoint {

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            sessions.add(session);
            session.addMessageHandler(String.class, (MessageHandler.Whole<String>) text -> {
                try {
                    frames.add((JsonObject) Jsoner.deserialize(text));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            String text = greeting;
            if (text != null) {
                session.getAsyncRemote().sendText(text);
            }
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            sessions.remove(session);
        }
    }
}
