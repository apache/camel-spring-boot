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
package org.apache.camel.dataformat.bindy.springboot.csv;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        BindyPojoSimpleCsvMarshallTest.class,
        BindyPojoSimpleCsvMarshallTest.TestConfiguration.class
    }
)
public class BindyPojoSimpleCsvMarshallTest {


    
    private static final String URI_MOCK_RESULT = "mock:result";
    private static final String URI_MOCK_ERROR = "mock:error";
    private static final String URI_DIRECT_START = "direct:start";

    private String expected;

    @Produce(URI_DIRECT_START)
    private ProducerTemplate template;

    @EndpointInject(URI_MOCK_RESULT)
    private MockEndpoint result;

    @Test
    @DirtiesContext
    public void testMarshallMessage() throws Exception {

        expected = "1,B2,Keira,Knightley,ISIN,XX23456789,BUY,Share,400.25,EUR,14-01-2009,17-02-2010 23:27:59\r\n";

        result.expectedBodiesReceived(expected);

        template.sendBody(generateModel());

        result.assertIsSatisfied();
    }

    public Object generateModel() {
        // just use the order POJO directly
        Order order = new Order();
        order.setOrderNr(1);
        order.setOrderType("BUY");
        order.setClientNr("B2");
        order.setFirstName("Keira");
        order.setLastName("Knightley");
        order.setAmount(new BigDecimal("400.25"));
        order.setInstrumentCode("ISIN");
        order.setInstrumentNumber("XX23456789");
        order.setInstrumentType("Share");
        order.setCurrency("EUR");

        Calendar calendar = new GregorianCalendar();
        calendar.set(2009, 0, 14);
        order.setOrderDate(calendar.getTime());

        calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        // 4 hour shift
        // 16-02-2010 23:21:59 by GMT+4
        calendar.set(2010, 1, 17, 19, 27, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        order.setOrderDateTime(calendar.getTime());

        return order;
    }
    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    BindyCsvDataFormat camelDataFormat
                            = new BindyCsvDataFormat(Order.class);
                    camelDataFormat.setLocale("en");

                    // default should errors go to mock:error
                    errorHandler(deadLetterChannel(URI_MOCK_ERROR).redeliveryDelay(0));

                    onException(Exception.class).maximumRedeliveries(0).handled(true);

                    from(URI_DIRECT_START).marshal(camelDataFormat).to(URI_MOCK_RESULT);
                }
            };
        }
    }
    
    @CsvRecord(separator = ",")
    public class Order {

        @DataField(pos = 1)
        private int orderNr;

        @DataField(pos = 2)
        private String clientNr;

        @DataField(pos = 3, defaultValue = "Joe")
        private String firstName;

        @DataField(pos = 4)
        private String lastName;

        @DataField(pos = 5)
        private String instrumentCode;

        @DataField(pos = 6)
        private String instrumentNumber;

        @DataField(pos = 7)
        private String orderType;

        @DataField(name = "Name", pos = 8)
        private String instrumentType;

        @DataField(pos = 9, precision = 2)
        private BigDecimal amount;

        @DataField(pos = 10)
        private String currency;

        @DataField(pos = 11, pattern = "dd-MM-yyyy")
        private Date orderDate;

        @DataField(pos = 12, pattern = "dd-MM-yyyy HH:mm:ss", timezone = "GMT+4")
        private Date orderDateTime;

        public int getOrderNr() {
            return orderNr;
        }

        public void setOrderNr(int orderNr) {
            this.orderNr = orderNr;
        }

        public String getClientNr() {
            return clientNr;
        }

        public void setClientNr(String clientNr) {
            this.clientNr = clientNr;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getInstrumentCode() {
            return instrumentCode;
        }

        public void setInstrumentCode(String instrumentCode) {
            this.instrumentCode = instrumentCode;
        }

        public String getInstrumentNumber() {
            return instrumentNumber;
        }

        public void setInstrumentNumber(String instrumentNumber) {
            this.instrumentNumber = instrumentNumber;
        }

        public String getOrderType() {
            return orderType;
        }

        public void setOrderType(String orderType) {
            this.orderType = orderType;
        }

        public String getInstrumentType() {
            return instrumentType;
        }

        public void setInstrumentType(String instrumentType) {
            this.instrumentType = instrumentType;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public Date getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(Date orderDate) {
            this.orderDate = orderDate;
        }

        public Date getOrderDateTime() {
            return orderDateTime;
        }

        public void setOrderDateTime(Date orderDateTime) {
            this.orderDateTime = orderDateTime;
        }

        @Override
        public String toString() {
            return "Model : " + Order.class.getName() + " : " + this.orderNr + ", " + this.orderType + ", "
                   + String.valueOf(this.amount) + ", " + this.instrumentCode + ", "
                   + this.instrumentNumber + ", " + this.instrumentType + ", " + this.currency + ", " + this.clientNr + ", "
                   + this.firstName + ", " + this.lastName + ", "
                   + String.valueOf(this.orderDate) + ", " + String.valueOf(this.orderDateTime);
        }
    }
    
}
