package com.royacaribs.shipproxy.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an HTTP request that will be sent through the TCP tunnel.
 * Sending JSON, from ship to shore.
 */
public class ProxyRequest {
    
    private final String requestId;
    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final byte[] body;
    private final long timestamp;
    
    @JsonCreator
    public ProxyRequest(
            @JsonProperty("requestId") String requestId,
            @JsonProperty("method") String method,
            @JsonProperty("url") String url,
            @JsonProperty("headers") Map<String, String> headers,
            @JsonProperty("body") byte[] body,
            @JsonProperty("timestamp") long timestamp) {
        this.requestId = requestId != null ? requestId : UUID.randomUUID().toString();
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
        this.timestamp = timestamp;
    }
    
    // Constructor for creating new requests
    public ProxyRequest(String method, String url, Map<String, String> headers, byte[] body) {
        this(UUID.randomUUID().toString(), method, url, headers, body, System.currentTimeMillis());
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("ProxyRequest{id='%s', method='%s', url='%s', timestamp=%d}", 
                requestId, method, url, timestamp);
    }
}