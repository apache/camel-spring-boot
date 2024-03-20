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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.impl.CatalogHelper;

/**
 * A Spring Boot based {@link RuntimeProvider} which only includes the supported Camel components, data formats, and languages
 * which can be installed in Spring Boot using the starter dependencies.
 */
public class SpringBootRuntimeProvider implements RuntimeProvider {

    private static final String COMPONENT_DIR = "org/apache/camel/springboot/catalog/components";
    private static final String DATAFORMAT_DIR = "org/apache/camel/springboot/catalog/dataformats";
    private static final String LANGUAGE_DIR = "org/apache/camel/springboot/catalog/languages";
    private static final String TRANSFORMER_DIR = "org/apache/camel/springboot/catalog/transformers";
    private static final String CONSOLE_DIR = "org/apache/camel/springboot/catalog/dev-consoles";
    private static final String OTHER_DIR = "org/apache/camel/springboot/catalog/others";
    private static final String BEAN_DIR = "org/apache/camel/springboot/catalog/beans";
    private static final String COMPONENTS_CATALOG = "org/apache/camel/springboot/catalog/components.properties";
    private static final String DATA_FORMATS_CATALOG = "org/apache/camel/springboot/catalog/dataformats.properties";
    private static final String LANGUAGE_CATALOG = "org/apache/camel/springboot/catalog/languages.properties";
    private static final String TRANSFORMER_CATALOG = "org/apache/camel/springboot/catalog/transformers.properties";
    private static final String CONSOLES_CATALOG = "org/apache/camel/springboot/catalog/dev-consoles.properties";
    private static final String OTHER_CATALOG = "org/apache/camel/springboot/catalog/others.properties";
    private static final String BEAN_CATALOG = "org/apache/camel/springboot/catalog/beans.properties";
    private static final String CAPABILITIES_CATALOG = "org/apache/camel/springboot/catalog/capabilities.properties";

    private CamelCatalog camelCatalog;

    @Override
    public CamelCatalog getCamelCatalog() {
        return camelCatalog;
    }

    @Override
    public void setCamelCatalog(CamelCatalog camelCatalog) {
        this.camelCatalog = camelCatalog;
    }

    @Override
    public String getProviderName() {
        return "springboot";
    }

    @Override
    public String getProviderGroupId() {
        return "org.apache.camel";
    }

    @Override
    public String getProviderArtifactId() {
        return "camel-catalog-provider-springboot";
    }

    @Override
    public String getComponentJSonSchemaDirectory() {
        return COMPONENT_DIR;
    }

    @Override
    public String getDataFormatJSonSchemaDirectory() {
        return DATAFORMAT_DIR;
    }

    @Override
    public String getLanguageJSonSchemaDirectory() {
        return LANGUAGE_DIR;
    }

    @Override
    public String getTransformerJSonSchemaDirectory() {
        return TRANSFORMER_DIR;
    }

    @Override
    public String getDevConsoleJSonSchemaDirectory() {
        return CONSOLE_DIR;
    }

    @Override
    public String getOtherJSonSchemaDirectory() {
        return OTHER_DIR;
    }

    @Override
    public String getPojoBeanJSonSchemaDirectory() {
        return BEAN_DIR;
    }

    @Override
    public List<String> findComponentNames() {
        return findNames(COMPONENTS_CATALOG);
    }

    @Override
    public List<String> findDataFormatNames() {
        return findNames(DATA_FORMATS_CATALOG);
    }

    @Override
    public List<String> findLanguageNames() {
        return findNames(LANGUAGE_CATALOG);
    }

    @Override
    public List<String> findTransformerNames() {
        return findNames(TRANSFORMER_CATALOG);
    }

    @Override
    public List<String> findDevConsoleNames() {
        return findNames(CONSOLES_CATALOG);
    }

    @Override
    public List<String> findOtherNames() {
        return findNames(OTHER_CATALOG);
    }

    @Override
    public List<String> findBeansNames() {
        return findNames(BEAN_CATALOG);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Map<String, String> findCapabilities() {
        final Properties properties = new Properties();

        try (InputStream is = getCamelCatalog().getVersionManager().getResourceAsStream(CAPABILITIES_CATALOG)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            // ignore
        }

        return new TreeMap<>((Map<String, String>) (Map) properties);
    }

    private List<String> findNames(String pathToPropertyCatalogDescriptor) {
        List<String> names = new ArrayList<>();
        try (InputStream is = camelCatalog.getVersionManager().getResourceAsStream(pathToPropertyCatalogDescriptor)) {
            if (is != null) {
                try {
                    CatalogHelper.loadLines(is, names);
                } catch (IOException e) {
                    // ignore
                }
            }
        } catch (IOException e1) {
            // ignore
        }
        return names;
    }

}
