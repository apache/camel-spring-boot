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


import static org.apache.camel.component.jira.JiraConstants.CHILD_ISSUE_KEY;
import static org.apache.camel.component.jira.JiraConstants.JIRA_REST_CLIENT_FACTORY;
import static org.apache.camel.component.jira.JiraConstants.LINK_TYPE;
import static org.apache.camel.component.jira.JiraConstants.PARENT_ISSUE_KEY;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.JIRA_CREDENTIALS;
import static org.apache.camel.component.jira.springboot.test.Utils.createIssue;
import static org.apache.camel.component.jira.springboot.test.Utils.createIssueWithLinks;
import static org.apache.camel.component.jira.springboot.test.Utils.newIssueLink;
import static org.apache.camel.test.junit5.TestSupport.assertIsInstanceOf;
import static org.apache.camel.test.junit5.TestSupport.assertStringContains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.input.LinkIssuesInput;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.mockito.stubbing.Answer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;


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
        AddIssueLinkProducerTest.class,
        AddIssueLinkProducerTest.TestConfiguration.class
    }
)

public class AddIssueLinkProducerTest {

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

    static Issue parentIssue;
    static Issue childIssue;
    
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

                parentIssue = createIssue(1);
                childIssue = createIssue(2);
                
                camelContext.getRegistry().bind(JIRA_REST_CLIENT_FACTORY, jiraRestClientFactory);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here                
            }
        };
    }
    
    @Test
    public void testAddIssueLink() throws InterruptedException {
        String comment = "A new test comment " + new Date();
        String linkType = "Relates";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PARENT_ISSUE_KEY, parentIssue.getKey());
        headers.put(CHILD_ISSUE_KEY, childIssue.getKey());
        headers.put(LINK_TYPE, linkType);

        when(issueRestClient.linkIssue(any(LinkIssuesInput.class)))
                .then((Answer<Void>) inv -> {
                    Collection<IssueLink> links = new ArrayList<>();
                    links.add(newIssueLink(childIssue.getId(), 1, comment));
                    parentIssue = createIssueWithLinks(parentIssue.getId(), links);
                    return null;
                });

        template.sendBodyAndHeaders(comment, headers);

        mockResult.expectedMessageCount(1);
        mockResult.assertIsSatisfied();

        verify(issueRestClient).linkIssue(any(LinkIssuesInput.class));
    }

    @Test
    public void testAddIssueLinkNoComment() throws InterruptedException {
        String linkType = "Relates";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PARENT_ISSUE_KEY, parentIssue.getKey());
        headers.put(CHILD_ISSUE_KEY, childIssue.getKey());
        headers.put(LINK_TYPE, linkType);

        when(issueRestClient.linkIssue(any(LinkIssuesInput.class)))
                .then((Answer<Void>) inv -> {
                    Collection<IssueLink> links = new ArrayList<>();
                    links.add(newIssueLink(childIssue.getId(), 1, null));
                    parentIssue = createIssueWithLinks(parentIssue.getId(), links);
                    return null;
                });

        template.sendBodyAndHeaders(null, headers);

        mockResult.expectedMessageCount(1);
        mockResult.assertIsSatisfied();

        verify(issueRestClient).linkIssue(any(LinkIssuesInput.class));
    }

    @Test
    public void testAddIssueLinkMissingParentIssueKey() throws InterruptedException {
        String comment = "A new test comment " + new Date();
        String linkType = "Relates";
        Map<String, Object> headers = new HashMap<>();
        headers.put(CHILD_ISSUE_KEY, childIssue.getKey());
        headers.put(LINK_TYPE, linkType);

        try {
            template.sendBodyAndHeaders(comment, headers);
            fail("Should have thrown an exception");
        } catch (CamelExecutionException e) {
            IllegalArgumentException cause = assertIsInstanceOf(IllegalArgumentException.class, e.getCause());
            assertStringContains(cause.getMessage(), PARENT_ISSUE_KEY);
        }

        mockResult.expectedMessageCount(0);
        mockResult.assertIsSatisfied();

        verify(issueRestClient, never()).linkIssue(any(LinkIssuesInput.class));
    }

    @Test
    public void testAddIssueLinkMissingChildIssueKey() throws InterruptedException {
        String comment = "A new test comment " + new Date();
        String linkType = "Relates";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PARENT_ISSUE_KEY, parentIssue.getKey());
        headers.put(LINK_TYPE, linkType);

        try {
            template.sendBodyAndHeaders(comment, headers);
            fail("Should have thrown an exception");
        } catch (CamelExecutionException e) {
            IllegalArgumentException cause = assertIsInstanceOf(IllegalArgumentException.class, e.getCause());
            assertStringContains(cause.getMessage(), CHILD_ISSUE_KEY);
        }

        mockResult.expectedMessageCount(0);
        mockResult.assertIsSatisfied();

        verify(issueRestClient, never()).linkIssue(any(LinkIssuesInput.class));
    }

    @Test
    public void testAddIssueLinkMissingLinkType() throws InterruptedException {
        String comment = "A new test comment " + new Date();
        Map<String, Object> headers = new HashMap<>();
        headers.put(PARENT_ISSUE_KEY, parentIssue.getKey());
        headers.put(CHILD_ISSUE_KEY, childIssue.getKey());

        try {
            template.sendBodyAndHeaders(comment, headers);
            fail("Should have thrown an exception");
        } catch (CamelExecutionException e) {
            IllegalArgumentException cause = assertIsInstanceOf(IllegalArgumentException.class, e.getCause());
            assertStringContains(cause.getMessage(), LINK_TYPE);
        }

        mockResult.expectedMessageCount(0);
        mockResult.assertIsSatisfied();

        verify(issueRestClient, never()).linkIssue(any(LinkIssuesInput.class));
    }

    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:start")
                            .to("jira://addIssueLink?jiraUrl=" + JIRA_CREDENTIALS)
                            .to(mockResult);
                }
            };
        }
        
      
    }
    
    
    
    
}
