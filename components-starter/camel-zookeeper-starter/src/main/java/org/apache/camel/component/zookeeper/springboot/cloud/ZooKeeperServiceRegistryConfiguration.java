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
package org.apache.camel.component.zookeeper.springboot.cloud;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.AuthInfo;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.component.zookeeper.service-registry")
public class ZooKeeperServiceRegistryConfiguration extends org.apache.camel.component.zookeeper.cloud.ZooKeeperServiceRegistryConfiguration {
    /**
     * Sets if the zookeeper service registry should be enabled or not, default is false.
     */
    private boolean enabled;

    /**
     * Service Registry ID
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
    // Fields copied from ZooKeeperServiceRegistryConfiguration to add
    // javadoc which is used by spring-boot-configuration-processor
    // to generate descritpions for inherited properties
    // ------------------------------------------
    /**
     * Should we remove all the registered services know by this registry on stop ? 
     * Default is true.
     */
    private boolean deregisterServicesOnStop = true;

    /**
     * Should we override the service host if given ? 
     * Default is true.
     */
    private boolean overrideServiceHost = true;

    /**
     * Service host.
     */
    private String serviceHost;

    /**
     * The Zookeeper server hosts (multiple servers can be separated by comma).
     */
    private List<String> nodes;

    /**
     * The base path to store in ZooKeeper.
     */
    private String basePath;

    /**
     * ZooKeeper namespace. If a namespace is set here, all paths will get pre-pended with the namespace.
     */
    private String namespace;

    /**
     * Initial amount of time to wait between retries.
     */
    private long reconnectBaseSleepTime;

    /**
     * ReconnectBaseSleepTime TimeUnit.
     * Default is TimeUnit.MILLISECONDS.
     */
    private TimeUnit reconnectBaseSleepTimeUnit = TimeUnit.MILLISECONDS;

    /**
     * Max number of times to retry.
     * Default is 3.
     */
    private int reconnectMaxRetries = 3;

    /**
     * Max time to sleep on each retry.
     * Default is Integer.MAX_VALUE.
     */
    private long reconnectMaxSleepTime = Integer.MAX_VALUE;
    
    /**
     * ReconnectMaxSleepTimeUnit TimeUnit.
     * Default is TimeUnit.MILLISECONDS.
     */
    private TimeUnit reconnectMaxSleepTimeUnit = TimeUnit.MILLISECONDS;
    
    /**
     * Session timeout.
     * Default is 60000.
     */
    private long sessionTimeout = 60 * 1000;

    /**
     * Session timeout TimeUnit.
     * Default is TimeUnit.MILLISECONDS.
     */
    private TimeUnit sessionTimeoutUnit =  TimeUnit.MILLISECONDS;

    /**
     * Connection timeout.
     * Default is 15000.
     */
    private long connectionTimeout = 15 * 1000;

    /**
     * Connection timeout TimeUnit.
     * Default is TimeUnit.MILLISECONDS.
     */
    private TimeUnit connectionTimeoutUnit = TimeUnit.MILLISECONDS;
    
    /**
     * Time to wait during close to join background threads.
     * Default is 1000.
     */
    private long maxCloseWait = 1000;
    
    /**
     * MaxCloseWait TimeUnit.
     * Default is TimeUnit.MILLISECONDS.
     */
    private TimeUnit maxCloseWaitUnit = TimeUnit.MILLISECONDS;
    
    /**
     * Zookeeper CuratorFramework-style client.
     */
    private CuratorFramework curatorFramework;

    /**
     * List of AuthInfo objects with scheme and auth.
     */
    private List<AuthInfo> authInfoList;

    /** 
     * Retry policy to use.
     */
    private RetryPolicy retryPolicy;
}
