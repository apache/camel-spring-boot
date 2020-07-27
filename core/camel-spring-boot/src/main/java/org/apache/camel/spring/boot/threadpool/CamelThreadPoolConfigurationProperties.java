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
package org.apache.camel.spring.boot.threadpool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.util.concurrent.ThreadPoolRejectedPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.threadpool")
public class CamelThreadPoolConfigurationProperties {

    /**
     * Sets the default core pool size (threads to keep minimum in pool)
     */
    private Integer poolSize;

    /**
     * Sets the default maximum pool size
     */
    private Integer maxPoolSize;

    /**
     * Sets the default keep alive time for inactive threads
     */
    private Long keepAliveTime;

    /**
     * Sets the default time unit used for keep alive time
     */
    private TimeUnit timeUnit;

    /**
     * Sets the default maximum number of tasks in the work queue.
     *
     * Use -1 or an unbounded queue
     */
    private Integer maxQueueSize;

    /**
     * Sets default whether to allow core threads to timeout
     */
    private Boolean allowCoreThreadTimeOut;

    /**
     * Sets the default handler for tasks which cannot be executed by the thread pool.
     */
    private ThreadPoolRejectedPolicy rejectedPolicy;

    /**
     * Adds a configuration for a specific thread pool profile (inherits default values)
     */
    private Map<String, ThreadPoolProfileConfigurationProperties> config = new HashMap<>();

    public Integer getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(Integer poolSize) {
        this.poolSize = poolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Integer getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(Integer maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public Boolean getAllowCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void setAllowCoreThreadTimeOut(Boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    public ThreadPoolRejectedPolicy getRejectedPolicy() {
        return rejectedPolicy;
    }

    public void setRejectedPolicy(ThreadPoolRejectedPolicy rejectedPolicy) {
        this.rejectedPolicy = rejectedPolicy;
    }

    public Map<String, ThreadPoolProfileConfigurationProperties> getConfig() {
        return config;
    }

    public void setConfig(Map<String, ThreadPoolProfileConfigurationProperties> config) {
        this.config = config;
    }

    public boolean isEmpty() {
        return poolSize == null && maxPoolSize == null && keepAliveTime == null && timeUnit == null
                && maxQueueSize == null && allowCoreThreadTimeOut == null && rejectedPolicy == null
                && config.isEmpty();
    }

    @ConfigurationProperties(prefix = "camel.health.config")
    public static class ThreadPoolProfileConfigurationProperties {

        /**
         * Sets the id of this thread pool
         */
        private String id;

        /**
         * Sets the core pool size (threads to keep minimum in pool)
         */
        private Integer poolSize;

        /**
         * Sets the maximum pool size
         */
        private Integer maxPoolSize;

        /**
         * Sets the keep alive time for inactive threads
         */
        private Long keepAliveTime;

        /**
         * Sets the time unit used for keep alive time
         */
        private TimeUnit timeUnit;

        /**
         * Sets the maximum number of tasks in the work queue.
         *
         * Use -1 or an unbounded queue
         */
        private Integer maxQueueSize;

        /**
         * Sets whether to allow core threads to timeout
         */
        private Boolean allowCoreThreadTimeOut;

        /**
         * Sets the handler for tasks which cannot be executed by the thread pool.
         */
        private ThreadPoolRejectedPolicy rejectedPolicy;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(Integer poolSize) {
            this.poolSize = poolSize;
        }

        public Integer getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(Integer maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public Long getKeepAliveTime() {
            return keepAliveTime;
        }

        public void setKeepAliveTime(Long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }

        public Integer getMaxQueueSize() {
            return maxQueueSize;
        }

        public void setMaxQueueSize(Integer maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
        }

        public Boolean getAllowCoreThreadTimeOut() {
            return allowCoreThreadTimeOut;
        }

        public void setAllowCoreThreadTimeOut(Boolean allowCoreThreadTimeOut) {
            this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
        }

        public ThreadPoolRejectedPolicy getRejectedPolicy() {
            return rejectedPolicy;
        }

        public void setRejectedPolicy(ThreadPoolRejectedPolicy rejectedPolicy) {
            this.rejectedPolicy = rejectedPolicy;
        }

    }
}


