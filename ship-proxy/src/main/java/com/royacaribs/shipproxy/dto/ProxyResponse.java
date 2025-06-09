package com.royacaribs.shipproxy.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents an HTTP response received from the intrnet and sent back through
 * the TCP tunnel. Sending JSON from shore to ship.
 */
public class ProxyResponse {

	private final String requestId;
	private final int statusCode;
	private final String statusMessage;
	private final Map<String, String> headers;
	private final byte[] body;
	private final long timestamp;
	private final boolean success;
	private final String errorMessage;

	@JsonCreator
	public ProxyResponse(@JsonProperty("requestId") String requestId, @JsonProperty("statusCode") int statusCode,
			@JsonProperty("statusMessage") String statusMessage, @JsonProperty("headers") Map<String, String> headers,
			@JsonProperty("body") byte[] body, @JsonProperty("timestamp") long timestamp,
			@JsonProperty("success") boolean success, @JsonProperty("errorMessage") String errorMessage) {
		this.requestId = requestId;
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.headers = headers;
		this.body = body;
		this.timestamp = timestamp;
		this.success = success;
		this.errorMessage = errorMessage;
	}

	// Constructor for successful responses
	public ProxyResponse(String requestId, int statusCode, String statusMessage, Map<String, String> headers,
			byte[] body) {
		this(requestId, statusCode, statusMessage, headers, body, System.currentTimeMillis(), true, null);
	}

	// Constructor for error responses
	public ProxyResponse(String requestId, String errorMessage) {
		this(requestId, 500, "Internal Server Error", Map.of(), new byte[0], System.currentTimeMillis(), false,
				errorMessage);
	}

	public String getRequestId() {
		return requestId;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public byte[] getBody() {
		return body;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String toString() {
		return String.format("ProxyResponse{requestId='%s', statusCode=%d, success=%s, timestamp=%d}", requestId,
				statusCode, success, timestamp);
	}
}