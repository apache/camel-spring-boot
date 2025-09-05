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
package org.apache.camel.component.platform.http.springboot.actuate.metrics.undertow;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

/**
 * {@link MeterBinder} for Undertow.
 * <p>
 * This binder provides metrics for XNIO worker threads and session management.
 *
 */
public class UndertowMetrics implements MeterBinder, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(UndertowMetrics.class);

    private final XnioWorker xnioWorker;
    private final SessionManager sessionManager;
    private final Deployment deployment;
    private final Iterable<Tag> tags;
    private final MBeanServer mBeanServer;

    public UndertowMetrics(XnioWorker xnioWorker,
                           SessionManager sessionManager,
                           Iterable<Tag> tags) {
        this(xnioWorker, sessionManager, null, tags, ManagementFactory.getPlatformMBeanServer());
    }

    public UndertowMetrics(XnioWorker xnioWorker,
                           SessionManager sessionManager,
                           Deployment deployment,
                           Iterable<Tag> tags) {
        this(xnioWorker, sessionManager, deployment, tags, ManagementFactory.getPlatformMBeanServer());
    }

    public UndertowMetrics(XnioWorker xnioWorker,
                           SessionManager sessionManager,
                           Deployment deployment,
                           Iterable<Tag> tags,
                           MBeanServer mBeanServer) {
        this.xnioWorker = xnioWorker;
        this.sessionManager = sessionManager;
        this.deployment = deployment;
        this.tags = tags;
        this.mBeanServer = mBeanServer;
    }

    public static void monitor(MeterRegistry registry,
                               XnioWorker xnioWorker,
                               SessionManager sessionManager,
                               String... tags) {
        monitor(registry, xnioWorker, sessionManager, Tags.of(tags));
    }

    public static void monitor(MeterRegistry registry,
                               XnioWorker xnioWorker,
                               SessionManager sessionManager,
                               Iterable<Tag> tags) {
        new UndertowMetrics(xnioWorker, sessionManager, tags).bindTo(registry);
    }

    public static void monitor(MeterRegistry registry,
                               XnioWorker xnioWorker,
                               SessionManager sessionManager,
                               Deployment deployment,
                               Iterable<Tag> tags) {
        new UndertowMetrics(xnioWorker, sessionManager, deployment, tags).bindTo(registry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registerWorkerThreadMetrics(registry);
        registerSessionMetrics(registry);
        registerJmxMetrics(registry);
    }

    private void registerWorkerThreadMetrics(MeterRegistry registry) {
        if (xnioWorker == null) {
            return;
        }

        // Core worker pool size
        Gauge.builder("undertow.threads.worker.core", xnioWorker, this::getCoreWorkerPoolSize)
                .tags(tags)
                .baseUnit(BaseUnits.THREADS)
                .description("Core worker thread pool size")
                .register(registry);

        // Maximum worker pool size
        Gauge.builder("undertow.threads.worker.max", xnioWorker, this::getMaxWorkerPoolSize)
                .tags(tags)
                .baseUnit(BaseUnits.THREADS)
                .description("Maximum worker thread pool size")
                .register(registry);

        // Current worker thread count
        Gauge.builder("undertow.threads.worker.current", xnioWorker, this::getCurrentWorkerThreadCount)
                .tags(tags)
                .baseUnit(BaseUnits.THREADS)
                .description("Current worker thread count")
                .register(registry);

        // Busy worker thread count
        Gauge.builder("undertow.threads.worker.busy", xnioWorker, this::getBusyWorkerThreadCount)
                .tags(tags)
                .baseUnit(BaseUnits.THREADS)
                .description("Busy worker thread count")
                .register(registry);

        // Worker thread utilization percentage
        Gauge.builder("undertow.threads.worker.utilization", xnioWorker, this::getWorkerThreadUtilization)
                .tags(tags)
                .baseUnit(BaseUnits.PERCENT)
                .description("Worker thread utilization percentage")
                .register(registry);

        // Worker queue size
        Gauge.builder("undertow.threads.worker.queue.size", xnioWorker, this::getWorkerQueueSize)
                .tags(tags)
                .description("Worker thread queue size")
                .register(registry);

        // IO thread count
        Gauge.builder("undertow.threads.io", xnioWorker, XnioWorker::getIoThreadCount)
                .tags(tags)
                .baseUnit(BaseUnits.THREADS)
                .description("IO thread count")
                .register(registry);
    }

    private void registerSessionMetrics(MeterRegistry registry) {
        if (sessionManager == null) {
            return;
        }

        // Active sessions
        Gauge.builder("undertow.sessions.active.current", sessionManager, this::getActiveSessions)
                .tags(tags)
                .baseUnit(BaseUnits.SESSIONS)
                .description("Current active sessions")
                .register(registry);

        // Maximum sessions (if available)
        Gauge.builder("undertow.sessions.active.max", sessionManager, this::getMaxSessions)
                .tags(tags)
                .baseUnit(BaseUnits.SESSIONS)
                .description("Maximum sessions allowed")
                .register(registry);

        // Session creation rate (if statistics are available)
        FunctionCounter.builder("undertow.sessions.created", sessionManager, this::getCreatedSessions)
                .tags(tags)
                .baseUnit(BaseUnits.SESSIONS)
                .description("Total sessions created")
                .register(registry);

        // Expired sessions
        FunctionCounter.builder("undertow.sessions.expired", sessionManager, this::getExpiredSessions)
                .tags(tags)
                .baseUnit(BaseUnits.SESSIONS)
                .description("Total sessions expired")
                .register(registry);
    }

    private void registerJmxMetrics(MeterRegistry registry) {
        // Register any available JMX-based metrics for Undertow
        // This is a placeholder for when JMX beans are available
        registerJmxMetricsIfAvailable(":type=thread-pool,name=*", registry);
    }

    private void registerJmxMetricsIfAvailable(String objectNamePattern, MeterRegistry registry) {
        try {
            ObjectName pattern = new ObjectName("jboss.threads" + objectNamePattern);
            var objectNames = mBeanServer.queryNames(pattern, null);

            for (ObjectName objectName : objectNames) {
                // Register JMX-based thread pool metrics if available
                registerJmxThreadPoolMetrics(objectName, registry);
            }
        } catch (Exception e) {
            // JMX beans not available, skip
        }
    }

    private void registerJmxThreadPoolMetrics(ObjectName objectName, MeterRegistry registry) {
        Iterable<Tag> allTags = Tags.concat(tags, Tags.of("name", getNameFromObjectName(objectName)));

        Gauge.builder("undertow.threads.jmx.active", mBeanServer,
                        s -> safeDouble(() -> {
                            try {
                                return s.getAttribute(objectName, "ActiveCount");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }))
                .tags(allTags)
                .baseUnit(BaseUnits.THREADS)
                .description("Active threads from JMX")
                .register(registry);

        Gauge.builder("undertow.threads.jmx.pool.size", mBeanServer,
                        s -> safeDouble(() -> {
                            try {
                                return s.getAttribute(objectName, "PoolSize");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }))
                .tags(allTags)
                .baseUnit(BaseUnits.THREADS)
                .description("Thread pool size from JMX")
                .register(registry);
    }

    private double getCoreWorkerPoolSize(XnioWorker worker) {
        return safeDouble(() -> this.xnioWorker.getMXBean().getCoreWorkerPoolSize());
    }

    private double getMaxWorkerPoolSize(XnioWorker worker) {
        return safeDouble(() -> this.xnioWorker.getMXBean().getMaxWorkerPoolSize());
    }

    private double getCurrentWorkerThreadCount(XnioWorker worker) {
        return safeDouble(() -> worker.getMXBean().getWorkerPoolSize());
    }

    private double getBusyWorkerThreadCount(XnioWorker worker) {
        return safeDouble(() -> worker.getMXBean().getBusyWorkerThreadCount());
    }

    private double getWorkerThreadUtilization(XnioWorker worker) {
        double current = getCurrentWorkerThreadCount(worker);
        double total = getCoreWorkerPoolSize(worker);
        if (total > 0 && !Double.isNaN(current) && !Double.isNaN(total)) {
            return (current / total) * 100.0;
        }
        return Double.NaN;
    }

    private double getWorkerQueueSize(XnioWorker worker) {
        return safeDouble(() -> worker.getMXBean().getWorkerQueueSize());
    }

    // Session metrics
    private double getActiveSessions(SessionManager manager) {
        if (manager.getStatistics() != null) {
            return safeDouble(() -> manager.getStatistics().getActiveSessionCount());
        }
        if (deployment != null && deployment.getSessionManager() != null) {
            return safeDouble(() -> deployment.getSessionManager().getActiveSessions().size());
        }
        return Double.NaN;
    }

    private double getMaxSessions(SessionManager manager) {
        if (manager.getStatistics() != null) {
            return safeDouble(() -> manager.getStatistics().getMaxActiveSessions());
        }
        return Double.NaN;
    }

    private double getCreatedSessions(SessionManager manager) {
        if (manager.getStatistics() != null) {
            return safeDouble(() -> manager.getStatistics().getCreatedSessionCount());
        }
        return Double.NaN;
    }

    private double getExpiredSessions(SessionManager manager) {
        if (manager.getStatistics() != null) {
            return safeDouble(() -> manager.getStatistics().getExpiredSessionCount());
        }
        return Double.NaN;
    }

    // Utility methods
    private double safeDouble(Supplier<Object> supplier) {
        try {
            Object result = supplier.get();
            if (result == null) {
                return Double.NaN;
            }
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
            return Double.parseDouble(result.toString());
        } catch (Exception e) {
            LOG.trace(e.getMessage(), e);
            return Double.NaN;
        }
    }

    private String getNameFromObjectName(ObjectName objectName) {
        String name = objectName.getKeyProperty("name");
        return name != null ? name.replace("\"", "") : "unknown";
    }


    @Override
    public void close() {
        // Cleanup any resources if needed
        // Currently no resources to clean up, but this provides the interface
        // for future enhancements
    }
}
