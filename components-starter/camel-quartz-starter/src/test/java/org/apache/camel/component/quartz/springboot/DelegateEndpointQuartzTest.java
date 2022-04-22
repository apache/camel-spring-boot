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
package org.apache.camel.component.quartz.springboot;



import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.DelegateEndpoint;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.quartz.QuartzConstants;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.DefaultEndpoint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.util.URISupport;
import org.quartz.JobDetail;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        DelegateEndpointQuartzTest.class,
        DelegateEndpointQuartzTest.TestConfiguration.class
    }
)
public class DelegateEndpointQuartzTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Bean("my")
    private MyComponent getMyComponent() {
        return new MyComponent();
    }
    
    class MyComponent extends DefaultComponent {

        @Override
        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
                throws Exception {

            String childUri = remaining;
            // we need to apply the params here
            if (parameters != null && parameters.size() > 0) {
                childUri = childUri + "?" + URISupport.createQueryString(parameters);
            }
            // need to clean the parameters to avoid default component verify parameter complain
            parameters.clear();
            Endpoint childEndpoint = context.getEndpoint(childUri);
            return new MyEndpoint(uri, childEndpoint);
        }

    }

    class MyEndpoint extends DefaultEndpoint implements DelegateEndpoint {
        private final Endpoint childEndpoint;

        MyEndpoint(String uri, Endpoint childEndpoint) {
            super(uri, null);
            this.childEndpoint = childEndpoint;
        }

        @Override
        public Producer createProducer() throws Exception {
            return childEndpoint.createProducer();
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return childEndpoint.createConsumer(processor);
        }

        @Override
        public boolean isSingleton() {
            return false;
        }

        @Override
        public Endpoint getEndpoint() {
            return childEndpoint;
        }

        @Override
        protected String createEndpointUri() {
            return "my:" + childEndpoint.getEndpointUri();
        }

    }
    
    @Test
    public void testQuartzCronRoute() throws Exception {
        
        mock.expectedMinimumMessageCount(3);

        mock.assertIsSatisfied();

        JobDetail job = mock.getReceivedExchanges().get(0).getIn().getHeader("jobDetail", JobDetail.class);
        assertNotNull(job);

        assertEquals("cron", job.getJobDataMap().get(QuartzConstants.QUARTZ_TRIGGER_TYPE));
        assertEquals("0/2 * * * * ?", job.getJobDataMap().get(QuartzConstants.QUARTZ_TRIGGER_CRON_EXPRESSION));
    }
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("my:quartz://myGroup/myTimerName?cron=0/2+*+*+*+*+?").to("mock:result");
                }
            };
        }
    }
    
   

}
