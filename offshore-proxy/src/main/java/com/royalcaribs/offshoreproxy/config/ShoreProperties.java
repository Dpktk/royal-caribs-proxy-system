package com.royalcaribs.offshoreproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shore")
public class ShoreProperties {
	
	private int tcpPort = 9090;
	private int httpClientTimeoutSeconds = 30;
	private int maxConcurrentRequests = 100;
	private boolean enableMetrics = true;

	//Getters & Setters
	public int getTcpPort() {
		return tcpPort;
	}

	public void setTcpPort(int tcpPort) {
		this.tcpPort = tcpPort;
	}

	public int getHttpClientTimeoutSeconds() {
		return httpClientTimeoutSeconds;
	}

	public void setHttpClientTimeoutSeconds(int httpClientTimeoutSeconds) {
		this.httpClientTimeoutSeconds = httpClientTimeoutSeconds;
	}

	public int getMaxConcurrentRequests() {
		return maxConcurrentRequests;
	}

	public void setMaxConcurrentRequests(int maxConcurrentRequests) {
		this.maxConcurrentRequests = maxConcurrentRequests;
	}

	public boolean isEnableMetrics() {
		return enableMetrics;
	}

	public void setEnableMetrics(boolean enableMetrics) {
		this.enableMetrics = enableMetrics;
	}
}
