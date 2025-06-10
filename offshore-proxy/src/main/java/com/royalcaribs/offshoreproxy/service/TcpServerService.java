package com.royalcaribs.offshoreproxy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.royalcaribs.offshoreproxy.config.ShoreProperties;
import com.royalcaribs.offshoreproxy.dto.ProxyRequest;
import com.royalcaribs.offshoreproxy.dto.ProxyResponse;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TcpServerService {

	private static final Logger logger = LoggerFactory.getLogger(TcpServerService.class);

	private final ShoreProperties config;
	private final ObjectMapper objectMapper;
	private final HttpClientService httpClientService;
	private final ExecutorService executorService;
	
	private ServerSocket serverSocket;
    private volatile boolean isRunning = false;

	public TcpServerService(ShoreProperties config, ObjectMapper objectMapper, HttpClientService httpClientService) {
		this.config = config;
		this.objectMapper = objectMapper;
		this.httpClientService = httpClientService;
		this.executorService = Executors.newFixedThreadPool(config.getMaxConcurrentRequests());
	}
	
	@PostConstruct
    public void startServer() {
        logger.info("Starting TCP server on port {}", config.getTcpPort());
        
        CompletableFuture.runAsync(() -> {
            try {
                serverSocket = new ServerSocket(config.getTcpPort());
                isRunning = true;
                logger.info("TCP server listening on port {}", config.getTcpPort());
                
                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Ship connected from: {}", clientSocket.getRemoteSocketAddress());
                    executorService.submit(() -> handleShipConnection(clientSocket));
                }
                
            } catch (Exception e) {
                if (isRunning) {
                    logger.error("TCP server error: {}", e.getMessage());
                }
            }
        });
    }
    
    @PreDestroy
    public void stopServer() {
        logger.info("Stopping TCP server...");
        isRunning = false;
        
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            executorService.shutdown();
        } catch (Exception e) {
            logger.error("Error stopping TCP server: {}", e.getMessage());
        }
    }
    
    private void handleShipConnection(Socket shipSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(shipSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(shipSocket.getOutputStream()))) {
            
            logger.info("Handling ship connection: {}", shipSocket.getRemoteSocketAddress());
            
            String jsonRequest;
            while ((jsonRequest = reader.readLine()) != null) {
                try {
                    ProxyResponse response = processRequest(jsonRequest);
                    sendResponse(writer, response);
                } catch (Exception e) {
                    logger.error("Error processing request: {}", e.getMessage());
                    sendErrorResponse(writer, "Request processing failed");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error handling ship connection: {}", e.getMessage());
        } finally {
            try {
                shipSocket.close();
                logger.info("Ship connection closed");
            } catch (Exception e) {
                logger.debug("Error closing ship socket: {}", e.getMessage());
            }
        }
    }
    
    private ProxyResponse processRequest(String jsonRequest) throws Exception {
        ProxyRequest request = objectMapper.readValue(jsonRequest, ProxyRequest.class);
        logger.info("Processing request: {} {} {}", 
                   request.getRequestId(), request.getMethod(), request.getUrl());
        
        return httpClientService.executeRequest(request);
    }
    
    private synchronized void sendResponse(BufferedWriter writer, ProxyResponse response) {
        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            writer.write(jsonResponse);
            writer.newLine();
            writer.flush();
            logger.debug("Sent response: {}", response.getRequestId());
        } catch (Exception e) {
            logger.error("Error sending response: {}", e.getMessage());
        }
    }
    
    private synchronized void sendErrorResponse(BufferedWriter writer, String errorMessage) {
        try {
            ProxyResponse errorResponse = new ProxyResponse();
            errorResponse.setRequestId("unknown");
            errorResponse.setSuccess(false);
            errorResponse.setStatusCode(500);
            errorResponse.setErrorMessage(errorMessage);
            
            sendResponse(writer, errorResponse);
        } catch (Exception e) {
            logger.error("Error sending error response: {}", e.getMessage());
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
}