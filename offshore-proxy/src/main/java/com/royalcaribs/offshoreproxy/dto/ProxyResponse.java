package com.royalcaribs.offshoreproxy.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents an HTTP response received from the internet and sent back through
 * the TCP tunnel. Sends JSON from shore to ship.
 */
public class ProxyResponse {
	private String requestId;
	private int statusCode;
	private Map<String, String> headers;
	private byte[] body;
	private boolean success;
	private String errorMessage;

	public ProxyResponse() {
	}

	public ProxyResponse(String requestId, int statusCode, Map<String, String> headers, byte[] body, boolean success) {
		this.requestId = requestId;
		this.statusCode = statusCode;
		this.headers = headers;
		this.body = body;
		this.success = success;
		this.errorMessage = null;
	}

	public ProxyResponse(String requestId, int statusCode, Map<String, String> headers, byte[] body, boolean success,
			String errorMessage) {
		this.requestId = requestId;
		this.statusCode = statusCode;
		this.headers = headers;
		this.body = body;
		this.success = success;
		this.errorMessage = errorMessage;
	}

	public static ProxyResponse success(String requestId, int statusCode, Map<String, String> headers, byte[] body) {
		return new ProxyResponse(requestId, statusCode, headers, body, true, null);
	}

	public static ProxyResponse error(String requestId, int statusCode, String errorMessage) {
		return new ProxyResponse(requestId, statusCode, null, errorMessage.getBytes(), false, errorMessage);
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return "ProxyResponse{requestId='" + requestId + "', statusCode=" + statusCode + ", success=" + success + "}";
	}
}