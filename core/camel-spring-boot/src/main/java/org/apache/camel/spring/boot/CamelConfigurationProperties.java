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
package org.apache.camel.spring.boot;

import org.apache.camel.LoggingLevel;
import org.apache.camel.ManagementMBeansLevel;
import org.apache.camel.ManagementStatisticsLevel;
import org.apache.camel.StartupSummaryLevel;
import org.apache.camel.main.DefaultConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "camel.springboot")
public class CamelConfigurationProperties extends DefaultConfigurationProperties<CamelConfigurationProperties> {

    // Spring Boot only Properties
    // ---------------------------

    /**
     * Whether to use the main run controller to ensure the Spring-Boot application
     * keeps running until being stopped or the JVM terminated.
     * You typically only need this if you run Spring-Boot standalone.
     * If you run Spring-Boot with spring-boot-starter-web then the web container keeps the JVM running.
     */
    private boolean mainRunController;

    /**
     * Whether to include non-singleton beans (prototypes) when scanning for RouteBuilder instances.
     * By default only singleton beans is included in the context scan.
     */
    private boolean includeNonSingletons;

    /**
     * Whether to log a WARN if Camel on Spring Boot was immediately shutdown after starting which
     * very likely is because there is no JVM thread to keep the application running.
     */
    private boolean warnOnEarlyShutdown = true;

    // Default Properties via camel-main
    // ---------------------------------

    // IMPORTANT: Must include the options from DefaultConfigurationProperties as spring boot apt compiler
    //            needs to grab the documentation from the javadoc on the field.

    /**
     * Sets the name of the CamelContext.
     */
    private String name;

    /**
     * Sets the description (intended for humans) of the Camel application.
     */
    private String description;

    /**
     * Controls the level of information logged during startup (and shutdown) of CamelContext.
     */
    private StartupSummaryLevel startupSummaryLevel = StartupSummaryLevel.Default;

    /**
     * Timeout in seconds to graceful shutdown Camel.
     */
    private int shutdownTimeout = 300;

    /**
     * Whether Camel should try to suppress logging during shutdown and timeout was triggered,
     * meaning forced shutdown is happening. And during forced shutdown we want to avoid logging
     * errors/warnings et all in the logs as a side-effect of the forced timeout.
     * Notice the suppress is a best effort as there may still be some logs coming
     * from 3rd party libraries and whatnot, which Camel cannot control.
     * This option is default false.
     */
    private boolean shutdownSuppressLoggingOnTimeout;

    /**
     * Sets whether to force shutdown of all consumers when a timeout occurred and thus
     * not all consumers was shutdown within that period.
     *
     * You should have good reasons to set this option to false as it means that the routes
     * keep running and is halted abruptly when CamelContext has been shutdown.
     */
    private boolean shutdownNowOnTimeout = true;

    /**
     * Sets whether routes should be shutdown in reverse or the same order as they where started.
     */
    private boolean shutdownRoutesInReverseOrder = true;

    /**
     * Sets whether to log information about the inflight Exchanges which are still running
     * during a shutdown which didn't complete without the given timeout.
     *
     * This requires to enable the option inflightRepositoryExchangeEnabled.
     */
    private boolean shutdownLogInflightExchangesOnTimeout = true;

    /**
     * UUID generator to use.
     *
     * default (32 bytes), short (16 bytes), classic (32 bytes or longer), simple (long incrementing counter), off
     * (turned off for exchanges - only intended for performance profiling)
     */
    private String uuidGenerator = "default";

    /**
     * Enable JMX in your Camel application.
     */
    private boolean jmxEnabled = true;

    /**
     * Producer template endpoints cache size.
     */
    private int producerTemplateCacheSize = 1000;

    /**
     * Consumer template endpoints cache size.
     */
    private int consumerTemplateCacheSize = 1000;

    /**
     * Whether camel-k style modeline is also enabled when not using camel-k. Enabling this allows to use a camel-k like
     * experience by being able to configure various settings using modeline directly in your route source code.
     */
    private boolean modeline;

    /**
     * Whether to enable developer console (requires camel-console on classpath).
     *
     * The developer console is only for assisting during development. This is NOT for production usage.
     */
    private boolean devConsoleEnabled;

    /**
     * Whether to load custom type converters by scanning classpath.
     * This is used for backwards compatibility with Camel 2.x.
     * Its recommended to migrate to use fast type converter loading
     * by setting <tt>@Converter(generateLoader = true)</tt> on your custom
     * type converter classes.
     */
    private boolean loadTypeConverters = true;

    /**
     * Whether to load custom health checks by scanning classpath.
     */
    private boolean loadHealthChecks;

    /**
     * Directory to load additional configuration files that contains
     * configuration values that takes precedence over any other configuration.
     * This can be used to refer to files that may have secret configuration that
     * has been mounted on the file system for containers.
     *
     * You must use either file: or classpath: as prefix to load
     * from file system or classpath. Then you can specify a pattern to load
     * from sub directories and a name pattern such as file:/var/app/secret/*.properties
     */
    private String fileConfigurations;

    /**
     * Used for filtering routes matching the given pattern, which follows the following rules:
     *
     * - Match by route id
     * - Match by route input endpoint uri
     *
     * The matching is using exact match, by wildcard and regular expression.
     *
     * For example to only include routes which starts with foo in their route id's, use: include=foo&#42;
     * And to exclude routes which starts from JMS endpoints, use: exclude=jms:&#42;
     *
     * Multiple patterns can be separated by comma, for example to exclude both foo and bar routes, use: exclude=foo&#42;,bar&#42;
     *
     * Exclude takes precedence over include.
     */
    private String routeFilterIncludePattern;

    /**
     * Used for filtering routes matching the given pattern, which follows the following rules:
     *
     * - Match by route id
     * - Match by route input endpoint uri
     *
     * The matching is using exact match, by wildcard and regular expression.
     *
     * For example to only include routes which starts with foo in their route id's, use: include=foo&#42;
     * And to exclude routes which starts from JMS endpoints, use: exclude=jms:&#42;
     *
     * Multiple patterns can be separated by comma, for example to exclude both foo and bar routes, use: exclude=foo&#42;,bar&#42;
     *
     * Exclude takes precedence over include.
     */
    private String routeFilterExcludePattern;

    /**
     * Used for enabling automatic routes reloading. If enabled then Camel will watch for file changes in the given
     * reload directory, and trigger reloading routes if files are changed.
     */
    private boolean routesReloadEnabled;

    /**
     * Directory to scan (incl subdirectories) for route changes. Camel cannot scan the classpath, so this must be
     * configured to a file directory. Development with Maven as build tool, you can configure the directory to be
     * src/main/resources to scan for Camel routes in XML or YAML files.
     */
    private String routesReloadDirectory = "src/main/resources";

    /**
     * Whether the directory to scan should include sub directories.
     *
     * Depending on the number of sub directories, then this can cause the JVM to startup slower as Camel uses the JDK
     * file-watch service to scan for file changes.
     */
    private boolean routesReloadDirectoryRecursive;

    /**
     * Used for inclusive filtering of routes from directories.
     *
     * Typical used for specifying to accept routes in XML or YAML files.
     * Multiple patterns can be specified separated by comma.
     */
    private String routesReloadPattern = "camel/*";

    /**
     * When reloading routes should all existing routes be stopped and removed.
     *
     * By default, Camel will stop and remove all existing routes before reloading routes. This ensures that only the
     * reloaded routes will be active. If disabled then only routes with the same route id is updated, and any existing
     * routes are continued to run.
     */
    private boolean routesReloadRemoveAllRoutes = true;

    /**
     * Whether to restart max duration when routes are reloaded. For example if max duration is 60 seconds, and a route
     * is reloaded after 25 seconds, then this will restart the count and wait 60 seconds again.
     */
     private boolean routesReloadRestartDuration;

    /**
     * To specify for how long time in seconds to keep running the JVM before automatic terminating the JVM.
     * You can use this to run Spring Boot for a short while.
     */
    private int durationMaxSeconds;

    /**
     * To specify for how long time in seconds Camel can be idle before automatic terminating the JVM.
     * You can use this to run Spring Boot for a short while.
     */
    private int durationMaxIdleSeconds;

    /**
     * To specify how many messages to process by Camel before automatic terminating the JVM.
     * You can use this to run Spring Boot for a short while.
     */
    private int durationMaxMessages;

    /**
     * Controls whether the Camel application should shutdown the JVM, or stop all routes, when duration max is
     * triggered.
     */
    private String durationMaxAction = "shutdown";

    /**
     * Is used to limit the maximum length of the logging Camel message bodies. If the message body
     * is longer than the limit, the log message is clipped. Use -1 to have unlimited length.
     * Use for example 1000 to log at most 1000 characters.
     */
    private int logDebugMaxChars;

    /**
     * Sets whether stream caching is enabled or not.
     *
     * While stream types (like StreamSource, InputStream and Reader) are commonly used in messaging for performance
     * reasons, they also have an important drawback: they can only be read once. In order to be able to work with
     * message content multiple times, the stream needs to be cached.
     *
     * Streams are cached in memory only (by default).
     *
     * If streamCachingSpoolEnabled=true, then, for large stream messages (over 128 KB by default) will be cached in a
     * temporary file instead, and Camel will handle deleting the temporary file once the cached stream is no longer
     * necessary.
     *
     * Default is true.
     */
    private boolean streamCachingEnabled = true;

    /**
     * To enable stream caching spooling to disk. This means, for large stream messages (over 128 KB by default) will be cached in a
     * temporary file instead, and Camel will handle deleting the temporary file once the cached stream is no longer
     * necessary.
     *
     * Default is false.
     */
    private boolean streamCachingSpoolEnabled;

    /**
     * Sets the stream caching spool (temporary) directory to use for overflow and spooling to disk.
     *
     * If no spool directory has been explicit configured, then a temporary directory
     * is created in the java.io.tmpdir directory.
     */
    private String streamCachingSpoolDirectory;

    /**
     * Sets a stream caching cipher name to use when spooling to disk to write with encryption.
     * By default the data is not encrypted.
     */
    private String streamCachingSpoolCipher;

    /**
     * Stream caching threshold in bytes when overflow to disk is activated.
     * The default threshold is 128kb.
     * Use -1 to disable overflow to disk.
     */
    private long streamCachingSpoolThreshold;

    /**
     * Sets a percentage (1-99) of used heap memory threshold to activate stream caching spooling to disk.
     */
    private int streamCachingSpoolUsedHeapMemoryThreshold;

    /**
     * Sets what the upper bounds should be when streamCachingSpoolUsedHeapMemoryThreshold is in use.
     */
    private String streamCachingSpoolUsedHeapMemoryLimit;

    /**
     * Sets whether if just any of the org.apache.camel.spi.StreamCachingStrategy.SpoolRule rules
     * returns true then shouldSpoolCache(long) returns true, to allow spooling to disk.
     * If this option is false, then all the org.apache.camel.spi.StreamCachingStrategy.SpoolRule must
     * return true.
     *
     * The default value is false which means that all the rules must return true.
     */
    private boolean streamCachingAnySpoolRules;

    /**
     * Sets the stream caching buffer size to use when allocating in-memory buffers used for in-memory stream caches.
     *
     * The default size is 4096.
     */
    private int streamCachingBufferSize;

    /**
     * Whether to remove stream caching temporary directory when stopping.
     * This option is default true.
     */
    private boolean streamCachingRemoveSpoolDirectoryWhenStopping = true;

    /**
     * Sets whether stream caching statistics is enabled.
     */
    private boolean streamCachingStatisticsEnabled;

    /**
     * Sets whether type converter statistics is enabled.
     *
     * By default the type converter utilization statistics is disabled.
     * Notice: If enabled then there is a slight performance impact under very heavy load.
     */
    private boolean typeConverterStatisticsEnabled;

    /**
     * Sets whether debugging is enabled or not.
     *
     * Default is false.
     */
    private boolean debugging;

    /**
     * Sets whether backlog tracing is enabled or not.
     *
     * Default is false.
     */
    private boolean backlogTracing;

    /**
     * Sets whether tracing is enabled or not.
     *
     * Default is false.
     */
    private boolean tracing;

    /**
     * Whether to set tracing on standby. If on standby then the tracer is installed and made available. Then the tracer
     * can be enabled later at runtime via JMX or via Tracer.setEnabled(true).
     */
    private boolean tracingStandby;

    /**
     * Tracing pattern to match which node EIPs to trace.
     * For example to match all To EIP nodes, use to*.
     * The pattern matches by node and route id's
     * Multiple patterns can be separated by comma.
     */
    private String tracingPattern;

    /**
     * To use a custom tracing logging format.
     *
     * The default format (arrow, routeId, label) is: %-4.4s [%-12.12s] [%-33.33s]
     */
    private String tracingLoggingFormat;

    /**
     * Sets whether message history is enabled or not.
     *
     * Default is true.
     */
    private boolean messageHistory = true;

    /**
     * Whether to capture precise source location:line-number for all EIPs in Camel routes.
     *
     * Enabling this will impact parsing Java based routes (also Groovy, Kotlin, etc.) on startup as this uses JDK
     * StackTraceElement to calculate the location from the Camel route, which comes with a performance cost. This only
     * impact startup, not the performance of the routes at runtime.
     */
    private boolean sourceLocationEnabled;

    /**
     * Sets whether log mask is enabled or not.
     *
     * Default is false.
     */
    private boolean logMask;

    /**
     * Sets whether to log exhausted message body with message history.
     *
     * Default is false.
     */
    private boolean logExhaustedMessageBody;

    /**
     * Sets whether the object should automatically start when Camel starts.
     * Important: Currently only routes can be disabled, as CamelContext's are always started.
     * Note: When setting auto startup false on CamelContext then that takes precedence
     * and no routes is started. You would need to start CamelContext explicit using
     * the org.apache.camel.CamelContext.start() method, to start the context, and then
     * you would need to start the routes manually using Camelcontext.getRouteController().startRoute(String).
     *
     * Default is true to always start up.
     */
    private boolean autoStartup = true;

    /**
     * Sets whether to allow access to the original message from Camel's error handler,
     * or from org.apache.camel.spi.UnitOfWork.getOriginalInMessage().
     * Turning this off can optimize performance, as defensive copy of the original message is not needed.
     *
     * Default is false.
     */
    private boolean allowUseOriginalMessage;

    /**
     * Whether to use case sensitive or insensitive headers.
     *
     * Important: When using case sensitive (this is set to false).
     * Then the map is case sensitive which means headers such as content-type and Content-Type are
     * two different keys which can be a problem for some protocols such as HTTP based, which rely on case insensitive headers.
     * However case sensitive implementations can yield faster performance. Therefore use case sensitive implementation with care.
     *
     * Default is true.
     */
    private boolean caseInsensitiveHeaders = true;

    /**
     * Whether autowiring is enabled. This is used for automatic autowiring options (the option must be marked as
     * autowired) by looking up in the registry to find if there is a single instance of matching type, which then gets
     * configured on the component. This can be used for automatic configuring JDBC data sources, JMS connection
     * factories, AWS Clients, etc.
     *
     * Default is true.
     */
    private boolean autowiredEnabled = true;

    /**
     * Sets whether the inflight repository should allow browsing each inflight exchange.
     *
     * This is by default disabled as there is a very slight performance overhead when enabled.
     */
    private boolean inflightRepositoryBrowseEnabled;

    /**
     * Sets whether endpoint runtime statistics is enabled (gathers runtime usage of each incoming and outgoing endpoints).
     *
     * The default value is false.
     */
    private boolean endpointRuntimeStatisticsEnabled;

    /**
     * Sets whether context load statistics is enabled (something like the unix load average).
     * The statistics requires to have camel-management on the classpath as JMX is required.
     *
     * The default value is false.
     */
    private boolean loadStatisticsEnabled;

    /**
     * Allows for bridging the consumer to the Camel routing Error Handler, which mean any exceptions occurred while
     * the consumer is trying to pickup incoming messages, or the likes, will now be processed as a message and
     * handled by the routing Error Handler.
     * <p/>
     * By default the consumer will use the org.apache.camel.spi.ExceptionHandler to deal with exceptions,
     * that will be logged at WARN/ERROR level and ignored.
     *
     * The default value is false.
     */
    public boolean endpointBridgeErrorHandler;

    /**
     * Whether the endpoint should use basic property binding (Camel 2.x) or the newer property binding with additional capabilities.
     *
     * The default value is false.
     */
    public boolean endpointBasicPropertyBinding;

    /**
     * Whether the producer should be started lazy (on the first message). By starting lazy you can use this to allow CamelContext and routes to startup
     * in situations where a producer may otherwise fail during starting and cause the route to fail being started. By deferring this startup to be lazy then
     * the startup failure can be handled during routing messages via Camel's routing error handlers. Beware that when the first message is processed
     * then creating and starting the producer may take a little time and prolong the total processing time of the processing.
     *
     * The default value is false.
     */
    private boolean endpointLazyStartProducer;

    /**
     * Whether to enable using data type on Camel messages.
     *
     * Data type are automatic turned on if one or more routes has been explicit configured with input and output types.
     * Otherwise data type is default off.
     */
    private boolean useDataType;

    /**
     * Set whether breadcrumb is enabled.
     * The default value is false.
     */
    private boolean useBreadcrumb;

    /**
     * Can be used to turn off bean post processing.
     *
     * Be careful to turn this off, as this means that beans that use Camel annotations such as
     * org.apache.camel.EndpointInject, org.apache.camel.ProducerTemplate,
     * org.apache.camel.Produce, org.apache.camel.Consume etc will not be injected and in use.
     *
     * Turning this off should only be done if you are sure you do not use any of these Camel features.
     *
     * Not all runtimes allow turning this off (such as camel-blueprint or camel-cdi with XML).
     *
     * The default value is true (enabled).
     */
    private boolean beanPostProcessorEnabled = true;

    /**
     * Whether its allowed to add new routes after Camel has been started.
     * This is enabled by default.
     * Setting this to false allows Camel to do some internal optimizations to reduce memory footprint.
     * <p/>
     * This should only be done on a JVM with a single Camel application (microservice like camel-main, camel-quarkus, camel-spring-boot).
     * As this affects the entire JVM where Camel JARs are on the classpath.
     */
    private boolean allowAddingNewRoutes = true;

    /**
     * Sets the JMX statistics level
     * The level can be set to Extended to gather additional information
     *
     * The default value is Default.
     */
    private ManagementMBeansLevel jmxManagementMBeansLevel = ManagementMBeansLevel.Default;

    /**
     * Sets the JMX statistics level
     * The level can be set to Extended to gather additional information
     *
     * The default value is Default.
     */
    private ManagementStatisticsLevel jmxManagementStatisticsLevel = ManagementStatisticsLevel.Default;

    /**
     * The naming pattern for creating the CamelContext JMX management name.
     *
     * The default pattern is #name#
     */
    private String jmxManagementNamePattern = "#name#";

    /**
     * If dumping is enabled then Camel will during startup dump all loaded routes (incl rests and route templates)
     * represented as XML DSL into the log. This is intended for trouble shooting or to assist during development.
     *
     * Sensitive information that may be configured in the route endpoints could potentially be included in the dump
     * output and is therefore not recommended to be used for production usage.
     *
     * This requires to have camel-xml-jaxb on the classpath to be able to dump the routes as XML.
     */
    private boolean dumpRoutes;

    /**
     * Sets global options that can be referenced in the camel context
     *
     * Important: This has nothing to do with property placeholders, and is just a plain set of key/value pairs
     * which are used to configure global options on CamelContext, such as a maximum debug logging length etc.
     */
    private Map<String, String> globalOptions;

    /**
     * Whether to include timestamps for all emitted Camel Events. Enabling this allows to know fine-grained at what
     * time each event was emitted, which can be used for reporting to report exactly the time of the events. This is by
     * default false to avoid the overhead of including this information.
     */
    private boolean camelEventsTimestampEnabled;

    /**
     * To turn on MDC logging
     */
    private boolean useMdcLogging;

    /**
     * Sets the pattern used for determining which custom MDC keys to propagate during message routing when
     * the routing engine continues routing asynchronously for the given message. Setting this pattern to * will
     * propagate all custom keys. Or setting the pattern to foo*,bar* will propagate any keys starting with
     * either foo or bar.
     * Notice that a set of standard Camel MDC keys are always propagated which starts with camel. as key name.
     *
     * The match rules are applied in this order (case insensitive):
     *
     * 1. exact match, returns true
     * 2. wildcard match (pattern ends with a * and the name starts with the pattern), returns true
     * 3. regular expression match, returns true
     * 4. otherwise returns false
     */
    private String mdcLoggingKeysPattern;

    /**
     * Sets the thread name pattern used for creating the full thread name.
     *
     * The default pattern is: Camel (#camelId#) thread ##counter# - #name#
     *
     * Where #camelId# is the name of the CamelContext.
     * and #counter# is a unique incrementing counter.
     * and #name# is the regular thread name.
     *
     * You can also use #longName# which is the long thread name which can includes endpoint parameters etc.
     */
    private String threadNamePattern;

    /**
     * Sets whether bean introspection uses extended statistics.
     * The default is false.
     */
    private boolean beanIntrospectionExtendedStatistics;

    /**
     * Used for enabling context reloading. If enabled then Camel allow external systems such as
     * security vaults (AWS secrets manager, etc.) to trigger refreshing Camel by updating
     * property placeholders and reload all existing routes to take changes into effect.
     */
    private boolean contextReloadEnabled;

    /**
     * Sets the logging level used by bean introspection, logging activity of its usage.
     * The default is TRACE.
     */
    private LoggingLevel beanIntrospectionLoggingLevel;

    /**
     * Whether the routes collector is enabled or not.
     *
     * When enabled Camel will auto-discover routes (RouteBuilder instances from the registry and also load additional
     * routes from the file system).
     *
     * The routes collector is default enabled.
     */
    private boolean routesCollectorEnabled = true;

    /**
     * Used for inclusive filtering of routes from directories. The exclusive filtering takes precedence over inclusive
     * filtering. The pattern is using Ant-path style pattern.
     *
     * Multiple patterns can be specified separated by comma, as example, to include all the routes from a directory
     * whose name contains foo use: &#42;&#42;/*foo*.
     */
    private String routesIncludePattern = "classpath:camel/*,classpath:camel-template/*,classpath:camel-rest/*";

    /**
     * Used for exclusive filtering of routes from directories. The exclusive filtering takes precedence over inclusive
     * filtering. The pattern is using Ant-path style pattern.
     *
     * Multiple patterns can be specified separated by comma, as example, to exclude all the routes from a directory
     * whose name contains foo use: &#42;&#42;/*foo*.
     */
    private String routesExcludePattern;

    /**
     * Used for inclusive filtering RouteBuilder classes which are collected from the registry or via classpath
     * scanning. The exclusive filtering takes precedence over inclusive filtering. The pattern is using Ant-path style
     * pattern. Multiple patterns can be specified separated by comma.
     *
     * Multiple patterns can be specified separated by comma. For example to include all classes starting with Foo use:
     * &#42;&#42;/Foo* To include all routes form a specific package use: com/mycompany/foo/&#42; To include all routes
     * form a specific package and its sub-packages use double wildcards: com/mycompany/foo/&#42;&#42; And to include
     * all routes from two specific packages use: com/mycompany/foo/&#42;,com/mycompany/stuff/&#42;
     */
    private String javaRoutesIncludePattern;

    /**
     * Used for exclusive filtering RouteBuilder classes which are collected from the registry or via classpath
     * scanning. The exclusive filtering takes precedence over inclusive filtering. The pattern is using Ant-path style
     * pattern. Multiple patterns can be specified separated by comma.
     *
     * For example to exclude all classes starting with Bar use: &#42;&#42;/Bar&#42; To exclude all routes form a
     * specific package use: com/mycompany/bar/&#42; To exclude all routes form a specific package and its sub-packages
     * use double wildcards: com/mycompany/bar/&#42;&#42; And to exclude all routes from two specific packages use:
     * com/mycompany/bar/&#42;,com/mycompany/stuff/&#42;
     */
    private String javaRoutesExcludePattern;

    /**
     * Sets the logging level used for logging route activity (such as starting and stopping routes). The default
     * logging level is DEBUG.
     */
    @Deprecated
    private LoggingLevel routeControllerLoggingLevel;

    /**
     * To enable using supervising route controller which allows Camel to startup
     * and then the controller takes care of starting the routes in a safe manner.
     *
     * This can be used when you want to startup Camel despite a route may otherwise
     * fail fast during startup and cause Camel to fail to startup as well. By delegating
     * the route startup to the supervising route controller then it manages the startup
     * using a background thread. The controller allows to be configured with various
     * settings to attempt to restart failing routes.
     */
    boolean routeControllerSuperviseEnabled;

    /**
     * The number of threads used by the route controller scheduled thread pool that are used for restarting
     * routes. The pool uses 1 thread by default, but you can increase this to allow the controller
     * to concurrently attempt to restart multiple routes in case more than one route has problems
     * starting.
     */
    int routeControllerThreadPoolSize = 1;

    /**
     * Initial delay in milli seconds before the route controller starts, after
     * CamelContext has been started.
     */
    long routeControllerInitialDelay;

    /**
     * Backoff delay in millis when restarting a route that failed to startup.
     */
    long routeControllerBackOffDelay = 2000;

    /**
     * Backoff maximum delay in millis when restarting a route that failed to startup.
     */
    long routeControllerBackOffMaxDelay;

    /**
     * Backoff maximum elapsed time in millis, after which the backoff should be considered
     * exhausted and no more attempts should be made.
     */
    long routeControllerBackOffMaxElapsedTime;

    /**
     * Backoff maximum number of attempts to restart a route that failed to startup.
     * When this threshold has been exceeded then the controller will give up
     * attempting to restart the route, and the route will remain as stopped.
     */
    long routeControllerBackOffMaxAttempts;

    /**
     * Backoff multiplier to use for exponential backoff. This is used to extend the delay
     * between restart attempts.
     */
    double routeControllerBackOffMultiplier = 1.0;

    /**
     * Pattern for filtering routes to be excluded as supervised.
     *
     * The pattern is matching on route id, and endpoint uri for the route.
     * Multiple patterns can be separated by comma.
     *
     * For example to exclude all JMS routes, you can say <tt>jms:*</tt>.
     * And to exclude routes with specific route ids <tt>mySpecialRoute,myOtherSpecialRoute</tt>.
     * The pattern supports wildcards and uses the matcher from
     * org.apache.camel.support.PatternHelper#matchPattern.
     */
    String routeControllerIncludeRoutes;

    /**
     * Pattern for filtering routes to be included as supervised.
     *
     * The pattern is matching on route id, and endpoint uri for the route.
     * Multiple patterns can be separated by comma.
     *
     * For example to include all kafka routes, you can say <tt>kafka:*</tt>.
     * And to include routes with specific route ids <tt>myRoute,myOtherRoute</tt>.
     * The pattern supports wildcards and uses the matcher from
     * org.apache.camel.support.PatternHelper#matchPattern.
     */
    String routeControllerExcludeRoutes;

    /**
     * Whether to mark the route as unhealthy (down) when all restarting attempts (backoff) have failed and the route is
     * not successfully started and the route manager is giving up.
     *
     * Setting this to true allows health checks to know about this and can report the Camel application as DOWN.
     *
     * The default is false.
     */
    private boolean routeControllerUnhealthyOnExhausted;

    /**
     * Experimental: Configure the context to be lightweight.
     * This will trigger some optimizations and memory reduction options.
     * Lightweight context has some limitations.
     * At this moment, dynamic endpoint destinations are not supported.
     */
    private boolean lightweight;

    /**
     * Controls whether to pool (reuse) exchanges or create new exchanges (prototype). Using pooled will reduce JVM
     * garbage collection overhead by avoiding to re-create Exchange instances per message each consumer receives. The
     * default is prototype mode.
     */
    private String exchangeFactory = "default";

    /**
     * The capacity the pool (for each consumer) uses for storing exchanges. The default capacity is 100.
     */
    private int exchangeFactoryCapacity = 100;

    /**
     * Configures whether statistics is enabled on exchange factory.
     */
    private boolean exchangeFactoryStatisticsEnabled;

    /**
     * To use startup recorder for capturing execution time during starting Camel. The recorder can be one of: false (or
     * off), logging, java-flight-recorder (or jfr).
     */
    private String startupRecorder;

    /**
     * To filter our sub steps at a maximum depth.
     *
     * Use -1 for no maximum. Use 0 for no sub steps. Use 1 for max 1 sub step, and so forth.
     *
     * The default is -1.
     */
    private int startupRecorderMaxDepth = -1;

    /**
     * To enable Java Flight Recorder to start a recording and automatic dump the recording to disk after startup is
     * complete.
     *
     * This requires that camel-jfr is on the classpath, and to enable this option.
     */
    private boolean startupRecorderRecording;

    /**
     * To use a specific Java Flight Recorder profile configuration, such as default or profile.
     *
     * The default is default.
     */
    private String startupRecorderProfile = "default";

    /**
     * How long time to run the startup recorder.
     *
     * Use 0 (default) to keep the recorder running until the JVM is exited.
     * Use -1 to stop the recorder right after Camel has been started (to only focus on potential Camel startup performance bottlenecks)
     * Use a positive value to keep recording for N seconds.
     *
     * When the recorder is stopped then the recording is auto saved to disk
     * (note: save to disk can be disabled by setting startupRecorderDir to false)
     */
    private long startupRecorderDuration;

    /**
     * Directory to store the recording. By default the user home directory will be used. Use false to turn off saving
     * recording to disk.
     */
    private String startupRecorderDir;

    // Getters & setters
    // -----------------

    public boolean isMainRunController() {
        return mainRunController;
    }

    public void setMainRunController(boolean mainRunController) {
        this.mainRunController = mainRunController;
    }

    public boolean isIncludeNonSingletons() {
        return includeNonSingletons;
    }

    public void setIncludeNonSingletons(boolean includeNonSingletons) {
        this.includeNonSingletons = includeNonSingletons;
    }

    public boolean isWarnOnEarlyShutdown() {
        return warnOnEarlyShutdown;
    }

    public void setWarnOnEarlyShutdown(boolean warnOnEarlyShutdown) {
        this.warnOnEarlyShutdown = warnOnEarlyShutdown;
    }

}
