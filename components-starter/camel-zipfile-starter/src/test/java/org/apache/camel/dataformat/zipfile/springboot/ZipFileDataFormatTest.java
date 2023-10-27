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


import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.test.junit5.TestSupport.assertIsInstanceOf;
import static org.apache.camel.test.junit5.TestSupport.deleteDirectory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.stream.InputStreamCache;
import org.apache.camel.dataformat.zipfile.ZipFileDataFormat;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.util.IOHelper;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        ZipFileDataFormatTest.class,
        ZipFileDataFormatTest.TestConfiguration.class
    }
)
public class ZipFileDataFormatTest {

    
    private static final String TEXT = "The Masque of Queen Bersabe (excerpt) \n"
        + "by: Algernon Charles Swinburne \n\n"
        + "My lips kissed dumb the word of Ah \n"
        + "Sighed on strange lips grown sick thereby. \n"
        + "God wrought to me my royal bed; \n"
        + "The inner work thereof was red, \n"
        + "The outer work was ivory. \n"
        + "My mouth's heat was the heat of flame \n"
        + "For lust towards the kings that came \n"
        + "With horsemen riding royally.";

    private static final File TEST_DIR = new File("target/zip");

    private static ZipFileDataFormat zip;
    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
   
    
    @EndpointInject("mock:zipStreamCache")
    MockEndpoint mockZipStreamCache;
    
    @EndpointInject("mock:zip")
    MockEndpoint mockZip;
    
    
    @Test
    public void testZipAndStreamCaching() throws Exception {
        
        mockZipStreamCache.setExpectedMessageCount(1);

        template.sendBody("direct:zipStreamCache", TEXT);

        mockZipStreamCache.assertIsSatisfied();

        Exchange exchange = mockZipStreamCache.getReceivedExchanges().get(0);
        assertEquals(exchange.getIn().getMessageId() + ".zip", exchange.getIn().getHeader(FILE_NAME));
        assertIsInstanceOf(InputStreamCache.class, exchange.getIn().getBody());
        assertArrayEquals(getZippedText(exchange.getIn().getMessageId()), exchange.getIn().getMandatoryBody(byte[].class));
    }

    @Test
    public void testZipWithoutFileName() throws Exception {
        mockZip.reset();
        mockZip.expectedMessageCount(1);

        template.sendBody("direct:zip", TEXT);

        mockZip.assertIsSatisfied();

        Exchange exchange = mockZip.getReceivedExchanges().get(0);
        assertEquals(exchange.getIn().getMessageId() + ".zip", exchange.getIn().getHeader(FILE_NAME));
        assertArrayEquals(getZippedText(exchange.getIn().getMessageId()), (byte[]) exchange.getIn().getBody(byte[].class));
    }

    @Test
    public void testZipWithFileName() throws Exception {
        mockZip.reset();
        mockZip.expectedBodiesReceived(getZippedText("poem.txt"));
        mockZip.expectedHeaderReceived(FILE_NAME, "poem.txt.zip");

        template.sendBodyAndHeader("direct:zip", TEXT, FILE_NAME, "poem.txt");

        mockZip.assertIsSatisfied();
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testZipWithPathElements() throws Exception {
        mockZip.reset();
        mockZip.expectedBodiesReceived(getZippedText("poem.txt"));
        mockZip.expectedHeaderReceived(FILE_NAME, "poem.txt.zip");

        template.sendBodyAndHeader("direct:zip", TEXT, FILE_NAME, "poems/poem.txt");

        mockZip.assertIsSatisfied();
    }

    @Test
    public void testZipWithPreservedPathElements() throws Exception {
        mockZip.reset();
        zip.setPreservePathElements(true);

        mockZip.expectedBodiesReceived(getZippedTextInFolder("poems/", "poems/poem.txt"));
        mockZip.expectedHeaderReceived(FILE_NAME, "poem.txt.zip");

        template.sendBodyAndHeader("direct:zip", TEXT, FILE_NAME, "poems/poem.txt");

        mockZip.assertIsSatisfied();
    }

    @EndpointInject("mock:unzip")
    MockEndpoint mockUnzip;
    
    @Test
    public void testUnzip() throws Exception {
        mockUnzip.expectedBodiesReceived(TEXT);
        mockUnzip.expectedHeaderReceived(FILE_NAME, "file");

        template.sendBody("direct:unzip", getZippedText("file"));

        mockUnzip.assertIsSatisfied();
    }

    @Test
    public void testUnzipWithEmptyDirectorySupported() {
        deleteDirectory(new File("hello_out"));
        zip.setUsingIterator(true);
        zip.setAllowEmptyDirectory(true);
        template.sendBody("direct:unzipWithEmptyDirectory", new File("src/test/resources/hello.odt"));
        assertTrue(Files.exists(Paths.get("hello_out/Configurations2")));
        deleteDirectory(new File("hello_out"));
    }

    @Test
    public void testUnzipWithEmptyDirectoryUnsupported() {
        deleteDirectory(new File("hello_out"));
        zip.setUsingIterator(true);
        zip.setAllowEmptyDirectory(false);
        template.sendBody("direct:unzipWithEmptyDirectory", new File("src/test/resources/hello.odt"));
        assertTrue(!Files.exists(Paths.get("hello_out/Configurations2")));
        deleteDirectory(new File("hello_out"));
    }

    @Test
    public void testUnzipWithCorruptedZipFile() {
        deleteDirectory(new File("hello_out"));

        assertThrows(CamelExecutionException.class,
                () -> template.sendBody("direct:corruptUnzip", new File("src/test/resources/corrupt.zip")));
    }

    @EndpointInject("mock:zipAndUnzip")
    MockEndpoint mockZipAndUnzip;
    @Test
    public void testZipAndUnzip() throws Exception {
        mockZipAndUnzip.expectedMessageCount(1);

        template.sendBody("direct:zipAndUnzip", TEXT);

        mockZipAndUnzip.assertIsSatisfied();

        Exchange exchange = mockZipAndUnzip.getReceivedExchanges().get(0);
        assertEquals(exchange.getIn().getMessageId(), exchange.getIn().getHeader(FILE_NAME));
        assertEquals(TEXT, new String((byte[]) exchange.getIn().getBody(byte[].class), "UTF-8"));
    }

    @EndpointInject("mock:intercepted")
    MockEndpoint mockIntercepted;
    @Test
    public void testZipToFileWithoutFileName() throws Exception {
        mockIntercepted.reset();
        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();

        String[] files = TEST_DIR.list();
        assertTrue(files == null || files.length == 0);

        
        mockIntercepted.expectedMessageCount(1);

        template.sendBody("direct:zipToFile", TEXT);

        mockIntercepted.assertIsSatisfied();

        // use builder to ensure the exchange is fully done before we check for file exists
        assertTrue(notify.matches(5, TimeUnit.SECONDS), "The exchange is not done in time.");

        Exchange exchange = mockIntercepted.getReceivedExchanges().get(0);
        File file = new File(TEST_DIR, exchange.getIn().getMessageId() + ".zip");
        assertTrue(file.exists(), "The file should exist.");
        assertArrayEquals(getZippedText(exchange.getIn().getMessageId()), getBytes(file), "Get a wrong message content.");
    }

    @EndpointInject("mock:zipToFile")
    MockEndpoint mockZipToFile;
    @Test
    public void testZipToFileWithFileName() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();

        
        mockZipToFile.expectedMessageCount(1);

        File file = new File(TEST_DIR, "poem.txt.zip");
        assertFalse(file.exists(), "The zip should not exit.");

        template.sendBodyAndHeader("direct:zipToFile", TEXT, FILE_NAME, "poem.txt");

        // just make sure the file is created
        mockZipToFile.assertIsSatisfied();

        // use builder to ensure the exchange is fully done before we check for file exists
        assertTrue(notify.matches(5, TimeUnit.SECONDS), "The exchange is not done in time.");

        assertTrue(file.exists(), "The file should exist.");
        assertArrayEquals(getZippedText("poem.txt"), getBytes(file), "Get a wrong message content.");
    }

    @EndpointInject("mock:dslZip")
    MockEndpoint mockDslZip;
    @Test
    public void testDslZip() throws Exception {
        mockDslZip.expectedBodiesReceived(getZippedText("poem.txt"));
        mockDslZip.expectedHeaderReceived(FILE_NAME, "poem.txt.zip");

        template.sendBodyAndHeader("direct:dslZip", TEXT, FILE_NAME, "poem.txt");

        mockDslZip.assertIsSatisfied();
    }

    @EndpointInject("mock:dslUnzip")
    MockEndpoint mockDslUnzip;
    @Test
    public void testDslUnzip() throws Exception {
        mockDslUnzip.expectedBodiesReceived(TEXT);
        mockDslUnzip.expectedHeaderReceived(FILE_NAME, "test.txt");

        template.sendBody("direct:dslUnzip", getZippedText("test.txt"));

        mockDslUnzip.assertIsSatisfied();
    }

    @Test
    public void testUnzipMaxDecompressedSize() {
        // We are only allowing 10 bytes to be decompressed, so we expect an error
        assertThrows(CamelExecutionException.class,
                () -> template.sendBody("direct:unzipMaxDecompressedSize", getZippedText("file")));
    }

    @BeforeEach
    public void setUp() throws Exception {
        deleteDirectory(TEST_DIR);
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }

   

    private static void copy(InputStream in, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            copy(in, out);
        }
    }
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public ZipFileDataFormat zip() {
            return new ZipFileDataFormat();
        }

        @Bean
        public RouteBuilder routeBuilder(ZipFileDataFormat zipDataFormat) {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    interceptSendToEndpoint("file:*").to("mock:intercepted");

                    zip = zipDataFormat;

                    from("direct:zip").marshal(zip).to("mock:zip");
                    from("direct:unzip").unmarshal(zip).to("mock:unzip");
                    from("direct:unzipWithEmptyDirectory").unmarshal(zip)
                            .split(bodyAs(Iterator.class))
                            .streaming()
                            //.to("file:hello_out?autoCreate=true")
                            .process(new Processor() {
                                @Override
                                public void process(Exchange exchange) throws Exception {
                                    ZipFile zfile = new ZipFile(new File("src/test/resources/hello.odt"));
                                    ZipEntry entry = new ZipEntry((String) exchange.getIn().getHeader(Exchange.FILE_NAME));
                                    String outputDirectory = "hello_out";
                                    File file = new File(outputDirectory, entry.getName());

                                    // Check for Path Traversal
                                    File destDirectory = new File(outputDirectory);
                                    String destCanonicalPath = destDirectory.getCanonicalPath();
                                    String outputCanonicalPath = file.getCanonicalPath();
                                    if (!outputCanonicalPath.startsWith(destCanonicalPath)) {
                                        throw new Exception("Zip path traversal found, expected " + destCanonicalPath + " but found " + outputCanonicalPath);
                                    }

                                    if (entry.isDirectory()) {
                                        file.mkdirs();
                                    } else {
                                        file.getParentFile().mkdirs();
                                        InputStream in = zfile.getInputStream(entry);
                                        try {
                                            copy(in, file);
                                        } finally {
                                            in.close();
                                        }
                                    }
                                }
                            })
                            .end();
                    from("direct:zipAndUnzip").marshal(zip).unmarshal(zip).to("mock:zipAndUnzip");
                    from("direct:zipToFile").marshal(zip).to("file:" + TEST_DIR.getPath()).to("mock:zipToFile");
                    from("direct:dslZip").marshal().zipFile().to("mock:dslZip");
                    from("direct:dslUnzip").unmarshal().zipFile().to("mock:dslUnzip");
                    from("direct:corruptUnzip").unmarshal().zipFile().to("mock:corruptUnzip");
                    from("direct:zipStreamCache").streamCaching().marshal().zipFile().to("mock:zipStreamCache");

                    ZipFileDataFormat maxDecompressedSizeZip = new ZipFileDataFormat();
                    // Only allow 10 bytes to be decompressed
                    maxDecompressedSizeZip.setMaxDecompressedSize(10L);
                    from("direct:unzipMaxDecompressedSize").unmarshal(maxDecompressedSizeZip).to("mock:unzip");
                }
            };
        }
    }
    
    private static byte[] getZippedText(String entryName) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        try {
            zos.putNextEntry(new ZipEntry(entryName));
            IOHelper.copy(bais, zos);
        } finally {
            IOHelper.close(bais, zos);
        }
        return baos.toByteArray();
    }

    private static byte[] getZippedTextInFolder(String folder, String file) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        try {
            zos.putNextEntry(new ZipEntry(folder));
            zos.putNextEntry(new ZipEntry(file));
            IOHelper.copy(bais, zos);
        } finally {
            IOHelper.close(bais, zos);
        }
        return baos.toByteArray();
    }

    private static byte[] getBytes(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            IOHelper.copy(fis, baos);
        } finally {
            IOHelper.close(fis, baos);
        }
        return baos.toByteArray();
    }

}
