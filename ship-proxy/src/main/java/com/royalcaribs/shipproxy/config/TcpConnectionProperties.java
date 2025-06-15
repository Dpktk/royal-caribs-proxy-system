package com.royalcaribs.shipproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for TCP connection to shore.
 * Maps to tcp.connection.* properties in app.props
 */

@Component
@ConfigurationProperties(prefix = "ship.proxy")
public class TcpConnectionProperties {

	// Proxy server settings
    private int proxyPort = 8080;
    
    // Shore connection settings
    private String shoreHost;
    private int shorePort;
    private int connectionTimeoutSeconds;
    private int responseTimeoutSeconds;
    private int connectionRetryDelaySeconds;
    private int connectionManagerSleepMs;
    private int queuePollTimeoutSeconds;
    private boolean keepAlive;
    private boolean tcpNoDelay;
    
    // Getters and Setters
    public int getProxyPort() {
        return proxyPort;
    }
    
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
    
    public String getShoreHost() {
        return shoreHost;
    }
    
    public void setShoreHost(String shoreHost) {
        this.shoreHost = shoreHost;
    }
    
    public int getShorePort() {
        return shorePort;
    }
    
    public void setShorePort(int shorePort) {
        this.shorePort = shorePort;
    }
    
    public int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }
    
    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }
    
    public int getResponseTimeoutSeconds() {
        return responseTimeoutSeconds;
    }
    
    public void setResponseTimeoutSeconds(int responseTimeoutSeconds) {
        this.responseTimeoutSeconds = responseTimeoutSeconds;
    }
    
    public int getConnectionRetryDelaySeconds() {
        return connectionRetryDelaySeconds;
    }
    
    public void setConnectionRetryDelaySeconds(int connectionRetryDelaySeconds) {
        this.connectionRetryDelaySeconds = connectionRetryDelaySeconds;
    }
    
    public int getConnectionManagerSleepMs() {
        return connectionManagerSleepMs;
    }
    
    public void setConnectionManagerSleepMs(int connectionManagerSleepMs) {
        this.connectionManagerSleepMs = connectionManagerSleepMs;
    }
    
    public int getQueuePollTimeoutSeconds() {
        return queuePollTimeoutSeconds;
    }
    
    public void setQueuePollTimeoutSeconds(int queuePollTimeoutSeconds) {
        this.queuePollTimeoutSeconds = queuePollTimeoutSeconds;
    }
    
    public boolean isKeepAlive() {
        return keepAlive;
    }
    
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
    
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }
    
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }
}