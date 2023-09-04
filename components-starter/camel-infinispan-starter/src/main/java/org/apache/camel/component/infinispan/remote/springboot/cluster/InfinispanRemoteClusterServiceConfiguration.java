package org.apache.camel.component.infinispan.remote.springboot.cluster;

import org.apache.camel.component.infinispan.remote.cluster.InfinispanRemoteClusterConfiguration;
import org.infinispan.client.hotrod.RemoteCacheManager;
import  org.infinispan.client.hotrod.configuration.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "camel.cluster.infinispan.remote")
public class InfinispanRemoteClusterServiceConfiguration extends InfinispanRemoteClusterConfiguration {
    /**
     * Sets if the zookeeper cluster service should be enabled or not, default is false.
     */
    private boolean enabled = false;

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
    // Fields copied from InifnispanRemoteConfiguration to add
    // javadoc which is used by spring-boot-configuration-processor
    // to generate descritpions for inherited properties
    // ------------------------------------------

    public String getHosts() {
        return getConfiguration().getHosts();
    }

    public void setHosts(String hosts) {
        getConfiguration().setHosts(hosts);
    }

    public boolean isSecure() {
        return getConfiguration().isSecure();
    }

    public void setSecure(boolean secure) {
        getConfiguration().setSecure(secure);
    }

    public String getUsername() {
        return getConfiguration().getUsername();
    }

    public void setUsername(String username) {
        getConfiguration().setUsername(username);
    }

    public String getPassword() {
        return getConfiguration().getPassword();
    }

    public void setPassword(String password) {
        getConfiguration().setPassword(password);
    }

    public String getSaslMechanism() {
        return getConfiguration().getSaslMechanism();
    }

    public void setSaslMechanism(String saslMechanism) {
        getConfiguration().setSaslMechanism(saslMechanism);
    }

    public String getSecurityRealm() {
        return getConfiguration().getSecurityRealm();
    }

    public void setSecurityRealm(String securityRealm) {
        getConfiguration().setSecurityRealm(securityRealm);
    }

    public String getSecurityServerName() {
        return getConfiguration().getSecurityServerName();
    }

    public void setSecurityServerName(String securityServerName) {
        getConfiguration().setSecurityServerName(securityServerName);
    }

    public Map<String, String> getConfigurationProperties() {
        return getConfiguration().getConfigurationProperties();
    }

    public void setConfigurationProperties(Map<String, String> configurationProperties) {
        getConfiguration().setConfigurationProperties(configurationProperties);
    }

    public void addConfigurationProperty(String key, String value) {
        getConfiguration().addConfigurationProperty(key, value);
    }

    public RemoteCacheManager getCacheContainer() {
        return getConfiguration().getCacheContainer();
    }

    public void setCacheContainer(RemoteCacheManager cacheContainer) {
        getConfiguration().setCacheContainer(cacheContainer);
    }

    public Configuration getCacheContainerConfiguration() {
        return getConfiguration().getCacheContainerConfiguration();
    }

    public void setCacheContainerConfiguration(Configuration cacheContainerConfiguration) {
        getConfiguration().setCacheContainerConfiguration(cacheContainerConfiguration);
    }
}
