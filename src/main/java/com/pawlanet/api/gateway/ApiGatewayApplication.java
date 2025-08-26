package com.pawlanet.api.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.pawlanet.api.gateway.registry.ServiceRegistry;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class ApiGatewayApplication {
	@Autowired
	private Environment environment;

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	public CommandLineRunner startupSetup(ServiceRegistry serviceRegistry) {
		return args ->{
	           String[] activeProfiles = environment.getActiveProfiles();
	            String profile = activeProfiles.length > 0 ? activeProfiles[0] : "default";
	            
	            log.info("========================================");
	            log.info("API Gateway Started Successfully");
	            log.info("Active Profile: {}", profile);
	            log.info("Service Registry Type: {}", serviceRegistry.getClass().getSimpleName());
	            log.info("========================================");
	            
	            // Log all registered services
	            serviceRegistry.getAllServices().forEach((name, service) -> {
	                log.info("Registered Service: {} -> {}", name, service.getFullUrl());
	            });
	            
	            log.info("Performing initial health checks...");
	            serviceRegistry.getAllServices().forEach((name, service) -> {
	                boolean healthy = serviceRegistry.isHealthy(name);
	                log.info("Service {} health: {}", name, healthy ? "UP AND rUNNING" : " Not available");
	            });
	            
	            log.info("Gateway is ready to handle requests!");
	        };
		}
	
}
