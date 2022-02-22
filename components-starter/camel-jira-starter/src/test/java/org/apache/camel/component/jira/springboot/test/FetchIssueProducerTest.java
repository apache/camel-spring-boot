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
import static org.apache.camel.test.junit5.TestSupport.assertIsInstanceOf;
import static org.apache.camel.test.junit5.TestSupport.assertStringContains;
import static org.apache.camel.component.jira.springboot.test.JiraTestConstants.JIRA_CREDENTIALS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.Issue;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

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
        FetchIssueProducerTest.class,
        FetchIssueProducerTest.TestConfiguration.class
    }
)

public class FetchIssueProducerTest {

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
    
    
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                //get chance to mock camelContext/Registry
                jiraRestClientFactory = mock(JiraRestClientFactory.class);
                jiraClient = mock(JiraRestClient.class);
                issueRestClient = mock(IssueRestClient.class);
                backendIssue = mock(Issue.class);
                lenient().when(jiraRestClientFactory.createWithBasicHttpAuthentication(any(), any(), any())).thenReturn(jiraClient);
                lenient().when(jiraClient.getIssueClient()).thenReturn(issueRestClient);

                camelContext.getRegistry().bind(JIRA_REST_CLIENT_FACTORY, jiraRestClientFactory);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                //do nothing here                
            }
        };
    }
    
    @Test
    public void testFetchIssue() throws InterruptedException {
        when(backendIssue.getKey()).thenReturn("TEST-123");
        when(issueRestClient.getIssue(any())).then(inv -> Promises.promise(backendIssue));

        template.sendBodyAndHeader(null, ISSUE_KEY, backendIssue.getKey());

        mockResult.expectedMessageCount(1);
        mockResult.assertIsSatisfied();

        verify(issueRestClient).getIssue(backendIssue.getKey());
    }

    @Test
    public void testFetchIssueMissingIssueKey() throws InterruptedException {
        try {
            template.sendBody(null);
            fail("Should have thrown an exception");
        } catch (CamelExecutionException e) {
            IllegalArgumentException cause = assertIsInstanceOf(IllegalArgumentException.class, e.getCause());
            assertStringContains(cause.getMessage(), ISSUE_KEY);
        }

        mockResult.expectedMessageCount(0);
        mockResult.assertIsSatisfied();

        verify(issueRestClient, never()).getIssue(backendIssue.getKey());
    }

    
    @Configuration
    public class TestConfiguration {
        
        

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                    .to("jira://fetchIssue?jiraUrl=" + JIRA_CREDENTIALS)
                    .to(mockResult);
                }
            };
        }
        
      
    }
    
    
    
    
}
