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
package org.apache.camel.component.aws2.ddbstream.springboot;

import javax.annotation.Generated;
import org.apache.camel.component.aws2.ddbstream.Ddb2StreamComponent;
import org.apache.camel.spring.boot.ComponentConfigurationPropertiesCommon;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.services.dynamodb.model.ShardIteratorType;

/**
 * The aws2-ddbstream component is used for working with Amazon DynamoDB
 * Streams.
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.springboot.maven.SpringBootAutoConfigurationMojo")
@ConfigurationProperties(prefix = "camel.component.aws2-ddbstream")
public class Ddb2StreamComponentConfiguration
        extends
            ComponentConfigurationPropertiesCommon {

    /**
     * Whether to enable auto configuration of the aws2-ddbstream component.
     * This is enabled by default.
     */
    private Boolean enabled;
    /**
     * Amazon DynamoDB client to use for all requests for this endpoint. The
     * option is a
     * software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient
     * type.
     */
    private String amazonDynamoDbStreamsClient;
    /**
     * Allows for bridging the consumer to the Camel routing Error Handler,
     * which mean any exceptions occurred while the consumer is trying to pickup
     * incoming messages, or the likes, will now be processed as a message and
     * handled by the routing Error Handler. By default the consumer will use
     * the org.apache.camel.spi.ExceptionHandler to deal with exceptions, that
     * will be logged at WARN or ERROR level and ignored.
     */
    private Boolean bridgeErrorHandler = false;
    /**
     * The component configuration. The option is a
     * org.apache.camel.component.aws2.ddbstream.Ddb2StreamConfiguration type.
     */
    private String configuration;
    /**
     * Defines where in the DynaboDB stream to start getting records. Note that
     * using TRIM_HORIZON can cause a significant delay before the stream has
     * caught up to real-time. if {AT,AFTER}_SEQUENCE_NUMBER are used, then a
     * sequenceNumberProvider MUST be supplied.
     */
    private ShardIteratorType iteratorType = ShardIteratorType.LATEST;
    /**
     * Maximum number of records that will be fetched in each poll
     */
    private Integer maxResultsPerRequest;
    /**
     * To define a proxy host when instantiating the DDBStreams client
     */
    private String proxyHost;
    /**
     * To define a proxy port when instantiating the DDBStreams client
     */
    private Integer proxyPort;
    /**
     * To define a proxy protocol when instantiating the DDBStreams client
     */
    private Protocol proxyProtocol = Protocol.HTTPS;
    /**
     * The region in which DDBStreams client needs to work
     */
    private String region;
    /**
     * Provider for the sequence number when using one of the two
     * ShardIteratorType.{AT,AFTER}_SEQUENCE_NUMBER iterator types. Can be a
     * registry reference or a literal sequence number. The option is a
     * org.apache.camel.component.aws2.ddbstream.SequenceNumberProvider type.
     */
    private String sequenceNumberProvider;
    /**
     * Whether the component should use basic property binding (Camel 2.x) or
     * the newer property binding with additional capabilities
     */
    private Boolean basicPropertyBinding = false;
    /**
     * Amazon AWS Access Key
     */
    private String accessKey;
    /**
     * Amazon AWS Secret Key
     */
    private String secretKey;

    public String getAmazonDynamoDbStreamsClient() {
        return amazonDynamoDbStreamsClient;
    }

    public void setAmazonDynamoDbStreamsClient(
            String amazonDynamoDbStreamsClient) {
        this.amazonDynamoDbStreamsClient = amazonDynamoDbStreamsClient;
    }

    public Boolean getBridgeErrorHandler() {
        return bridgeErrorHandler;
    }

    public void setBridgeErrorHandler(Boolean bridgeErrorHandler) {
        this.bridgeErrorHandler = bridgeErrorHandler;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public ShardIteratorType getIteratorType() {
        return iteratorType;
    }

    public void setIteratorType(ShardIteratorType iteratorType) {
        this.iteratorType = iteratorType;
    }

    public Integer getMaxResultsPerRequest() {
        return maxResultsPerRequest;
    }

    public void setMaxResultsPerRequest(Integer maxResultsPerRequest) {
        this.maxResultsPerRequest = maxResultsPerRequest;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public Protocol getProxyProtocol() {
        return proxyProtocol;
    }

    public void setProxyProtocol(Protocol proxyProtocol) {
        this.proxyProtocol = proxyProtocol;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSequenceNumberProvider() {
        return sequenceNumberProvider;
    }

    public void setSequenceNumberProvider(String sequenceNumberProvider) {
        this.sequenceNumberProvider = sequenceNumberProvider;
    }

    public Boolean getBasicPropertyBinding() {
        return basicPropertyBinding;
    }

    public void setBasicPropertyBinding(Boolean basicPropertyBinding) {
        this.basicPropertyBinding = basicPropertyBinding;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}