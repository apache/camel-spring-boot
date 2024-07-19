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
package org.apache.camel.spring.boot.actuate.endpoint;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class ActuatorTestControlledRoutes extends RouteBuilder {

    @Bean(name="myCxfEndpointBean")
    private CxfEndpoint myCxfEndpointBean() {
        return new CxfEndpoint();
    }

    @Override
    public void configure() throws Exception {
        from("direct:controlled-foo")
                .routeId("controlled-foo")
                .to("mock:end");

        from("direct:controlled-bar")
                .routeId("controlled-bar")
                .to("cxf:bean:myCxfEndpointBean?wsdlURL=http://localhost:7777/service?wsdl");
    }

}
