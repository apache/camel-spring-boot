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
package org.apache.camel.itest.springboot;

import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Contains the main class of the sample spring-boot application created for the 
 * module under test.
 *
 */
// Don't load the datasource bean here because the properties are only defined in the unit test class (CamelSqlTest)
@SpringBootApplication(excludeName = {"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"}
)
@EnableAsync
@Import(ITestXmlConfiguration.class)
public class ITestApplication {

    public static void main(String[] args) throws Exception {

        try {
            SpringApplication app = new SpringApplication(ITestApplication.class);
            // Prevent writing to stdout
            app.setBannerMode(Mode.OFF);
            // Prevent starting webserver; it will be started in unit tests if required
            app.setWebApplicationType(WebApplicationType.NONE);
            app.run(args);
        } catch (Throwable t) {
            LoggerFactory.getLogger(ITestApplication.class).error("Error while executing test", t);
            throw t;
        }
    }

    @Override
    public String toString() {
        // to tell source-check this is not a utility-class
        return "spring-boot-main";
    }

}
