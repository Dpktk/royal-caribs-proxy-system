package com.royalcaribs.shipproxy.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.royalcaribs.shipproxy.config.TcpConnectionProperties;
import com.royalcaribs.shipproxy.dto.ProxyRequest;
import com.royalcaribs.shipproxy.dto.ProxyResponse;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.TimeoutException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ShipProxyService {
	private static final Logger logger = LoggerFactory.getLogger(ShipProxyService.class);

	private final TcpConnectionProperties properties;
	private final ObjectMapper objectMapper;

	// LittleProxy server
	private HttpProxyServer proxyServer;

	// Sequential processing
	private final LinkedBlockingQueue<RequestTask> requestQueue = new LinkedBlockingQueue<>();
	private final AtomicBoolean isRunning = new AtomicBoolean(false);
	private Thread sequentialProcessor;

	// Shore connection
	private volatile Socket shoreSocket;
	private volatile PrintWriter writer;
	private volatile BufferedReader reader;
	private final Object connectionLock = new Object();

	public ShipProxyService(TcpConnectionProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}
	
    private final AtomicBoolean started = new AtomicBoolean(false);

	//Start the proxy service
	@PostConstruct
	public void start() {
		 if (!started.compareAndSet(false, true)) {
	            logger.info("Ship Proxy Service already started, skipping...");
	            return;
	        }
		 logger.info("Starting Ship Proxy Service on port {} (Profile: {})", 
	               properties.getProxyPort(), getCurrentProfile());
		 
		 logger.info(" Shore target: {}:{}", properties.getShoreHost(), properties.getShorePort());

		isRunning.set(true);
		connectToShore();
		startSequentialProcessor();

//		proxyServer = DefaultHttpProxyServer.bootstrap().withPort(properties.getProxyPort())
//				.withFiltersSource(new SequentialHttpFiltersSource())
//				.start();
		
		try {
		    logger.info("Starting LittleProxy with minimal config...");
		    
		    proxyServer = DefaultHttpProxyServer.bootstrap()
		        .withPort(properties.getProxyPort())
		        .withAddress(new InetSocketAddress("0.0.0.0", properties.getProxyPort()))  // Force bind to all interfaces
		        .withFiltersSource(new SequentialHttpFiltersSource())
		        .start();
		    
		    logger.info(" Minimal LittleProxy started on: {}", proxyServer.getListenAddress());
		    
		} catch (Exception e) {
		    logger.error("Even minimal proxy failed: {}", e.getMessage(), e);
		    throw new RuntimeException("Cannot start proxy server", e);
		}
		logger.info("Ship Proxy started on port {}", proxyServer.getListenAddress().getPort());
	}
	
	// Helper method to get current profile
	private String getCurrentProfile() {
	    return System.getProperty("spring.profiles.active", "local");
	}

	//Stop the proxy service
	@PreDestroy
	public void stop() {
		logger.info("Stopping Ship Proxy Service...");
		isRunning.set(false);

		if (proxyServer != null) {
			proxyServer.stop();
		}
		if (sequentialProcessor != null) {
			sequentialProcessor.interrupt();
		}
		closeShoreConnection();

		logger.info("Ship Proxy Service stopped");
	}

	//Sequential HTTP Filters Source
	private class SequentialHttpFiltersSource extends HttpFiltersSourceAdapter {
		@Override
		public HttpFilters filterRequest(HttpRequest originalRequest) {
			return new SequentialHttpFilters(originalRequest);
		}
	}

	//process requests one by one
	private class SequentialHttpFilters extends HttpFiltersAdapter {

		public SequentialHttpFilters(HttpRequest originalRequest) {
			super(originalRequest);
		}

		public HttpResponse clientToProxyRequest(HttpObject httpObject) {
		    if (httpObject instanceof HttpRequest) {
		        HttpRequest request = (HttpRequest) httpObject;
		        String requestId = UUID.randomUUID().toString();

		        logger.info("Intercepted request {}: {} {}", requestId, request.method(), request.uri());

		        try {
		            CompletableFuture<ProxyResponse> future = new CompletableFuture<>();
		            
		            byte[] requestBody = new byte[0];
		            if(httpObject instanceof FullHttpRequest) {
		            	FullHttpRequest fullRequest = (FullHttpRequest) httpObject;
		            	
		            	if(fullRequest.content().readableBytes() > 0) {
		            		requestBody = new byte[fullRequest.content().readableBytes()];
		            		fullRequest.content().readBytes(requestBody);
		            		fullRequest.content().resetReaderIndex();
		            	}
		            }
		            
		            RequestTask task = new RequestTask(request, requestId, future, httpObject, requestBody);

		            boolean queued = requestQueue.offer(task, 100, TimeUnit.MILLISECONDS);
		            if (!queued) {
		                logger.warn("Request {} queue full, rejecting", requestId);
		                return createErrorResponse("Proxy overloaded", HttpResponseStatus.SERVICE_UNAVAILABLE);
		            }

		            logger.debug("Queued request {} for sequential processing", requestId);
		            
		            // CRITICAL FIX: Wait for the response from sequential processor
		            try {
		                ProxyResponse proxyResponse = future.get(properties.getResponseTimeoutSeconds(), TimeUnit.SECONDS);
		                logger.info("Received response for request {}: {}", requestId, proxyResponse.getStatusCode());
		                
		                // Convert ProxyResponse to HttpResponse
		                return convertToHttpResponse(proxyResponse);
		                
		            } catch (TimeoutException e) {
		                logger.error("Timeout waiting for response to request {}", requestId);
		                return createErrorResponse("Request timeout", HttpResponseStatus.GATEWAY_TIMEOUT);
		            } catch (InterruptedException e) {
		                Thread.currentThread().interrupt();
		                logger.error("Interrupted waiting for response to request {}", requestId);
		                return createErrorResponse("Request interrupted", HttpResponseStatus.INTERNAL_SERVER_ERROR);
		            } catch (ExecutionException e) {
		                logger.error("Error executing request {}: {}", requestId, e.getCause().getMessage());
		                return createErrorResponse("Execution error: " + e.getCause().getMessage(), 
		                                         HttpResponseStatus.INTERNAL_SERVER_ERROR);
		            }

		        } catch (Exception e) {
		            logger.error("Error processing request {}: {}", requestId, e.getMessage());
		            return createErrorResponse("Processing error: " + e.getMessage(),
		                                     HttpResponseStatus.INTERNAL_SERVER_ERROR);
		        }
		    }
		    return null;
		}

		/**
		 * Convert ProxyResponse to LittleProxy HttpResponse
		 */
		private HttpResponse convertToHttpResponse(ProxyResponse proxyResponse) {
		    try {
		        HttpResponseStatus status = HttpResponseStatus.valueOf(proxyResponse.getStatusCode());
		        
		        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
		            HttpVersion.HTTP_1_1, 
		            status,
		            Unpooled.wrappedBuffer(proxyResponse.getBody())
		        );
		        
		        // Set headers from ProxyResponse
		        if (proxyResponse.getHeaders() != null) {
		            proxyResponse.getHeaders().forEach((key, value) -> {
		                try {
		                    response.headers().set(key, value);
		                } catch (Exception e) {
		                    logger.debug("Skipping invalid header: {}={}", key, value);
		                }
		            });
		        }
		        
		        // Ensure Content-Length is set
		        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, proxyResponse.getBody().length);
		        
		        logger.debug("Converted ProxyResponse to HttpResponse: {} bytes, status {}", 
		                    proxyResponse.getBody().length, proxyResponse.getStatusCode());
		        
		        return response;
		        
		    } catch (Exception e) {
		        logger.error("Error converting ProxyResponse to HttpResponse: {}", e.getMessage());
		        return createErrorResponse("Response conversion error", HttpResponseStatus.INTERNAL_SERVER_ERROR);
		    }
		}

		//Create error HTTP response
		private HttpResponse createErrorResponse(String message, HttpResponseStatus status) {
			byte[] errorBytes = message.getBytes();
			DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
					Unpooled.wrappedBuffer(errorBytes));
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
			response.headers().set(HttpHeaderNames.CONTENT_LENGTH, errorBytes.length);
			return response;
		}
	}

	//Start sequential processor thread
	private void startSequentialProcessor() {
		sequentialProcessor = new Thread(() -> {
			logger.info(" Sequential request processor started");

			while (isRunning.get() || !requestQueue.isEmpty()) {
				try {
					RequestTask task = requestQueue.poll(1, TimeUnit.SECONDS);
					if (task != null) {
						processRequestSequentially(task);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					logger.error("Error in sequential processor: {}", e.getMessage());
				}
			}

			logger.info("Sequential request processor stopped");
		}, "sequential-processor");

		sequentialProcessor.setDaemon(false);
		sequentialProcessor.start();
	}

	//Process single request sequentially
	private void processRequestSequentially(RequestTask task) {
		try {
			
//			if (true) {
//			    logger.info("❗ Forcing exception for request {} to test client impact", task.requestId);
//			    throw new RuntimeException("Simulated failure in processRequestSequentially()");
//			}
			logger.info("Processing request {} sequentially", task.requestId);

			// Extract headers
			Map<String, String> headers = new HashMap<>();
			task.httpRequest.headers().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));

			// Create proxy request
			ProxyRequest proxyRequest = new ProxyRequest(task.requestId, task.httpRequest.method().name(),
					task.httpRequest.uri(), headers, task.requestBody, System.currentTimeMillis());

			// Send to shore via TCP
			synchronized (connectionLock) {
				ensureShoreConnection();

				String requestJson = objectMapper.writeValueAsString(proxyRequest);
				writer.println(requestJson);
				writer.flush();

				String responseJson = reader.readLine();
				if (responseJson == null) {
					throw new IOException("Shore connection closed");
				}

				ProxyResponse response = objectMapper.readValue(responseJson, ProxyResponse.class);
				task.future.complete(response);

				logger.info("Received response for request {} with status {}", task.requestId,
						response.getStatusCode());
			}

		} catch (Exception e) {
			logger.error("Failed to process request {}: {}", task.requestId, e.getMessage());
			
			ProxyResponse errorResponse = ProxyResponse.error(task.requestId, 500, 
					"TCP communication error: " + e.getMessage());
			task.future.complete(errorResponse);

			if (e instanceof IOException) {
				reconnectToShore();
			}
		}
	}

	//Connect to shore proxy
	private void connectToShore() {
		try {
			synchronized (connectionLock) {
				shoreSocket = new Socket();
				shoreSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(properties.getResponseTimeoutSeconds()));
				shoreSocket.setKeepAlive(properties.isKeepAlive());
				shoreSocket.setTcpNoDelay(properties.isTcpNoDelay());

				shoreSocket.connect(
						new java.net.InetSocketAddress(properties.getShoreHost(), properties.getShorePort()),
						(int) TimeUnit.SECONDS.toMillis(properties.getConnectionTimeoutSeconds()));

				writer = new PrintWriter(new OutputStreamWriter(shoreSocket.getOutputStream()), true);
				reader = new BufferedReader(new InputStreamReader(shoreSocket.getInputStream()));

				logger.info(" Connected to shore proxy at {}:{}", properties.getShoreHost(),
						properties.getShorePort());
			}
		} catch (Exception e) {
			logger.error(" Failed to connect to shore: {}", e.getMessage());
			closeShoreConnection();
		}
	}

	//Ensure shore connection
	private void ensureShoreConnection() throws IOException {
		if (shoreSocket == null || shoreSocket.isClosed() || !shoreSocket.isConnected()) {
			reconnectToShore();
		}
	}

	//Reconnect to shore
	private void reconnectToShore() {
		logger.warn(" Reconnecting to shore proxy...");
		closeShoreConnection();

		for (int attempt = 1; attempt <= 3; attempt++) {
			try {
				Thread.sleep(properties.getConnectionRetryDelaySeconds() * 1000L);
				connectToShore();
				if (shoreSocket != null && shoreSocket.isConnected()) {
					logger.info("Reconnected to shore proxy successfully");
					return;
				}
			} catch (Exception e) {
				logger.warn("Reconnection attempt {} failed: {}", attempt, e.getMessage());
			}
		}

		logger.error("Failed to reconnect to shore proxy after 3 attempts");
	}

	//Close shore connection
	private void closeShoreConnection() {
		synchronized (connectionLock) {
			try {
				if (writer != null) {
					writer.close();
					writer = null;
				}
				if (reader != null) {
					reader.close();
					reader = null;
				}
				if (shoreSocket != null && !shoreSocket.isClosed()) {
					shoreSocket.close();
					shoreSocket = null;
				}
			} catch (Exception e) {
				logger.warn("Error closing shore connection: {}", e.getMessage());
			}
		}
	}

	//Request task for sequential processing
	private static class RequestTask {
		final HttpRequest httpRequest;
		final String requestId;
		final CompletableFuture<ProxyResponse> future;
		final HttpObject httpObject;
		  final byte[] requestBody; 

		RequestTask(HttpRequest httpRequest, String requestId, CompletableFuture<ProxyResponse> future,
				HttpObject httpObject, byte[] requestBody) {
			this.httpRequest = httpRequest;
			this.requestId = requestId;
			this.future = future;
			this.httpObject = httpObject;
			this.requestBody = requestBody;
		}
	}
}