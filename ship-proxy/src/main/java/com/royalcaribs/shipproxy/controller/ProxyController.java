//package com.royalcaribs.shipproxy.controller;
//
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.royacaribs.shipproxy.dto.ProxyRequest;
//import com.royacaribs.shipproxy.dto.ProxyResponse;
//import com.royalcaribs.shipproxy.service.TcpConnectionService;
//import jakarta.servlet.http.HttpServletRequest;
//
//
//@RestController
//public class ProxyController {
//	
//	private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);
//	
//	private final TcpConnectionService tcpConnectionService;
//	
//	public ProxyController(TcpConnectionService tcpConnectionService) {
//		this.tcpConnectionService = tcpConnectionService;
//	}
//	
//	@RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, 
//			 RequestMethod.PUT, RequestMethod.PATCH, 
//			 RequestMethod.DELETE})
//	public CompletableFuture<ResponseEntity<byte[]>> proxyRequest(
//            HttpServletRequest request,
//            @RequestBody(required = false) byte[] body) {
//        
//        String requestId = UUID.randomUUID().toString();
//        
//        String requestPath = request.getRequestURI();
//        String targetUrl = extractTargetUrl(requestPath, request);
//        
//        String queryString = request.getQueryString();
//        if (queryString != null && !targetUrl.contains("?")) {
//            targetUrl += "?" + queryString;
//        }
//        
//        logger.info("Proxying request {} - {} {} -> {}", 
//                   requestId, request.getMethod(), request.getRequestURL(), targetUrl);
//        
//        Map<String, String> headers = extractHeaders(request);
//        
//        ProxyRequest proxyRequest = new ProxyRequest(
//            requestId,
//            request.getMethod(),
//            targetUrl,  
//            headers,
//            body != null ? body : new byte[0], System.currentTimeMillis()
//        );
//        
//        return tcpConnectionService.sendRequest(proxyRequest)
//            .thenApply(this::buildHttpResponse)
//            .exceptionally(this::handleError);
//    }
//	
//	private Map<String, String> extractHeaders(HttpServletRequest request) {
//        Map<String, String> headers = new HashMap<>();
//        Enumeration<String> headerNames = request.getHeaderNames();
//        
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            String headerValue = request.getHeader(headerName);
//            headers.put(headerName, headerValue);
//        }
//        
//        return headers;
//    }
//	
//	private String extractTargetUrl(String requestPath, HttpServletRequest request) {
//        // Remove leading slash
//        if (requestPath.startsWith("/")) {
//            requestPath = requestPath.substring(1);
//        }
//        
//        // If it starts with http:// or https://, it's already a full URL
//        if (requestPath.startsWith("http://") || requestPath.startsWith("https://")) {
//            return requestPath;
//        }
//        
//        // If it starts with https:/ (missing one slash), fix it
//        if (requestPath.startsWith("https:/")) {
//            return "https://" + requestPath.substring(7);
//        }
//        
//        if (requestPath.startsWith("http:/")) {
//            return "http://" + requestPath.substring(6);
//        }
//        
//        // Default: assume it's an external URL and add https://
//        return "https://" + requestPath;
//    }
//	
//	private ResponseEntity<byte[]> buildHttpResponse(ProxyResponse proxyResponse) {
//        if (proxyResponse.isSuccess()) {
//            return ResponseEntity
//                .status(proxyResponse.getStatusCode())
//                .headers(httpHeaders -> {
//                    proxyResponse.getHeaders().forEach(httpHeaders::add);
//                })
//                .body(proxyResponse.getBody());
//        } else {
//            logger.error("Error response from shore: {}", proxyResponse.getErrorMessage());
//            return ResponseEntity
//                .status(HttpStatus.BAD_GATEWAY)
//                .body(proxyResponse.getErrorMessage().getBytes());
//        }
//    }
//    
//    private ResponseEntity<byte[]> handleError(Throwable throwable) {
//        logger.error("Error processing proxy request: {}", throwable.getMessage());
//        return ResponseEntity
//            .status(HttpStatus.GATEWAY_TIMEOUT)
//            .body("Gateway timeout - shore connection unavailable".getBytes());
//    }
//	
//	
//}
