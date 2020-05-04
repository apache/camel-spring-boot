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
package org.apache.camel.example.undertow.spring.boot;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.spring.security.SpringSecurityConfiguration;
import org.apache.camel.component.spring.security.SpringSecurityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

//CHECKSTYLE:OFF
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    private DelegatingFilterProxyRegistrationBean filterProxyRegistrationBean;

    @Component
    public class Routes extends RouteBuilder {
        @Override
        public void configure() {

                from("undertow:http://localhost:8082/hi?securityConfiguration=#springSecurityConfiguration&allowedRoles=role02")
                        .transform(simple("Hello ${in.header."
                                + SpringSecurityProvider.PRINCIPAL_NAME_HEADER
                                + "}!"))
                        .log("${body}");

        }
    }

    @Bean(name = "springSecurityConfiguration")
    public SpringSecurityConfiguration securityConfiguration() {
        return () -> filterProxyRegistrationBean.getFilter();
    }

}
//CHECKSTYLE:ON
