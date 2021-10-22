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
package org.apache.camel.itest.springboot;

import org.apache.camel.itest.springboot.util.ArquillianPackager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(ArquillianExtension.class)
public class CamelGooglePubsubTest extends AbstractSpringBootTestSupport {

    @Deployment
    public static Archive<?> createSpringBootPackage() throws Exception {
        return ArquillianPackager.springBootPackage(createTestConfig());
    }

    public static ITestConfig createTestConfig() {
        return new ITestConfigBuilder()
                .module(inferModuleName(CamelGooglePubsubTest.class))
                .dependency("com.google.http-client:google-http-client-jackson2:1.34.0")
                .dependency("com.google.http-client:google-http-client:1.34.0")
                .dependency("io.grpc:grpc-alts:1.28.1")
                .dependency("io.grpc:grpc-api:1.28.1")
                .dependency("io.grpc:grpc-auth:1.28.1")
                .dependency("io.grpc:grpc-context:1.28.1")
                .dependency("io.grpc:grpc-core:1.28.1")
                .dependency("io.grpc:grpc-grpclb:1.28.1")
                .dependency("io.grpc:grpc-netty-shaded:1.28.1")
                .dependency("io.grpc:grpc-protobuf:1.28.1")
                .dependency("io.grpc:grpc-protobuf-lite:1.28.1")
                .dependency("io.grpc:grpc-stub:1.28.1")
                .build();
    }

    @Test
    public void componentTests() throws Exception {
        this.runComponentTest(config);
        this.runModuleUnitTestsIfEnabled(config);
    }


}
