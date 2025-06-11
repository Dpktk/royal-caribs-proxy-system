package com.royalcaribs.offshoreproxy.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.royalcaribs.offshoreproxy.dto.ProxyRequest;
import com.royalcaribs.offshoreproxy.dto.ProxyResponse;
import com.royalcaribs.offshoreproxy.service.HttpClientService;

@RestController
public class ProxyController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);
    
    private final HttpClientService httpClientService;
    
    public ProxyController(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }
    
    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("Royal Caribs Offshore Proxy - Server Running");
    }
    
    @PostMapping("/proxy")
    public ResponseEntity<ProxyResponse> proxyRequest(@RequestBody ProxyRequest request) {
        logger.info("HTTP proxy endpoint called for request: {} {} {}", 
                   request.getRequestId(), request.getMethod(), request.getUrl());
        
        try {
            ProxyResponse response = httpClientService.executeRequest(request);
            
            logger.info("HTTP proxy request {} completed with status {}", 
                       request.getRequestId(), response.getStatusCode());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in HTTP proxy endpoint for request {}: {}", 
                        request.getRequestId(), e.getMessage());
            
            ProxyResponse errorResponse = new ProxyResponse(
                request.getRequestId(),
                500,
                new java.util.HashMap<>(),
                ("Proxy request failed: " + e.getMessage()).getBytes(),
                false,
                "Proxy request failed: " + e.getMessage()
            );
            
            return ResponseEntity.ok(errorResponse);
        }
    }
}