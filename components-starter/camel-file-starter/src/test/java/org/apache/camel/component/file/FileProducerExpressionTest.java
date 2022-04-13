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
package org.apache.camel.component.file;

import org.apache.camel.Exchange;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Unit test for expression option for file producer.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileProducerExpressionTest.class
        }
)
public class FileProducerExpressionTest extends BaseFile {

    @Test
    public void testProducerDateByHeader() throws Exception {
        template.sendBodyAndHeader(fileUri(), "Hello World", Exchange.FILE_NAME,
                context.resolveLanguage("simple").createExpression("myfile-${date:now:yyyyMMdd}.txt"));

        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        assertFileExists(testFile("myfile-" + date + ".txt"));
    }

}
