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
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileBrowsableEndpointTest.class
        }
)
public class FileBrowsableEndpointTest extends BaseFile {

    @Test
    public void testBrowsableTwoFiles() throws Exception {
        template.sendBodyAndHeader(fileUri(), "B", Exchange.FILE_NAME, "b.txt");
        template.sendBodyAndHeader(fileUri(), "A", Exchange.FILE_NAME, "a.txt");

        FileEndpoint endpoint
                = context.getEndpoint(fileUri("?initialDelay=0&delay=10&sortBy=file:name"), FileEndpoint.class);
        assertNotNull(endpoint);

        MemoryIdempotentRepository repo = (MemoryIdempotentRepository) endpoint.getInProgressRepository();
        assertEquals(0, repo.getCacheSize());

        List<Exchange> list = endpoint.getExchanges();
        assertNotNull(list);
        assertEquals(2, list.size());

        assertEquals("a.txt", list.get(0).getIn().getHeader(Exchange.FILE_NAME));
        assertEquals("b.txt", list.get(1).getIn().getHeader(Exchange.FILE_NAME));

        // the in progress repo should not leak
        assertEquals(0, repo.getCacheSize());

        // and the files is still there
        assertTrue(Files.exists(testFile("a.txt")), "File should exist a.txt");
        assertTrue(Files.exists(testFile("b.txt")), "File should exist b.txt");
    }

}
