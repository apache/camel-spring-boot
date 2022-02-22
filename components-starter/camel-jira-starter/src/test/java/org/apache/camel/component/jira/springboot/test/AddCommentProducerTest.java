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
import static org.apache.camel.component.jira.JiraConstants.JIRA_REST_CLIENT_FACTORY;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.JIRA_CREDENTIALS;
import static org.apache.camel.component.jira.springboot.test.Utils.createIssueWithComments;
import static org.apache.camel.component.jira.springboot.test.Utils.newComment;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;

import org.apache.camel.CamelContext;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        AddCommentProducerTest.class,
        AddCommentProducerTest.TestConfiguration.class
    }
)

public class AddCommentProducerTest {

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
    
    static String comment;
    
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

                when(issueRestClient.addComment(any(), any())).then((Answer<Void>) inv -> {
                    Collection<Comment> comments = new ArrayList<>();
                    for (Comment c : backendIssue.getComments()) {
                        comments.add(c);
                    }
                    comments.add(newComment(backendIssue.getId(), comments.size() + 1, comment));
                    backendIssue = createIssueWithComments(backendIssue.getId(), comments);
                    return null;
                });
                backendIssue = createIssueWithComments(1, 1);
                when(issueRestClient.getIssue(any())).then(inv -> Promises.promise(backendIssue));
                
                camelContext.getRegistry().bind(JIRA_REST_CLIENT_FACTORY, jiraRestClientFactory);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here                
            }
        };
    }
    
    @Test
    public void verifyLastComment() throws InterruptedException {
        template.sendBodyAndHeader(comment, ISSUE_KEY, backendIssue.getKey());

        Issue issue = issueRestClient.getIssue(backendIssue.getKey()).claim();
        String lastComment = null;
        for (Comment c : issue.getComments()) {
            lastComment = c.getBody();
        }
        assertEquals(comment, lastComment);
        mockResult.expectedMessageCount(1);
        mockResult.assertIsSatisfied();
    }

    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    comment = "A new test comment " + new Date();
                    from("direct:start")
                            .to("jira://addComment?jiraUrl=" + JIRA_CREDENTIALS)
                            .to(mockResult);
                }
            };
        }
        
      
    }
    
    
    
    
}
