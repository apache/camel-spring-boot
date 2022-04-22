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



import java.util.Date;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.quartz.QuartzComponent;
import org.apache.camel.component.quartz.QuartzConstants;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.quartz.Calendar;
import org.quartz.Scheduler;
import org.quartz.impl.calendar.HolidayCalendar;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        QuartzCustomCalendarNoFireTest.class,
        QuartzCustomCalendarNoFireTest.TestConfiguration.class
    }
)
public class QuartzCustomCalendarNoFireTest extends BaseQuartzTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testQuartzCustomCronRouteNoFire() throws Exception {
        
        mock.expectedMinimumMessageCount(0);

        QuartzComponent component = context.getComponent("quartz", QuartzComponent.class);
        Scheduler scheduler = component.getScheduler();

        Calendar c = scheduler.getCalendar(QuartzConstants.QUARTZ_CAMEL_CUSTOM_CALENDAR);
        Date now = new Date();
        java.util.Calendar tomorrow = java.util.Calendar.getInstance();
        tomorrow.setTime(now);
        tomorrow.add(java.util.Calendar.DAY_OF_MONTH, 1);
        assertEquals(true, c.isTimeIncluded(tomorrow.getTimeInMillis()));
        assertEquals(false, c.isTimeIncluded(now.getTime()));
        mock.assertIsSatisfied();
    }

    @Bean("calendar")
    public HolidayCalendar loadCalendar() {

        HolidayCalendar cal = new HolidayCalendar();
        cal.addExcludedDate(new Date());

        return cal;
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
                    from("quartz://MyTimer?customCalendar=#calendar&cron=05+00+00+*+*+?").to("mock:result");
                }
            };
        }
    }
    
   

}
