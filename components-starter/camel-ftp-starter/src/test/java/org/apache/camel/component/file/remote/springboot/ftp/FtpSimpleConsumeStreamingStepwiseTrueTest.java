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
package org.apache.camel.component.file.remote.springboot.ftp;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FtpSimpleConsumeStreamingStepwiseTrueTest.class,
                FtpSimpleConsumeStreamingStepwiseTrueTest.TestConfiguration.class
        }
)
public class FtpSimpleConsumeStreamingStepwiseTrueTest extends FtpSimpleConsumeStreamingStepwiseFalseTest {

    @Override
    boolean isStepwise() {
        return true;
    }

    @Override
    void configureMock() {
        mock.expectedMessageCount(0);
    }

    @Override
    void assertMore(MockEndpoint mock) {
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  FtpSimpleConsumeStreamingStepwiseFalseTest.TestConfiguration {
    }
}
