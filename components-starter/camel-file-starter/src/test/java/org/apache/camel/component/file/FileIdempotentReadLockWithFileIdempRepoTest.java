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

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.processor.idempotent.FileIdempotentRepository;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileIdempotentReadLockWithFileIdempRepoTest.class,
                FileIdempotentReadLockWithFileIdempRepoTest.TestConfiguration.class
        }
)
//Based on FileIdempotentReadLockTest
public class FileIdempotentReadLockWithFileIdempRepoTest extends BaseFile {

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    static File repoFile;
    static FileIdempotentRepository myRepo;

    static {
        File f;
        myRepo = null;
        try {
            f = File.createTempFile(FileIdempotentReadLockWithFileIdempRepoTest.class.getName(), "-repo");

            myRepo = new FileIdempotentRepository(f, new HashMap<>());
        } catch (IOException e) {
            //asserted before the test
        }
    }

    @Test
    public void testIdempotentReadLock() throws Exception {
        assertNotNull(myRepo);
        assertEquals(0, myRepo.getCacheSize());

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(2).create();

        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(fileUri(), "Hello World", Exchange.FILE_NAME, "hello.txt");
        template.sendBodyAndHeader(fileUri(), "Bye World", Exchange.FILE_NAME, "bye.txt");

        assertTrue(notify.matches(5, TimeUnit.SECONDS));

        assertMockEndpointsSatisfied();

        // the files are kept on commit
        // if you want to remove them then the idempotent repo need some way to
        // evict idle keys
        assertEquals(2, myRepo.getCacheSize());
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        @Bean(value = "myRepo")
        public IdempotentRepository myRepo() {
            return myRepo;
        }

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from(fileUri("?initialDelay=0&delay=10&readLock=idempotent&idempotentRepository=#myRepo"))
                            .process(new Processor() {
                                @Override
                                public void process(Exchange exchange) throws Exception {
                                    // we are in progress
                                    int size = myRepo.getCacheSize();
                                    assertTrue(size == 1 || size == 2);
                                }
                            }).to("mock:result");
                }
            };
        }
    }
}
