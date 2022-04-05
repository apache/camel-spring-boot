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
package org.apache.camel.springboot.catalog;

import java.util.List;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringBootRuntimeProviderTest {

    static CamelCatalog catalog;

    @BeforeAll
    public static void createCamelCatalog() {
        catalog = new DefaultCamelCatalog();
        catalog.setRuntimeProvider(new SpringBootRuntimeProvider());
    }

    @Test
    public void testGetVersion() throws Exception {
        String version = catalog.getCatalogVersion();
        assertNotNull(version);

        String loaded = catalog.getLoadedVersion();
        assertNotNull(loaded);
        assertEquals(version, loaded);
    }

    @Test
    public void testProviderName() throws Exception {
        assertEquals("springboot", catalog.getRuntimeProvider().getProviderName());
    }

    @Test
    public void testFindComponentNames() throws Exception {
        List<String> names = catalog.findComponentNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("file"));
        assertTrue(names.contains("ftp"));
        assertTrue(names.contains("jms"));
        // camel-pac-logging does not work in spring-boot
        assertFalse(names.contains("paxlogging"));
    }

    @Test
    public void testFindDataFormatNames() throws Exception {
        List<String> names = catalog.findDataFormatNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("bindyCsv"));
        assertTrue(names.contains("zipDeflater"));
        assertTrue(names.contains("zipFile"));
    }

    @Test
    public void testFindLanguageNames() throws Exception {
        List<String> names = catalog.findLanguageNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("spel"));
        assertTrue(names.contains("xpath"));
    }

    @Test
    public void testFindOtherNames() throws Exception {
        List<String> names = catalog.findOtherNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("zipkin"));

        assertFalse(names.contains("blueprint"));
    }

    @Test
    public void testComponentArtifactId() throws Exception {
        String json = catalog.componentJSonSchema("ftp");

        assertNotNull(json);
        assertTrue(json.contains("camel-ftp-starter"));
    }

    @Test
    public void testDataFormatArtifactId() throws Exception {
        String json = catalog.dataFormatJSonSchema("bindyCsv");

        assertNotNull(json);
        assertTrue(json.contains("camel-bindy-starter"));
    }

    @Test
    public void testLanguageArtifactId() throws Exception {
        String json = catalog.languageJSonSchema("spel");

        assertNotNull(json);
        assertTrue(json.contains("camel-spring-starter"));
    }

    @Test
    public void testOtherArtifactId() throws Exception {
        String json = catalog.otherJSonSchema("zipkin");

        assertNotNull(json);
        assertTrue(json.contains("camel-zipkin-starter"));
    }

}
