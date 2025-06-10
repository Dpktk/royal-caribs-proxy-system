package com.royalcaribs.offshoreproxy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.royalcaribs.offshoreproxy.config.ShoreProperties;
import com.royalcaribs.offshoreproxy.dto.ProxyRequest;
import com.royalcaribs.offshoreproxy.dto.ProxyResponse;

import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;


import java.util.HashMap;
import java.util.Map;


@Service
public class HttpClientService {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClientService.class);
    
    private final ShoreProperties config;
    private final RestTemplate restTemplate;
    
    public HttpClientService(ShoreProperties config) {
        this.config = config;
        this.restTemplate = createRestTemplate();
    }
    
    /**
     * Connect to the actual destination server.
     */
    public ProxyResponse executeRequest(ProxyRequest request) {
        try {
            logger.info("Executing HTTP request: {} {}", request.getMethod(), request.getUrl());
            
            // Handle HTTPS tunneling
            if ("CONNECT".equalsIgnoreCase(request.getMethod())) {
                return handleConnectRequest(request);
            }
            
            // Build HTTP headers for regular requests
            HttpHeaders headers = new HttpHeaders();
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(headers::add);
            }
            
            HttpEntity<byte[]> httpEntity = new HttpEntity<>(request.getBody(), headers);
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                request.getUrl(),
                HttpMethod.valueOf(request.getMethod()),
                httpEntity,
                byte[].class
            );
            
            return buildSuccessResponse(request.getRequestId(), response);
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.warn("HTTP error for request {}: {} {}", 
                       request.getRequestId(), e.getStatusCode(), e.getMessage());
            return buildErrorResponse(request.getRequestId(), e.getStatusCode().value(), 
                                    e.getResponseBodyAsString());
            
        } catch (Exception e) {
            logger.error("Network error for request {}: {}", request.getRequestId(), e.getMessage());
            return buildErrorResponse(request.getRequestId(), 502, 
                                    "Gateway error: " + e.getMessage());
        }
    }
    
    /**
     * Handle CONNECT requests for HTTPS
     */
	private ProxyResponse handleConnectRequest(ProxyRequest request) {
		try {
			String target = request.getUrl();
			String[] parts = target.split(":");
			String hostname = parts[0];
			int port = Integer.parseInt(parts[1]);

			if (port == 443) {
				// Convert CONNECT to HTTPS GET request
				String httpsUrl = "https://" + hostname + "/";
				ResponseEntity<byte[]> response = restTemplate.exchange(httpsUrl, HttpMethod.GET,
						new HttpEntity<>(new HttpHeaders()), byte[].class);
				return buildSuccessResponse(request.getRequestId(), response);
			} else {
				// Return connection established
				return new ProxyResponse(request.getRequestId(), 200, new HashMap<>(),
						"Connection established".getBytes(), true, null);
			}

		} catch (Exception e) {
			logger.error("Failed to handle CONNECT request {}: {}", request.getRequestId(), e.getMessage());
			return buildErrorResponse(request.getRequestId(), 502, "CONNECT failed: " + e.getMessage());
		}
	}
    
    /**
     * Create RestTemplate with SSL support
     */
    private RestTemplate createRestTemplate() {
    	 try {
             HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
             factory.setConnectTimeout(config.getHttpClientTimeoutSeconds() * 1000);
             factory.setReadTimeout(config.getHttpClientTimeoutSeconds() * 1000);
             
             var httpClient = HttpClients.custom()
                 .setConnectionManager(
                     PoolingHttpClientConnectionManagerBuilder.create()
                         .setSSLSocketFactory(
                             SSLConnectionSocketFactoryBuilder.create()
                                 .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                 .build())
                         .build())
                 .build();
                 
             factory.setHttpClient(httpClient);
             RestTemplate restTemplate = new RestTemplate(factory);
             logger.info("Created SSL-enabled RestTemplate");
             return restTemplate;
             
         } catch (Exception e) {
             logger.error("Failed to create SSL-enabled RestTemplate: {}", e.getMessage());
             return new RestTemplate();
         }
     }
    
    private ProxyResponse buildSuccessResponse(String requestId, ResponseEntity<byte[]> response) {
    	Map<String, String> responseHeaders = new HashMap<>();
        response.getHeaders().forEach((key, values) -> {
            if (!values.isEmpty()) {
                responseHeaders.put(key, values.get(0));
            }
        });
        
        return new ProxyResponse(
            requestId,
            response.getStatusCode().value(),
            responseHeaders,
            response.getBody() != null ? response.getBody() : new byte[0],
            true,
            null
        );
    }

    private ProxyResponse buildErrorResponse(String requestId, int statusCode, String errorMessage) {
        return new ProxyResponse(
            requestId,
            statusCode,
            new HashMap<>(),
            errorMessage.getBytes(),
            false,
            errorMessage
        );
    }
}