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
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.JIRA_CREDENTIALS;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.PROJECT;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.WATCHED_COMPONENTS;
import static org.apache.camel.component.jira.springboot.test.Utils.createIssue;
import static org.apache.camel.component.jira.springboot.test.Utils.setPriority;
import static org.apache.camel.component.jira.springboot.test.Utils.transitionIssueDone;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Priority;
import com.atlassian.jira.rest.client.api.domain.SearchResult;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jira.JiraConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import io.atlassian.util.concurrent.Promise;
import io.atlassian.util.concurrent.Promises;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;



@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        WatchUpdatesConsumerTest.class,
        WatchUpdatesConsumerTest.TestConfiguration.class
    }
)

public class WatchUpdatesConsumerTest {

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
    
    static SearchRestClient searchRestClient;
    
    static List<Issue> issues = new ArrayList<>();
    
    @BeforeAll
    public static void beforeAll() {
        issues.clear();
        issues.add(createIssue(1L));
        issues.add(createIssue(2L));
        issues.add(createIssue(3L));
    }
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                //get chance to mock camelContext/Registry
                jiraRestClientFactory = mock(JiraRestClientFactory.class);
                jiraClient = mock(JiraRestClient.class);
                issueRestClient = mock(IssueRestClient.class);
                searchRestClient = mock(SearchRestClient.class);
                SearchResult result = new SearchResult(0, 50, 100, issues);
                Promise<SearchResult> promiseSearchResult = Promises.promise(result);

                when(jiraClient.getSearchClient()).thenReturn(searchRestClient);
                when(jiraRestClientFactory.createWithBasicHttpAuthentication(any(), any(), any())).thenReturn(jiraClient);
                when(searchRestClient.searchJql(any(), any(), any(), any())).thenReturn(promiseSearchResult);
                
                camelContext.getRegistry().bind(JIRA_REST_CLIENT_FACTORY, jiraRestClientFactory);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here                
            }
        };
    }
    
    @Test
    public void emptyAtStartupTest() throws Exception {
        mockResult.expectedMessageCount(0);
        mockResult.assertIsSatisfied();
    }

    @Test
    public void singleChangeTest() throws Exception {
        Issue issue = setPriority(issues.get(0), new Priority(
                null, 4L, "High", null, null, null));
        reset(searchRestClient);
        AtomicBoolean searched = new AtomicBoolean();
        when(searchRestClient.searchJql(any(), any(), any(), any())).then(invocation -> {

            if (!searched.get()) {
                issues.remove(0);
                issues.add(0, issue);
            }
            SearchResult result = new SearchResult(0, 50, 100, issues);
            return Promises.promise(result);
        });

        mockResult.expectedBodiesReceived(issue.getPriority());
        mockResult.expectedHeaderReceived(JiraConstants.ISSUE_CHANGED, "Priority");
        mockResult.expectedHeaderReceived(JiraConstants.ISSUE_KEY, "TST-1");
        mockResult.expectedMessageCount(1);
        mockResult.assertIsSatisfied(0);
    }

    @Test
    public void multipleChangesWithAddedNewIssueTest() throws Exception {
        final Issue issue = transitionIssueDone(issues.get(1));
        final Issue issue2 = setPriority(issues.get(2), new Priority(
                null, 4L, "High", null, null, null));

        reset(searchRestClient);
        AtomicBoolean searched = new AtomicBoolean();
        when(searchRestClient.searchJql(any(), any(), any(), any())).then(invocation -> {
            if (!searched.get()) {
                issues.add(createIssue(4L));
                issues.remove(1);
                issues.add(1, issue);
                issues.remove(2);
                issues.add(2, issue2);
                searched.set(true);
            }

            SearchResult result = new SearchResult(0, 50, 3, issues);
            return Promises.promise(result);
        });

        mockResult.expectedMessageCount(3);
        mockResult.expectedBodiesReceivedInAnyOrder(issue.getStatus(), issue.getResolution(), issue2.getPriority());
        mockResult.assertIsSatisfied(1000);
    }

    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("jira://watchUpdates?jiraUrl=" + JIRA_CREDENTIALS
                         + "&jql=project=" + PROJECT + "&delay=5000&watchedFields=" + WATCHED_COMPONENTS)
                                 .to(mockResult);
                }
            };
        }
        
      
    }
    
    
    
    
}
