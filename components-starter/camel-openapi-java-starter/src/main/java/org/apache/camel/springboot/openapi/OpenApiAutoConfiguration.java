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
package org.apache.camel.springboot.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.apache.camel.CamelContext;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.openapi.BeanConfig;
import org.apache.camel.openapi.DefaultRestDefinitionsResolver;
import org.apache.camel.openapi.RestDefinitionsResolver;
import org.apache.camel.openapi.RestOpenApiReader;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import java.util.List;
import java.util.Map;

import static org.apache.camel.openapi.OpenApiHelper.clearVendorExtensions;

/**
 * Open API auto-configuration.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({OpenApiConfiguration.class})
@ConditionalOnBean(type = "org.apache.camel.spring.boot.CamelAutoConfiguration")
@ConditionalOnProperty(name = "camel.openapi.enabled", matchIfMissing = true)
@AutoConfigureAfter(name = "org.apache.camel.spring.boot.CamelAutoConfiguration")
@AutoConfigureBefore(name = "org.springdoc.core.SpringDocConfiguration")
public class OpenApiAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiAutoConfiguration.class);

    private final RestOpenApiReader reader = new RestOpenApiReader();
    private final RestDefinitionsResolver resolver = new DefaultRestDefinitionsResolver();
    private final OpenAPI openAPI = new OpenAPI();

    @Bean
    CamelContextConfiguration onBeforeStart(GenericApplicationContext ac, CamelContext camelContext, OpenApiConfiguration config) {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                // routes have now been loaded, so we need to detect rest-dsl APIs in Camel
                // this will trigger spring boot to create the bean which springdoc can detect
                try {
                    OpenAPI created = createOpenAPI(camelContext);
                    if (created != null) {
                        LOG.info("OpenAPI ({}) created from Camel Rest-DSL v{} - {}", created.getOpenapi(), created.getInfo().getVersion(), created.getInfo().getTitle());
                        // transfer data to the existing
                        openAPI.setInfo(created.getInfo());
                        openAPI.setOpenapi(created.getOpenapi());
                        if (created.getComponents() != null) {
                            openAPI.setComponents(created.getComponents());
                        }
                        if (created.getExtensions() != null) {
                            openAPI.setExtensions(created.getExtensions());
                        }
                        if (created.getSecurity() != null) {
                            openAPI.setSecurity(created.getSecurity());
                        }
                        if (created.getExternalDocs() != null) {
                            openAPI.setExternalDocs(created.getExternalDocs());
                        }
                        if (created.getPaths() != null) {
                            openAPI.setPaths(created.getPaths());
                        }
                        if (created.getTags() != null) {
                            openAPI.setTags(created.getTags());
                        }
                        // do not copy servers as we use the spring-boot configured setting
                    }
                } catch (Exception e) {
                    LOG.warn("Error generating OpenAPI from Camel Rest DSL due to: " + e.getMessage() + ". This exception is ignored.", e);
                }
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // noop
            }
        };
    }

    @Bean
    OpenAPI camelRestDSLOpenApi() {
        // due to ordering how beans are resolved and setup in spring boot, then we need to create
        // this provisional bean which is later updated with the actual Rest DSL APIs after the routes has been loaded
        // into Camel
        return openAPI;
    }

    private OpenAPI createOpenAPI(CamelContext camelContext) throws Exception {
        List<RestDefinition> rests = resolver.getRestDefinitions(camelContext, null);
        if (rests == null || rests.isEmpty()) {
            return null;
        }

        BeanConfig bc = new BeanConfig();
        Info info = new Info();

        RestConfiguration rc = camelContext.getRestConfiguration();
        initOpenApi(bc, info, rc.getApiProperties());

        OpenAPI openApi = reader.read(camelContext, rests, bc, null, camelContext.getClassResolver());
        if (openApi != null){
            if (!rc.isApiVendorExtension()) {
                clearVendorExtensions(openApi);
            }
            openApi.setInfo(info);
        }
        return openApi;
    }

    private static void initOpenApi(BeanConfig bc, Info info, Map<String, Object> config) {
        // configure openApi options
        String s = (String) config.get("openapi.version");
        if (s != null) {
            bc.setVersion(s);
        }
        s = (String) config.get("base.path");
        if (s != null) {
            bc.setBasePath(s);
        }
        s = (String) config.get("host");
        if (s != null) {
            bc.setHost(s);
        }
        s = (String) config.get("schemes");
        if (s == null) {
            // deprecated due typo
            s = (String) config.get("schemas");
        }
        if (s != null) {
            String[] schemes = s.split(",");
            bc.setSchemes(schemes);
        } else {
            // assume http by default
            bc.setSchemes(new String[] { "http" });
        }

        String version = (String) config.get("api.version");
        String title = (String) config.get("api.title");
        String description = (String) config.get("api.description");
        String termsOfService = (String) config.get("api.termsOfService");
        String licenseName = (String) config.get("api.license.name");
        String licenseUrl = (String) config.get("api.license.url");
        String contactName = (String) config.get("api.contact.name");
        String contactUrl = (String) config.get("api.contact.url");
        String contactEmail = (String) config.get("api.contact.email");

        bc.setTitle(title);
        bc.setLicense(licenseName);
        bc.setLicenseUrl(licenseUrl);

        info.setTitle(title);
        info.setVersion(version);
        info.setDescription(description);
        info.setTermsOfService(termsOfService);
        if (contactName != null) {
            Contact contact = new Contact();
            contact.setName(contactName);
            contact.setEmail(contactEmail);
            contact.setUrl(contactUrl);
            info.setContact(contact);
        }
    }

}
