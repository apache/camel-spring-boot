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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.camel.cli.connector.CliWebSocketClient;
import org.apache.camel.cli.connector.CliWebSocketHandshakeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class SpringCliWebSocketClientTest {

    private final ToolServer tool = new ToolServer();
    private final SpringCliWebSocketClient client = new SpringCliWebSocketClient();
    private final Recorder listener = new Recorder();

    @BeforeEach
    void startTool() throws Exception {
        tool.start();
    }

    @AfterEach
    void stopTool() throws Exception {
        tool.close();
    }

    @Test
    void sendsTheHeadersAndReassemblesLargeMessages() throws Exception {
        tool.requiredToken = "t0k3n";
        // much larger than the Jakarta WebSocket text buffer (8 KB): Tomcat passes it in parts
        String large = "x".repeat(2 * 1024 * 1024);
        tool.greeting = "{\"big\":\"" + large + "\"}";

        CliWebSocketClient.Channel channel = client.connect(URI.create(tool.url()),
                Map.of("Authorization", "Bearer t0k3n"), listener).toCompletableFuture().get(10, TimeUnit.SECONDS);

        String text = listener.texts.poll(10, TimeUnit.SECONDS);
        assertThat(text).isEqualTo(tool.greeting);

        channel.sendText("{\"v\":1}").toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(tool.frames.poll(10, TimeUnit.SECONDS).getInteger("v")).isEqualTo(1);

        channel.sendPing().toCompletableFuture().get(10, TimeUnit.SECONDS);
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(listener.pongs).isPositive());

        channel.close(1000, "bye").toCompletableFuture().get(10, TimeUnit.SECONDS);
        await().atMost(10, TimeUnit.SECONDS).until(tool.sessions::isEmpty);
    }

    @Test
    void failsTheConnectionOnAMessageLargerThanTheLimit() throws Exception {
        tool.greeting = "x".repeat(CliWebSocketClient.MAX_MESSAGE_SIZE + 1);

        client.connect(URI.create(tool.url()), Map.of(), listener).toCompletableFuture().get(10, TimeUnit.SECONDS);

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> assertThat(listener.error)
                .hasMessage("Message larger than " + CliWebSocketClient.MAX_MESSAGE_SIZE + " chars"));
        await().atMost(10, TimeUnit.SECONDS).until(tool.sessions::isEmpty);
        assertThat(listener.texts).isEmpty();
    }

    @Test
    void reportsTheHttpStatusOfARejectedHandshake() {
        tool.requiredToken = "t0k3n";

        assertThatThrownBy(() -> client.connect(URI.create(tool.url()), Map.of("Authorization", "Bearer wrong"), listener)
                .toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOfSatisfying(CliWebSocketHandshakeException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(401));
    }

    @Test
    void reportsWhenTheToolClosesTheConnection() throws Exception {
        client.connect(URI.create(tool.url()), Map.of(), listener).toCompletableFuture().get(10, TimeUnit.SECONDS);
        await().atMost(10, TimeUnit.SECONDS).until(() -> !tool.sessions.isEmpty());

        tool.sessions.get(0).close();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(listener.closeCode).isEqualTo(1000));
    }

    @Test
    void abortClosesTheConnection() throws Exception {
        CliWebSocketClient.Channel channel = client.connect(URI.create(tool.url()), Map.of(), listener)
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        await().atMost(10, TimeUnit.SECONDS).until(() -> !tool.sessions.isEmpty());

        channel.abort();

        await().atMost(10, TimeUnit.SECONDS).until(tool.sessions::isEmpty);
        assertThatThrownBy(() -> channel.sendText("{}").toCompletableFuture().get(10, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    private static class Recorder implements CliWebSocketClient.Listener {

        final BlockingQueue<String> texts = new LinkedBlockingQueue<>();
        volatile int pongs;
        volatile int closeCode;
        volatile Throwable error;

        @Override
        public void onText(String text) {
            texts.add(text);
        }

        @Override
        public void onPong() {
            pongs++;
        }

        @Override
        public void onClose(int code, String reason) {
            closeCode = code;
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }
    }
}
