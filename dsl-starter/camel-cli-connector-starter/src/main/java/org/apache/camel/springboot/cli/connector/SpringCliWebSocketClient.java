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
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;

import org.apache.camel.cli.connector.CliWebSocketClient;
import org.apache.camel.cli.connector.CliWebSocketHandshakeException;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * The Camel CLI connector WebSocket client of Spring ({@link StandardWebSocketClient}, Jakarta WebSocket, for example
 * Tomcat's <tt>tomcat-embed-websocket</tt>), used when the application has it instead of the JDK client.
 * <p/>
 * Sends go through the asynchronous remote endpoint of the Jakarta session, so a send that hangs does not block a
 * thread and can be aborted by the transport. Partial messages are reassembled here: raising the Jakarta text buffer
 * to the largest accepted message would allocate that buffer for every connection.
 */
public class SpringCliWebSocketClient implements CliWebSocketClient {

    // Tomcat client options (user properties of the endpoint config), ignored by other Jakarta WebSocket clients
    static final String IO_TIMEOUT_PROPERTY = "org.apache.tomcat.websocket.IO_TIMEOUT_MS";
    static final String BLOCKING_SEND_TIMEOUT_PROPERTY = "org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT";
    private static final long TIMEOUT = 10000;
    // each step of the Tomcat handshake has its own timeout: the connection must be open within a few of them
    private static final int CONNECT_TIMEOUTS = 3;

    // Jakarta WebSocket has no status code for a rejected upgrade: Tomcat puts it in the message, as [401]
    private static final Pattern HTTP_STATUS = Pattern.compile("\\[([1-5][0-9]{2})]");

    private final SSLContext sslContext;
    private final long timeout;
    private volatile StandardWebSocketClient client;

    public SpringCliWebSocketClient() {
        this(null);
    }

    /**
     * @param sslContext for wss:// urls, or null for the default one
     */
    public SpringCliWebSocketClient(SSLContext sslContext) {
        this(sslContext, TIMEOUT);
    }

    SpringCliWebSocketClient(SSLContext sslContext, long timeout) {
        this.sslContext = sslContext;
        this.timeout = timeout;
    }

    @Override
    public String getName() {
        return "spring";
    }

    SSLContext getSslContext() {
        return sslContext;
    }

    @Override
    public CompletionStage<Channel> connect(URI url, Map<String, String> headers, Listener listener) {
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        headers.forEach(handshakeHeaders::add);
        Handler handler = new Handler(listener);
        CompletableFuture<Channel> answer = new CompletableFuture<>();
        try {
            client().execute(handler, handshakeHeaders, url).whenComplete((session, e) -> {
                if (e != null) {
                    answer.completeExceptionally(translate(e));
                } else if (!answer.complete(handler.channel)) {
                    // opened after the connect timed out: nobody uses it
                    SpringChannel.abort(session);
                }
            });
        } catch (RuntimeException e) {
            // no Jakarta WebSocket implementation on the classpath
            answer.completeExceptionally(e);
        }
        // the transport only reconnects once this completes: never wait forever, whatever the Jakarta implementation
        return answer.orTimeout(timeout * CONNECT_TIMEOUTS, TimeUnit.MILLISECONDS);
    }

    /**
     * Created on first use, so the application starts even without a Jakarta WebSocket implementation when this client
     * is not used.
     */
    private StandardWebSocketClient client() {
        StandardWebSocketClient answer = client;
        if (answer == null) {
            synchronized (this) {
                answer = client;
                if (answer == null) {
                    answer = new StandardWebSocketClient(ContainerProvider.getWebSocketContainer());
                    answer.setSslContext(sslContext);
                    Map<String, Object> properties = new HashMap<>();
                    // connecting, the TLS handshake, and each read and write of the HTTP upgrade: as the JDK client
                    properties.put(IO_TIMEOUT_PROPERTY, Long.toString(timeout));
                    // pings and close frames are blocking sends
                    properties.put(BLOCKING_SEND_TIMEOUT_PROPERTY, timeout);
                    answer.setUserProperties(properties);
                    answer.setTaskExecutor(new SimpleAsyncTaskExecutor("CliConnectorWebSocketConnect-"));
                    client = answer;
                }
            }
        }
        return answer;
    }

    static Throwable translate(Throwable e) {
        Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
        for (Throwable t = cause; t != null; t = t.getCause() != t ? t.getCause() : null) {
            if (t instanceof DeploymentException && t.getMessage() != null) {
                Matcher m = HTTP_STATUS.matcher(t.getMessage());
                if (m.find()) {
                    return new CliWebSocketHandshakeException(Integer.parseInt(m.group(1)), cause);
                }
            }
        }
        return cause;
    }

    private static final class SpringChannel implements Channel {

        private final Session session;

        SpringChannel(WebSocketSession session) {
            this.session = ((NativeWebSocketSession) session).getNativeSession(Session.class);
        }

        @Override
        public CompletionStage<?> sendText(String text) {
            CompletableFuture<Void> answer = new CompletableFuture<>();
            try {
                session.getAsyncRemote().sendText(text, result -> {
                    if (result.isOK()) {
                        answer.complete(null);
                    } else {
                        answer.completeExceptionally(result.getException());
                    }
                });
            } catch (RuntimeException e) {
                answer.completeExceptionally(e);
            }
            return answer;
        }

        @Override
        public CompletionStage<?> sendPing() {
            try {
                session.getAsyncRemote().sendPing(ByteBuffer.allocate(0));
                return CompletableFuture.completedFuture(null);
            } catch (IOException | RuntimeException e) {
                return CompletableFuture.failedFuture(e);
            }
        }

        @Override
        public CompletionStage<?> close(int code, String reason) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(code), reason));
                return CompletableFuture.completedFuture(null);
            } catch (IOException | RuntimeException e) {
                return CompletableFuture.failedFuture(e);
            }
        }

        @Override
        public void abort() {
            abort(session);
        }

        static void abort(WebSocketSession session) {
            abort(((NativeWebSocketSession) session).getNativeSession(Session.class));
        }

        static void abort(Session session) {
            try {
                // not sent as such: Tomcat tries a close frame for a short time only, then closes the socket
                session.close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "aborted"));
            } catch (IOException | RuntimeException e) {
                // already closed
            }
        }
    }

    private static final class Handler extends AbstractWebSocketHandler {

        private final Listener listener;
        private final StringBuilder partial = new StringBuilder();
        private volatile Channel channel;

        Handler(Listener listener) {
            this.listener = listener;
        }

        @Override
        public boolean supportsPartialMessages() {
            return true;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            // before connect() completes, and before any message
            channel = new SpringChannel(session);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            partial.append(message.getPayload());
            if (partial.length() > MAX_MESSAGE_SIZE) {
                partial.setLength(0);
                // as the JDK client: stop reading right away, the transport reconnects
                SpringChannel.abort(session);
                listener.onError(new IOException("Message larger than " + MAX_MESSAGE_SIZE + " chars"));
            } else if (message.isLast()) {
                String text = partial.toString();
                partial.setLength(0);
                listener.onText(text);
            }
        }

        @Override
        protected void handlePongMessage(WebSocketSession session, PongMessage message) {
            listener.onPong();
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            listener.onError(exception);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            listener.onClose(status.getCode(), status.getReason());
        }
    }
}
