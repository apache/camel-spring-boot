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
package org.apache.camel.dataformat.thrift.springboot;

import org.apache.camel.spring.boot.DataFormatConfigurationPropertiesCommon;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Serialize and deserialize messages using Apache Thrift binary data format.
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@ConfigurationProperties(prefix = "camel.dataformat.thrift")
public class ThriftDataFormatConfiguration
        extends
            DataFormatConfigurationPropertiesCommon {

    /**
     * Whether to enable auto configuration of the thrift data format. This is
     * enabled by default.
     */
    private Boolean enabled;
    /**
     * Name of class to use when unmarshalling
     */
    private String instanceClass;
    /**
     * Defines a content type format in which thrift message will be
     * serialized/deserialized from(to) the Java been. The format can either be
     * native or json for either native binary thrift, json or simple json
     * fields representation. The default value is binary.
     */
    private String contentTypeFormat = "binary";
    /**
     * Whether the data format should set the Content-Type header with the type
     * from the data format. For example application/xml for data formats
     * marshalling to XML, or application/json for data formats marshalling to
     * JSON
     */
    private Boolean contentTypeHeader = true;

    public String getInstanceClass() {
        return instanceClass;
    }

    public void setInstanceClass(String instanceClass) {
        this.instanceClass = instanceClass;
    }

    public String getContentTypeFormat() {
        return contentTypeFormat;
    }

    public void setContentTypeFormat(String contentTypeFormat) {
        this.contentTypeFormat = contentTypeFormat;
    }

    public Boolean getContentTypeHeader() {
        return contentTypeHeader;
    }

    public void setContentTypeHeader(Boolean contentTypeHeader) {
        this.contentTypeHeader = contentTypeHeader;
    }
}