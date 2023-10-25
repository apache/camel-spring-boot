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


import static org.apache.camel.component.jira.JiraConstants.ISSUE_KEY;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_WATCHERS_ADD;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_WATCHERS_REMOVE;
import static org.apache.camel.component.jira.JiraConstants.JIRA_REST_CLIENT_FACTORY;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.KEY;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.TEST_JIRA_URL;
import static org.apache.camel.component.jira.springboot.test.Utils.createIssue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.BasicWatchers;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Watchers;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        WatcherProducerTest.class,
        WatcherProducerTest.TestConfiguration.class
    }
)

public class WatcherProducerTest {

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

    static Issue backendIssue;
    
    static Issue issue;
    
    static List<String> backendwatchers = new ArrayList<>();
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                //get chance to mock camelContext/Registry
                jiraRestClientFactory = mock(JiraRestClientFactory.class);
                jiraClient = mock(JiraRestClient.class);
                issueRestClient = mock(IssueRestClient.class);
                lenient().when(jiraRestClientFactory.createWithBasicHttpAuthentication(any(), any(), any())).thenReturn(jiraClient);
                lenient().when(jiraClient.getIssueClient()).thenReturn(issueRestClient);

                backendwatchers.add("user1");
                backendwatchers.add("user2");
                backendwatchers.add("user3");
                backendwatchers.add("user4");
                backendwatchers.add("user5");
                URI watchersUri = URI.create(TEST_JIRA_URL + "/rest/api/2/backendIssue/" + KEY + "-11/backendwatchers");
                BasicWatchers initialBasicWatchers = new BasicWatchers(watchersUri, true, backendwatchers.size());
                backendIssue
                        = createIssue(11L, "Test backendIssue", KEY + "-" + 11, null, null, null, null, null, initialBasicWatchers);
                lenient().when(issueRestClient.addWatcher(any(URI.class), anyString())).then(inv -> {
                    String username = inv.getArgument(1);
                    backendwatchers.add(username);
                    BasicWatchers basicWatchers = new BasicWatchers(watchersUri, true, backendwatchers.size());
                    backendIssue = createIssue(backendIssue.getId(), backendIssue.getSummary(), backendIssue.getKey(),
                            backendIssue.getIssueType(), backendIssue.getDescription(),
                            backendIssue.getPriority(), backendIssue.getAssignee(), null, basicWatchers);
                    return null;
                });
                lenient().when(issueRestClient.removeWatcher(any(URI.class), anyString())).then(inv -> {
                    String username = inv.getArgument(1);
                    backendwatchers.remove(username);
                    BasicWatchers basicWatchers = new BasicWatchers(watchersUri, true, backendwatchers.size());
                    backendIssue = createIssue(backendIssue.getId(), backendIssue.getSummary(), backendIssue.getKey(),
                            backendIssue.getIssueType(), backendIssue.getDescription(),
                            backendIssue.getPriority(), backendIssue.getAssignee(), null, basicWatchers);
                    return null;
                });
                lenient().when(issueRestClient.getIssue(anyString())).then(inv -> Promises.promise(backendIssue));
                lenient().when(issueRestClient.getWatchers(any(URI.class))).then(inv -> {
                    Collection<BasicUser> users = new ArrayList<>();
                    for (String watcher : backendwatchers) {
                        users.add(new BasicUser(null, watcher, watcher));
                    }
                    BasicWatchers basicWatchers = new BasicWatchers(watchersUri, true, users.size());
                    Watchers watchers = new Watchers(basicWatchers, users);
                    return Promises.promise(watchers);
                });
                
                camelContext.getRegistry().bind(JIRA_REST_CLIENT_FACTORY, jiraRestClientFactory);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here                
            }
        };
    }
    
    @Test
    public void addWatchers() throws InterruptedException {
        List<String> watchersToAdd = new ArrayList<>();
        watchersToAdd.add("user1A");
        watchersToAdd.add("user1B");
        Map<String, Object> headers = new HashMap<>();
        headers.put(ISSUE_KEY, backendIssue.getKey());
        headers.put(ISSUE_WATCHERS_ADD, watchersToAdd);
        template.sendBodyAndHeaders(null, headers);

        Issue retrievedIssue = issueRestClient.getIssue(backendIssue.getKey()).claim();
        assertEquals(backendIssue, retrievedIssue);
        assertEquals(retrievedIssue.getWatchers().getNumWatchers(), backendwatchers.size());

        Watchers watchers = issueRestClient.getWatchers(retrievedIssue.getWatchers().getSelf()).claim();
        for (BasicUser user : watchers.getUsers()) {
            assertTrue(backendwatchers.contains(user.getName()));
        }
        mockResult.expectedMessageCount(1);
        mockResult.assertIsSatisfied();
    }

    @Test
    public void removeWatchers() throws InterruptedException {
        List<String> watchersToRemove = new ArrayList<>();
        watchersToRemove.add("user2");
        watchersToRemove.add("user3");
        Map<String, Object> headers = new HashMap<>();
        headers.put(ISSUE_KEY, backendIssue.getKey());
        headers.put(ISSUE_WATCHERS_REMOVE, watchersToRemove);
        template.sendBodyAndHeaders(null, headers);

        Issue retrievedIssue = issueRestClient.getIssue(backendIssue.getKey()).claim();
        assertEquals(backendIssue, retrievedIssue);
        assertEquals(retrievedIssue.getWatchers().getNumWatchers(), backendwatchers.size());

        Watchers watchers = issueRestClient.getWatchers(retrievedIssue.getWatchers().getSelf()).claim();
        for (BasicUser user : watchers.getUsers()) {
            assertTrue(backendwatchers.contains(user.getName()));
        }
        mockResult.expectedMessageCount(1);
        mockResult.assertIsSatisfied();
    }

    @Test
    public void addRemoveWatchers() throws InterruptedException {
        List<String> watchersToAdd = new ArrayList<>();
        watchersToAdd.add("user2A");
        watchersToAdd.add("user2B");
        List<String> watchersToRemove = new ArrayList<>();
        watchersToRemove.add("user4");
        watchersToRemove.add("user5");
        Map<String, Object> headers = new HashMap<>();
        headers.put(ISSUE_KEY, backendIssue.getKey());
        headers.put(ISSUE_WATCHERS_ADD, watchersToAdd);
        headers.put(ISSUE_WATCHERS_REMOVE, watchersToRemove);
        template.sendBodyAndHeaders(null, headers);

        Issue retrievedIssue = issueRestClient.getIssue(backendIssue.getKey()).claim();
        assertEquals(backendIssue, retrievedIssue);
        assertEquals(retrievedIssue.getWatchers().getNumWatchers(), backendwatchers.size());

        Watchers watchers = issueRestClient.getWatchers(retrievedIssue.getWatchers().getSelf()).claim();
        for (BasicUser user : watchers.getUsers()) {
            assertTrue(backendwatchers.contains(user.getName()));
        }
        mockResult.expectedMessageCount(1);
        mockResult.assertIsSatisfied();
    }

    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws IOException {
                    from("direct:start")
                            .to("jira://watchers?jiraUrl=" + JiraTestConstants.getJiraCredentials())
                            .to(mockResult);
                }
            };
        }
        
      
    }
    
    
    
    
}
