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

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.apache.camel.cli.connector.CliWebSocketClient;
import org.apache.camel.spi.CliConnectorFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "camel.cli.enabled", matchIfMissing = true)
@AutoConfigureBefore(name = "org.apache.camel.spring.boot.CamelAutoConfiguration") // configure early to have Camel CLI
                                                                                   // during startup
@EnableConfigurationProperties({ CliConnectorConfiguration.class })
public class CliConnectorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CliConnectorFactory.class)
    public CliConnectorFactory cliConnectorFactory(AbstractApplicationContext applicationContext,
            CliConnectorConfiguration config) {

        CliConnectorFactory answer = new SpringCliConnectorFactory(applicationContext);
        answer.setEnabled(config.getEnabled());
        answer.setRuntime("Spring Boot");
        answer.setRuntimeVersion(SpringBootVersion.getVersion());

        // if packaged as fat-jar then we need to know what was the main class that started this integration
        try {
            Enumeration<URL> en = this.getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (en.hasMoreElements()) {
                URL u = en.nextElement();
                try (InputStream is = u.openStream()) {
                    Manifest manifest = new Manifest(is);
                    String sc = manifest.getMainAttributes().getValue("Start-Class");
                    if (sc != null) {
                        answer.setRuntimeStartClass(sc);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return answer;
    }

    /**
     * The Spring WebSocket client for the websocket transport, when the application has spring-websocket and a Jakarta
     * WebSocket client (such as Tomcat with spring-boot-starter-websocket). Otherwise Camel uses the JDK client.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
            "org.springframework.web.socket.client.standard.StandardWebSocketClient",
            "jakarta.websocket.ContainerProvider" })
    static class SpringWebSocketClientConfiguration {

        @Bean
        @ConditionalOnMissingBean(CliWebSocketClient.class)
        public CliWebSocketClient cliWebSocketClient(
                CliConnectorConfiguration config, ObjectProvider<SslBundles> sslBundles) {
            String bundle = config.getWebsocket().getSslBundle();
            if (bundle == null || bundle.isBlank()) {
                return new SpringCliWebSocketClient();
            }
            SslBundles bundles = sslBundles.getIfAvailable();
            if (bundles == null) {
                throw new IllegalStateException(
                        "camel.cli.websocket.ssl-bundle=" + bundle + " but there are no SSL bundles (spring.ssl.bundle)");
            }
            return new SpringCliWebSocketClient(bundles.getBundle(bundle).createSslContext());
        }
    }
}
