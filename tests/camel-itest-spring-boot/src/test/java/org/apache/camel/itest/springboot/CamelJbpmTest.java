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
public class CamelJbpmTest extends AbstractSpringBootTestSupport {

    @Deployment
    public static Archive<?> createSpringBootPackage() throws Exception {
        return ArquillianPackager.springBootPackage(createTestConfig());
    }

    public static ITestConfig createTestConfig() {
        return new ITestConfigBuilder()
                .module(inferModuleName(CamelJbpmTest.class))
                .dependency("org.kie:kie-api:7.59.0.Final")
                .dependency("org.kie:kie-dmn-api:7.59.0.Final")
                .dependency("org.kie:kie-dmn-model:7.59.0.Final")
                .dependency("org.kie:kie-internal:7.59.0.Final")
                .dependency("org.drools:drools-compiler:7.59.0.Final")
                .dependency("org.drools:drools-core:7.59.0.Final")
                .dependency("org.drools:drools-core-reflective:7.59.0.Final")
                .dependency("org.drools:drools-core-dynamic:7.59.0.Final")
                .dependency("org.drools:drools-canonical-model:7.59.0.Final")
                .dependency("org.drools:drools-ruleunit:7.59.0.Final")
                .dependency("org.drools:drools-alphanetwork-compiler:7.59.0.Final")
                .dependency("org.drools:drools-model-compiler:7.59.0.Final")
                .dependency("org.drools:drools-mvel-compiler:7.59.0.Final")
                .dependency("org.drools:drools-mvel-parser:7.59.0.Final")
                .unitTestExpectedNumber(0)
                .build();
    }

    @Test
    public void componentTests() throws Exception {
        this.runComponentTest(config);
        this.runModuleUnitTestsIfEnabled(config);
    }


}
