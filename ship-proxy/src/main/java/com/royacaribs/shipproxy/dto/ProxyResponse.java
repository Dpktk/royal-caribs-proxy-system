package com.royacaribs.shipproxy.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an HTTP response received from the intrnet and sent back through
 * the TCP tunnel. Sending JSON from shore to ship.
 */
public class ProxyResponse {
    
    private final String requestId;
    private final boolean success;
    private final int statusCode;
    private final Map<String, String> headers;
    private final byte[] body;
    private final String errorMessage;
    
    @JsonCreator
    public ProxyResponse(
            @JsonProperty("requestId") String requestId,
            @JsonProperty("success") boolean success,
            @JsonProperty("statusCode") int statusCode,
            @JsonProperty("headers") Map<String, String> headers,
            @JsonProperty("body") byte[] body,
            @JsonProperty("errorMessage") String errorMessage) {
        this.requestId = requestId;
        this.success = success;
        this.statusCode = statusCode;
        this.headers = headers != null ? headers : new HashMap<>();
        this.body = body != null ? body : new byte[0];
        this.errorMessage = errorMessage;
    }
    
    // Getters
    public String getRequestId() {
        return requestId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public byte[] getBody() {
        return body;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    // Static factory methods for convenience
    
    /**
     * Create a successful response
     */
    public static ProxyResponse success(String requestId, int statusCode, 
                                      Map<String, String> headers, byte[] body) {
        return new ProxyResponse(requestId, true, statusCode, headers, body, null);
    }
    
    /**
     * Create an error response
     */
    public static ProxyResponse error(int statusCode, String errorMessage) {
        return new ProxyResponse(null, false, statusCode, new HashMap<>(), new byte[0], errorMessage);
    }
    
    /**
     * Create an error response with request ID
     */
    public static ProxyResponse error(String requestId, int statusCode, String errorMessage) {
        return new ProxyResponse(requestId, false, statusCode, new HashMap<>(), new byte[0], errorMessage);
    }
    
    /**
     * Create a simple success response
     */
    public static ProxyResponse success(String requestId, int statusCode, byte[] body) {
        return new ProxyResponse(requestId, true, statusCode, new HashMap<>(), body, null);
    }
    
    @Override
    public String toString() {
        return "ProxyResponse{" +
                "requestId='" + requestId + '\'' +
                ", success=" + success +
                ", statusCode=" + statusCode +
                ", headers=" + headers.size() + " headers" +
                ", bodySize=" + body.length +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}