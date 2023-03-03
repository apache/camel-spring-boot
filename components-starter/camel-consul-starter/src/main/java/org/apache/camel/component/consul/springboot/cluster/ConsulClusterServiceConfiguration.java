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
package org.apache.camel.component.consul.springboot.cluster;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.orbitz.consul.option.ConsistencyMode;

import org.apache.camel.component.consul.cluster.ConsulClusterConfiguration;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.cluster.consul")
public class ConsulClusterServiceConfiguration extends ConsulClusterConfiguration {

    /**
     * Sets if the consul cluster service should be enabled or not, default is false.
     */
    private boolean enabled;

    /**
     * Cluster Service ID
     */
    private String id;

    /**
     * Custom service attributes.
     */
    private Map<String, Object> attributes;
    
    /**
     * Service lookup order/priority.
     */
    private Integer order;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }


    //
    // Fields copied from org.apache.camel.component.consul.cluster.ConsulClusterConfiguration
    // to add javadoc which is used by spring-boot-configuration-processor
    // to generate descritpions for inherited properties
    // ------------------------------------------
    /**
     * The value of TTL
     */
    private int sessionTtl = 60;

    /**
     * The value for lockDelay
     */
    private int sessionLockDelay = 5;

    /**
     * The value of wait attribute
     */
    private int sessionRefreshInterval = 5;

    /**
     * Consul root path
     */
    private String rootPath = "/camel";

    // 
    // Fields copied from 
    // org.apache.camel.component.consul.ConsulClientConfiguration
    // ---------------

    /**
     * The Consul agent URL
     */
    private String url;

    /**
     * Use datacenter instead
     */
    private String dc;

    /**
     * The data center
     */
    private String datacenter;

    /**
     * The near node to use for queries.
     */
    private String nearNode;

    /**
     * The note meta-data to use for queries.
     */
    private List<String> nodeMeta;

    /**
     * The consistencyMode used for queries, default ConsistencyMode.DEFAULT
     */
    private ConsistencyMode consistencyMode = ConsistencyMode.DEFAULT;

    /**
     * Set tags. You can separate multiple tags by comma.
     */
    private Set<String> tags;

    /**
     * SSL configuration using an
     * org.apache.camel.support.jsse.SSLContextParameters instance.
     */
    private SSLContextParameters sslContextParameters;

    /**
     * Sets the ACL token to be used with Consul
     */
    private String aclToken;

    /**
     * Sets the username to be used for basic authentication
     */
    private String userName;

    /**
     * Sets the password to be used for basic authentication
     */
    private String password;

    /**
     * Use connectTimeout instead
     */
    private Long connectTimeoutMillis;

    /**
     * Connect timeout for OkHttpClient
     */
    private Duration connectTimeout;

    /**
     * Use readTimeout instead.
     */
    private Long readTimeoutMillis;

    /**
     * Read timeout for OkHttpClient
     */
    private Duration readTimeout;

    /**
     * Use writeTimeout instead.
     */
    private Long writeTimeoutMillis;

    /**
     * Write timeout for OkHttpClient
     */
    private Duration writeTimeout;

    /**
     * Configure if the AgentClient should attempt a ping before returning the
     * Consul instance
     */
    private boolean pingInstance = true;

    /**
     * The time (in seconds) to wait for a watch event, default 10 seconds
     */
    private Integer blockSeconds = 10;

    /**
     * The first index for watch for, default 0
     */
    private BigInteger firstIndex = BigInteger.valueOf(0L);

    /**
     * Recursively watch, default false
     */
    private boolean recursive;
}

