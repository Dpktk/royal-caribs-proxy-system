package com.royalcaribs.shipproxy;

import org.springframework.boot.CommandLineRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.royalcaribs.shipproxy.service.ShipProxyService;

@SpringBootApplication
@EnableConfigurationProperties
public class RoyalCaribsShipProxyApplication implements CommandLineRunner{
	
	private static final Logger logger = LoggerFactory.getLogger(RoyalCaribsShipProxyApplication.class);
	
private final ShipProxyService shipProxyService;
    
    public RoyalCaribsShipProxyApplication(ShipProxyService shipProxyService) {
        this.shipProxyService = shipProxyService;
    }

	public static void main(String[] args) {
		SpringApplication.run(RoyalCaribsShipProxyApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		 logger.info("🚢 Starting Royal Caribs Ship Proxy with LittleProxy");
	        
	        // Start the proxy service
	        shipProxyService.start();
	        
	        // Keep application running
	        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	            logger.info("🛑 Shutting down Ship Proxy...");
	            shipProxyService.stop();
	        }));
	        
	        logger.info("✅ Ship Proxy is running. Configure your browser proxy to: localhost:8080");
	        
	        // Keep main thread alive
	        Thread.currentThread().join();
	    }

}