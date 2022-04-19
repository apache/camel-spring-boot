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
package org.apache.camel.dataformat.zipfile.springboot;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.zipfile.ZipFileDataFormat;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.junit5.TestSupport;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        ZipFileSplitAndDeleteTest.class,
        ZipFileSplitAndDeleteTest.TestConfiguration.class
    }
)
public class ZipFileSplitAndDeleteTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
   
    
    @EndpointInject("mock:end")
    MockEndpoint mockEnd;
    
    
    @BeforeEach
    public void setUp() throws Exception {
        TestSupport.deleteDirectory("target/testDeleteZipFileWhenUnmarshalWithDataFormat");
        TestSupport.deleteDirectory("target/testDeleteZipFileWhenUnmarshalWithSplitter");
        
    }

    @Test
    public void testDeleteZipFileWhenUnmarshalWithDataFormat() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context)
                .from("file://target/" + "testDeleteZipFileWhenUnmarshalWithDataFormat").whenDone(1).create();
        mockEnd.expectedMessageCount(2);
        String zipFile = createZipFile("testDeleteZipFileWhenUnmarshalWithDataFormat");

        mockEnd.assertIsSatisfied();

        notify.matchesWaitTime();

        // the original file should have been deleted
        assertFalse(new File(zipFile).exists(), "File should been deleted");
    }

    @Test
    public void testDeleteZipFileWhenUnmarshalWithSplitter() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context).from("file://target/" + "testDeleteZipFileWhenUnmarshalWithSplitter")
                .whenDone(1).create();
        mockEnd.expectedMessageCount(2);
        String zipFile = createZipFile("testDeleteZipFileWhenUnmarshalWithSplitter");

        mockEnd.assertIsSatisfied();

        notify.matchesWaitTime();

        // the original file should have been deleted,
        assertFalse(new File(zipFile).exists(), "File should been deleted");
    }
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    ZipFileDataFormat dataFormat = new ZipFileDataFormat();
                    dataFormat.setUsingIterator(true);

                    from("file://target/testDeleteZipFileWhenUnmarshalWithDataFormat?delay=10&delete=true")
                            .unmarshal(dataFormat)
                            .split(bodyAs(Iterator.class)).streaming()
                            .convertBodyTo(String.class)
                            .to("mock:end")
                            .end();

                    from("file://target/testDeleteZipFileWhenUnmarshalWithSplitter?delay=10&delete=true")
                            .split(new ZipSplitter()).streaming()
                            .convertBodyTo(String.class)
                            .to("mock:end")
                            .end();
                }
            };
        }
    }
    
    private String createZipFile(String folder) throws IOException {
        Path source = Paths.get("src/test/resources/data.zip");
        Path target = Paths.get("target" + File.separator + folder + File.separator + "data.zip");
        target.toFile().getParentFile().mkdirs();
        Path copy = Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return copy.toAbsolutePath().toString();
    }

}
