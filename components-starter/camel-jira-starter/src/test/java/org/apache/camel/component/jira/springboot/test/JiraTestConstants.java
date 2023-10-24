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
package org.apache.camel.component.jira.springboot.test;

import java.io.IOException;
import java.util.Properties;

public class JiraTestConstants {

    static String KEY = "TST";
    static String TEST_JIRA_URL = "https://somerepo.atlassian.net";
    static String PROJECT = "TST";
    static String WATCHED_COMPONENTS = "Priority,Status,Resolution";

    private static Properties loadAuthProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(JiraTestConstants.class.getClassLoader().getResourceAsStream("jiraauth.properties"));
        return properties;
    }

    public static String getJiraCredentials() throws IOException {
        Properties props = loadAuthProperties();
        return TEST_JIRA_URL + "&username=" + props.getProperty("username") + "&password=" + props.getProperty("password");
    }
}
