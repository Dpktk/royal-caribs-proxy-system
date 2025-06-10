//package com.royalcaribs.shipproxy.service;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.royacaribs.shipproxy.dto.ProxyRequest;
//import com.royacaribs.shipproxy.dto.ProxyResponse;
//import com.royalcaribs.shipproxy.config.TcpConnectionProperties;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//
//import java.net.Socket;
//import java.net.SocketException;
//import java.io.*;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//
//@Service
//public class TcpConnectionService {
//
//	 private static final Logger logger = LoggerFactory.getLogger(TcpConnectionService.class);
//	    
//	    private final TcpConnectionProperties config;
//	    private final ObjectMapper objectMapper = new ObjectMapper();
//	    private final BlockingQueue<ProxyRequest> requestQueue = new LinkedBlockingQueue<>();
//	    private final ConcurrentHashMap<String, CompletableFuture<ProxyResponse>> pendingRequests = new ConcurrentHashMap<>();
//	    
//	    private Socket tcpSocket;
//	    private BufferedWriter socketWriter;
//	    private BufferedReader socketReader;
//	    private volatile boolean isConnected = false;
//	    private volatile boolean isShuttingDown = false;
//	    
//	    public TcpConnectionService(TcpConnectionProperties config) {
//	    	this.config=config;
//	    }
//	    
//	    
//	    @PostConstruct
//	    public void initialize() {
//	        logger.info("Initializing TCP connection service with shore: {}:{}", 
//	                   config.getShoreHost(), config.getShorePort());
//	        startConnectionManager();
//	    }
//	    
//	    @PreDestroy
//	    public void shutDown() {
//	    	logger.info("Shutting down TCP connection service...");
//	        isShuttingDown = true;
//	        
//	        closeConnection();
//	    }
//	    
//	    private void establishConnection() {
//	    	
//	    	try {
//	    		logger.info("Attempting to connect to shore at {}:{}", 
//	                       config.getShoreHost(), config.getShorePort());
//	    		
//	    		tcpSocket = new Socket(config.getShoreHost(), config.getShorePort());
//	    		tcpSocket.setKeepAlive(config.isKeepAlive());
//	    		tcpSocket.setTcpNoDelay(config.isTcpNoDelay());
//	    		
//	    		socketWriter = new BufferedWriter(new OutputStreamWriter(tcpSocket.getOutputStream()));
//	    		socketReader =  new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
//	    		
//	    		isConnected = true;
//	    		logger.info("Successfully connected to shore");
//	    	}
//	    	catch(Exception e) {
//	    		logger.warn("Failed to connect to shore: {}", e.getMessage());
//	            closeConnection();
//	    	}
//	    	
//	    }
//	    
//	    public CompletableFuture<ProxyResponse> sendRequest(ProxyRequest request){
//	    	
//	    	CompletableFuture<ProxyResponse> future = new CompletableFuture<>();
//	    	pendingRequests.put(request.getRequestId(), future);
//	    	
//	    	future.orTimeout(config.getResponseTimeoutSeconds(), TimeUnit.SECONDS)
//	    				.whenComplete((response, throwable)->{
//	    					if(throwable != null) {
//	    						pendingRequests.remove(request.getRequestId());
//	    					}
//	    				});
//	    	
//	    	requestQueue.offer(request);
//	    	return future;
//	    	
//	    }
//	    
//	    private void startConnectionManager() {
//	    	
//	    	Thread connectionThread = new Thread(this::manageConnection, "tcp-connection-manager");
//	    	connectionThread.setDaemon(true);
//	    	connectionThread.start();
//	    	
//	    	Thread responseThread = new Thread(this::listenForResponses, "tcp-response-listener");
//	    	responseThread.setDaemon(true);
//	    	responseThread.start();
//	    	
//	    }
//	    
//	    private void manageConnection() {
//	        while (!isShuttingDown) {
//	            try {
//	                if (!isConnected) {
//	                    establishConnection();
//	                }
//	                
//	                if (isConnected) {
//	                    processRequestQueue();
//	                }
//	                
//	                Thread.sleep(config.getConnectionManagerSleepMs());
//	                
//	            } catch (InterruptedException e) {
//	                Thread.currentThread().interrupt();
//	                break;
//	            } catch (Exception e) {
//	                logger.error("Error in connection management: {}", e.getMessage());
//	                closeConnection();
//	                // Wait before retry
//	                try {
//	                    Thread.sleep(config.getConnectionRetryDelaySeconds() * 1000L);
//	                } catch (InterruptedException ie) {
//	                    Thread.currentThread().interrupt();
//	                    break;
//	                }
//	            }
//	        }
//	    }
//	    
//	    private void processRequestQueue() {
//	        try {
//	            ProxyRequest request = requestQueue.poll(config.getQueuePollTimeoutSeconds(), TimeUnit.SECONDS);
//	            if (request != null && isConnected) {
//	                sendRequestToShore(request);
//	            }
//	        } catch (InterruptedException e) {
//	            Thread.currentThread().interrupt();
//	        } catch (Exception e) {
//	            logger.error("Error processing request queue: {}", e.getMessage());
//	            closeConnection();
//	        }
//	    }
//	    
//	    private void sendRequestToShore(ProxyRequest request) throws IOException {
//	        String jsonRequest = objectMapper.writeValueAsString(request);
//	        logger.debug("Sending to shore: {}", request);
//	        
//	        socketWriter.write(jsonRequest);
//	        socketWriter.newLine();
//	        socketWriter.flush();
//	    }
//	    
//	    private void listenForResponses() {
//	        while (!isShuttingDown) {
//	            try {
//	                if (isConnected && socketReader != null) {
//	                    String jsonResponse = socketReader.readLine();
//	                    if (jsonResponse != null) {
//	                        handleResponse(jsonResponse);
//	                    } else {
//	                        // Connection closed by shore
//	                        logger.warn("Shore closed the connection");
//	                        closeConnection();
//	                    }
//	                } else {
//	                    Thread.sleep(1000);
//	                }
//	            } catch (SocketException e) {
//	                logger.warn("Socket error while listening for responses: {}", e.getMessage());
//	                closeConnection();
//	            } catch (Exception e) {
//	                logger.error("Error listening for responses: {}", e.getMessage());
//	                closeConnection();
//	            }
//	        }
//	    }
//	    
//	    private void handleResponse(String jsonResponse) {
//	        try {
//	            ProxyResponse response = objectMapper.readValue(jsonResponse, ProxyResponse.class);
//	            logger.debug("Received response: {}", response);
//	            
//	            CompletableFuture<ProxyResponse> future = pendingRequests.remove(response.getRequestId());
//	            if (future != null) {
//	                future.complete(response);
//	            } else {
//	                logger.warn("Received response for unknown request: {}", response.getRequestId());
//	            }
//	            
//	        } catch (Exception e) {
//	            logger.error("Error parsing response: {}", e.getMessage());
//	        }
//	    }
//	    
//	    private void closeConnection() {
//	        isConnected = false;
//	        try {
//	            if (socketWriter != null) socketWriter.close();
//	            if (socketReader != null) socketReader.close();
//	            if (tcpSocket != null) tcpSocket.close();
//	        } catch (Exception e) {
//	            logger.debug("Error closing connection: {}", e.getMessage());
//	        }
//	        socketWriter = null;
//	        socketReader = null;
//	        tcpSocket = null;
//	    }
//	    
//	    public boolean isConnected() {
//	        return isConnected;
//	    }
//	    
//	    public int getQueueSize() {
//	        return requestQueue.size();
//	    }
//	    
//	    public int getPendingRequestCount() {
//	        return pendingRequests.size();
//	    }
//	    
//	    
//}
