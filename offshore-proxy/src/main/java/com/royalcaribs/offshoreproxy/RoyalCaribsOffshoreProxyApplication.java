package com.royalcaribs.offshoreproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class RoyalCaribsOffshoreProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoyalCaribsOffshoreProxyApplication.class, args);
	}

}
