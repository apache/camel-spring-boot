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

import static org.apache.camel.component.jira.JiraConstants.JIRA_REST_CLIENT_FACTORY;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.PROJECT;
import static org.apache.camel.component.jira.springboot.test.Utils.createIssue;
import static org.apache.camel.component.jira.springboot.test.Utils.createIssueWithCreationDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.UserRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;

import com.atlassian.jira.rest.client.api.domain.User;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.atlassian.util.concurrent.Promise;
import io.atlassian.util.concurrent.Promises;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(classes = { CamelAutoConfiguration.class, NewIssuesConsumerTest.class,
        NewIssuesConsumerTest.TestConfiguration.class })

public class NewIssuesConsumerTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    @Produce("direct:start")
    ProducerTemplate template;

    @EndpointInject("mock:result")
    MockEndpoint mockResult;

    static JiraRestClient jiraClient;

    static JiraRestClientFactory jiraRestClientFactory;

    static IssueRestClient issueRestClient;

    static UserRestClient userRestClient;

    static SearchRestClient searchRestClient;

    static List<Issue> issues = new ArrayList<>();

    @BeforeAll
    public static void beforeAll() {
        issues.add(createIssueWithCreationDate(3L, DateTime.now().minusMinutes(10)));
        issues.add(createIssueWithCreationDate(2L, DateTime.now().minusMinutes(8)));
        issues.add(createIssueWithCreationDate(1L, DateTime.now().minusMinutes(6)));
    }

    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                // get chance to mock camelContext/Registry
                jiraRestClientFactory = mock(JiraRestClientFactory.class);
                jiraClient = mock(JiraRestClient.class);
                issueRestClient = mock(IssueRestClient.class);
                searchRestClient = mock(SearchRestClient.class);
                userRestClient = mock(UserRestClient.class);
                SearchResult result = new SearchResult(0, 50, 100, issues);
                Promise<SearchResult> promiseSearchResult = Promises.promise(result);
                User user = new User(
                        null, "admin", null, null, true, null,
                        Map.of("48x48", URI.create("")), DateTime.now().getZone().getID());
                Promise<User> promiseUserResult = Promises.promise(user);

                when(jiraClient.getSearchClient()).thenReturn(searchRestClient);
                when(jiraClient.getUserClient()).thenReturn(userRestClient);
                when(jiraRestClientFactory.createWithBasicHttpAuthentication(any(), any(), any()))
                        .thenReturn(jiraClient);
                when(searchRestClient.searchJql(any(), any(), any(), any())).thenReturn(promiseSearchResult);
                when(userRestClient.getUser(any(URI.class))).thenReturn(promiseUserResult);
                camelContext.getRegistry().bind(JIRA_REST_CLIENT_FACTORY, jiraRestClientFactory);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // do nothing here
            }
        };
    }

    @Test
    public void emptyAtStartupTest() throws Exception {
        mockResult.expectedMessageCount(0);
        mockResult.assertIsSatisfied();
    }

    @Test
    public void singleIssueTest() throws Exception {
        Issue issue = createIssueWithCreationDate(11, DateTime.now());

        reset(searchRestClient);
        AtomicBoolean searched = new AtomicBoolean();
        when(searchRestClient.searchJql(any(), any(), any(), any())).then(invocation -> {
            List<Issue> newIissues = new ArrayList<>();
            if (!searched.get()) {
                newIissues.add(issue);
                searched.set(true);
            }
            SearchResult result = new SearchResult(0, 50, 100, newIissues);
            return Promises.promise(result);
        });
        mockResult.expectedBodiesReceived(issue);
        mockResult.assertIsSatisfied();
    }

    @Test
    public void multipleIssuesTest() throws Exception {
        Issue issue1 = createIssueWithCreationDate(21, DateTime.now());
        Issue issue2 = createIssueWithCreationDate(22, DateTime.now());
        Issue issue3 = createIssueWithCreationDate(23, DateTime.now());

        reset(searchRestClient);
        AtomicBoolean searched = new AtomicBoolean();
        when(searchRestClient.searchJql(any(), any(), any(), any())).then(invocation -> {
            List<Issue> newIssues = new ArrayList<>();
            if (!searched.get()) {
                newIssues.add(issue1);
                newIssues.add(issue2);
                newIssues.add(issue3);
                searched.set(true);
            }
            SearchResult result = new SearchResult(0, 50, 3, newIssues);
            return Promises.promise(result);
        });

        mockResult.expectedBodiesReceivedInAnyOrder(issue1, issue2, issue3);
        mockResult.assertIsSatisfied();
    }

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws IOException {
                    from("jira://newIssues?jiraUrl=" + JiraTestConstants.getJiraCredentials() + "&jql=project="
                            + PROJECT + "&delay=5000").to(mockResult);
                }
            };
        }

    }

}
