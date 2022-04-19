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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.zipfile.ZipFileDataFormat;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


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
        ZipFileSplitOneFileTest.class,
        ZipFileSplitOneFileTest.TestConfiguration.class
    }
)
public class ZipFileSplitOneFileTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
   
    
    @EndpointInject("mock:end")
    MockEndpoint mockEnd;
    
    @EndpointInject("mock:input")
    MockEndpoint mockInput;
    
    @BeforeEach
    public void setUp() throws Exception {
        TestSupport.deleteDirectory("target/zip-unmarshal");
        
    }

    @Test
    public void testZipFileUnmarshal() throws Exception {
        mockInput.expectedHeaderReceived(Exchange.FILE_NAME_ONLY, "test.zip");
        mockEnd.expectedBodiesReceived("Hello World");

        createZipFile("Hello World");
        mockEnd.assertIsSatisfied();
        mockInput.assertIsSatisfied();
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
                    ZipFileDataFormat zf = new ZipFileDataFormat();
                    zf.setUsingIterator(true);

                    from("file://target/zip-unmarshal?noop=true&include=.*zip").to("mock:input").unmarshal(zf)
                        .split(bodyAs(Iterator.class)).streaming().convertBodyTo(String.class).to("mock:end")
                        .end();
                }
            };
        }
    }
    
    private void createZipFile(String content) throws IOException {
        String basePath = "target" + File.separator + "zip-unmarshal" + File.separator;
        File file = new File(basePath + "test.txt");
        file.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(file);
             FileOutputStream fos = new FileOutputStream(basePath + "test.zip");
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(basePath + "test.txt")) {

            fw.write(content);
            fw.close();

            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);

            int len;
            byte[] buffer = new byte[1024];

            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            zos.closeEntry();
        }
    }

}
