package com.royalcaribs.shipproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for TCP connection to shore.
 * Maps to tcp.connection.* properties in app.props
 */

@Component
@ConfigurationProperties(prefix = "tcp.connection")
public class TcpConnectionProperties {

	private String shoreHost = "localhost";
	private int shorePort = 9090;
	private int responseTimeoutSeconds = 30;
	private int connectionRetryDelaySeconds = 5;
	private boolean keepAlive = true;
	private boolean tcpNoDelay = true;
	private int queuePollTimeoutSeconds = 1;
	private int connectionManagerSleepMs = 100;

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

	public int getQueuePollTimeoutSeconds() {
		return queuePollTimeoutSeconds;
	}

	public void setQueuePollTimeoutSeconds(int queuePollTimeoutSeconds) {
		this.queuePollTimeoutSeconds = queuePollTimeoutSeconds;
	}

	public int getConnectionManagerSleepMs() {
		return connectionManagerSleepMs;
	}

	public void setConnectionManagerSleepMs(int connectionManagerSleepMs) {
		this.connectionManagerSleepMs = connectionManagerSleepMs;
	}
}
