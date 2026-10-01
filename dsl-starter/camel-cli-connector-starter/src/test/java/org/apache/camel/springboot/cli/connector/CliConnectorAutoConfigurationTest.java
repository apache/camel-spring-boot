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

import javax.net.ssl.SSLContext;

import org.apache.camel.cli.connector.CliWebSocketClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class CliConnectorAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CliConnectorAutoConfiguration.class));

    @Test
    void usesTheSpringClientWhenTheApplicationHasSpringWebSocket() {
        runner.run(context -> assertThat(context).getBean(CliWebSocketClient.class)
                .isInstanceOf(SpringCliWebSocketClient.class)
                .extracting(CliWebSocketClient::getName).isEqualTo("spring"));
    }

    @Test
    void leavesTheJdkClientToCamelWithoutSpringWebSocket() {
        runner.withClassLoader(new FilteredClassLoader(StandardWebSocketClient.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(CliWebSocketClient.class));
    }

    @Test
    void leavesTheJdkClientToCamelWithoutJakartaWebSocket() {
        runner.withClassLoader(new FilteredClassLoader("jakarta.websocket."))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(CliWebSocketClient.class));
    }

    @Test
    void leavesTheJdkClientToCamelWithoutJakartaWebSocketImplementation() {
        // spring-websocket and the Jakarta WebSocket API, but no implementation (no Tomcat, Jetty, ...)
        runner.withClassLoader(new FilteredClassLoader(
                new ClassPathResource("META-INF/services/" + OnSpringWebSocketClientCondition.PROVIDER_CLASS)))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(CliWebSocketClient.class));
    }

    @Test
    void noSpringClientWhenTheJdkClientIsAskedFor() {
        runner.withPropertyValues("camel.cli.websocket.client=jdk")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(CliWebSocketClient.class));
    }

    @Test
    void warnsThatTheSslBundleIsIgnoredWithTheJdkClient(CapturedOutput output) {
        runner.withPropertyValues("camel.cli.transport=websocket", "camel.cli.websocket.ssl-bundle=tool",
                "camel.cli.websocket.client=jdk")
                .run(context -> assertThat(output).contains(
                        "camel.cli.websocket.ssl-bundle=tool is ignored: the JDK WebSocket client is used"
                                                            + " (camel.cli.websocket.client=jdk)"));
    }

    @Test
    void warnsThatTheSslBundleIsIgnoredWithoutSpringWebSocket(CapturedOutput output) {
        runner.withClassLoader(new FilteredClassLoader(StandardWebSocketClient.class))
                .withPropertyValues("camel.cli.transport=websocket", "camel.cli.websocket.ssl-bundle=tool")
                .run(context -> assertThat(output).contains(
                        "camel.cli.websocket.ssl-bundle=tool is ignored: the JDK WebSocket client is used"
                                                            + " (the application has no spring-websocket and Jakarta"
                                                            + " WebSocket implementation)"));
    }

    @Test
    void keepsTheClientOfTheApplication() {
        CliWebSocketClient custom = new SpringCliWebSocketClient();
        runner.withBean(CliWebSocketClient.class, () -> custom)
                .run(context -> assertThat(context).getBean(CliWebSocketClient.class).isSameAs(custom));
    }

    @Test
    void usesTheSslBundle() throws Exception {
        SslBundle bundle = SslBundle.of(SslStoreBundle.NONE);
        runner.withBean(SslBundles.class, () -> new DefaultSslBundleRegistry("tool", bundle))
                .withPropertyValues("camel.cli.websocket.ssl-bundle=tool")
                .run(context -> {
                    SSLContext ssl = ((SpringCliWebSocketClient) context.getBean(CliWebSocketClient.class)).getSslContext();
                    assertThat(ssl).isNotNull();
                    assertThat(ssl.getProtocol()).isEqualTo(bundle.getProtocol());
                });
    }

    @Test
    void failsOnAnSslBundleWithoutSslBundles() {
        runner.withPropertyValues("camel.cli.websocket.ssl-bundle=tool")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasRootCauseMessage(
                                "camel.cli.websocket.ssl-bundle=tool but there are no SSL bundles (spring.ssl.bundle)"));
    }
}
