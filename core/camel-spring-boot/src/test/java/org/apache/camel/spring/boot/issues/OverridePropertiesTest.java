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
package org.apache.camel.spring.boot.issues;

import java.util.Properties;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.spring.boot.SpringPropertiesParser;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

public class OverridePropertiesTest extends CamelSpringTestSupport {

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        Properties prop = new Properties();
        prop.put("name", "Donald Duck");
        camelContextConfiguration.
                withUseOverridePropertiesWithPropertiesComponent(prop);

        PropertiesComponent pc = (PropertiesComponent) context.getPropertiesComponent();
        pc.setPropertiesParser(new SpringPropertiesParser(applicationContext.getEnvironment()));
        return context;
    }

    @Test
    public void testOverrideProperties() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").setBody().simple("Name is {{name}}").to("mock:result");
            }
        });

        // should find camel override property value
        getMockEndpoint("mock:result").expectedBodiesReceived("Name is Donald Duck");

        template.sendBody("direct:start", "Hello World");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    public void testNotOverrideProperties() throws Exception {
        PropertiesComponent pc = (PropertiesComponent) context.getPropertiesComponent();
        pc.getOverrideProperties().clear();

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").setBody().simple("Name is {{name}}").to("mock:result");
            }
        });

        // should find spring property value
        getMockEndpoint("mock:result").expectedBodiesReceived("Name is Jack Sparrow");

        template.sendBody("direct:start", "Hello World");

        MockEndpoint.assertIsSatisfied(context);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        AbstractApplicationContext ac = new ClassPathXmlApplicationContext();

        // set property into spring
        Properties prop = new Properties();
        prop.put("name", "Jack Sparrow");
        ac.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("custom", prop));

        return ac;
    }
}
