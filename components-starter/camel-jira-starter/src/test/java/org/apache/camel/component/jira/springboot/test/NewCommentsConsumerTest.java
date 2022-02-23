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
import static org.apache.camel.component.jira.springboot.test.Utils.createIssueWithComments;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.JIRA_CREDENTIALS;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.PROJECT;
import static org.apache.camel.component.jira.springboot.test.Utils.createIssue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
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
        NewCommentsConsumerTest.class,
        NewCommentsConsumerTest.TestConfiguration.class
    }
)

public class NewCommentsConsumerTest {

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
        issues.add(createIssueWithComments(1L, 1));
        issues.add(createIssueWithComments(2L, 1));
        issues.add(createIssueWithComments(3L, 1));
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
                Issue issue = createIssueWithComments(4L, 1);
                Promise<Issue> promiseIssue = Promises.promise(issue);

                when(jiraClient.getSearchClient()).thenReturn(searchRestClient);
                when(jiraClient.getIssueClient()).thenReturn(issueRestClient);
                when(jiraRestClientFactory.createWithBasicHttpAuthentication(any(), any(), any())).thenReturn(jiraClient);
                when(searchRestClient.searchJql(any(), any(), any(), any())).thenReturn(promiseSearchResult);
                when(issueRestClient.getIssue(anyString())).thenReturn(promiseIssue);
                
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
    public void singleIssueCommentsTest() throws Exception {
        Issue issueWithCommends = createIssueWithComments(11L, 3000);
        Issue issueWithNoComments = createIssue(51L);

        reset(issueRestClient);
        AtomicInteger regulator = new AtomicInteger();
        when(issueRestClient.getIssue(anyString())).then(inv -> {
            int idx = regulator.getAndIncrement();
            Issue issue = issueWithNoComments;
            if (idx < 1) {
                issue = issueWithCommends;
            }
            return Promises.promise(issue);
        });
        List<Comment> comments = new ArrayList<>();
        for (Comment c : issueWithCommends.getComments()) {
            comments.add(c);
        }
        // reverse the order, from oldest comment to recent
        Collections.reverse(comments);
        // expect 3000 comments
        mockResult.expectedBodiesReceived(comments);
        mockResult.assertIsSatisfied();
    }

    @Test
    public void multipleIssuesTest() throws Exception {
        Issue issue1 = createIssueWithComments(20L, 2000);
        Issue issue2 = createIssueWithComments(21L, 3000);
        Issue issue3 = createIssueWithComments(22L, 1000);
        List<Issue> newIssues = new ArrayList<>();
        newIssues.add(issue1);
        newIssues.add(issue2);
        newIssues.add(issue3);
        Issue issueWithNoComments = createIssue(31L);

        reset(searchRestClient);
        reset(issueRestClient);
        SearchResult searchResult = new SearchResult(0, 50, 3, newIssues);
        Promise<SearchResult> searchResultPromise = Promises.promise(searchResult);
        when(searchRestClient.searchJql(anyString(), any(), any(), any())).thenReturn(searchResultPromise);
        AtomicInteger regulator = new AtomicInteger();
        when(issueRestClient.getIssue(anyString())).then(inv -> {
            int idx = regulator.getAndIncrement();
            Issue issue = issueWithNoComments;
            if (idx < newIssues.size()) {
                issue = newIssues.get(idx);
            }
            return Promises.promise(issue);
        });
        List<Comment> comments = new ArrayList<>();
        for (Issue issue : newIssues) {
            for (Comment c : issue.getComments()) {
                comments.add(c);
            }
        }
        // reverse the order, from oldest comment to recent
        Collections.reverse(comments);
        // expect 6000 comments
        mockResult.expectedBodiesReceived(comments);
        mockResult.assertIsSatisfied();
    }
    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("jira://newComments?jiraUrl=" + JIRA_CREDENTIALS + "&jql=project=" + PROJECT + "&delay=1000")
                            .to(mockResult);
                }
            };
        }
        
      
    }
    
    
    
    
}
