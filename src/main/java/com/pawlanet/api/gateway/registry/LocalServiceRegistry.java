package com.pawlanet.api.gateway.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ServiceNotFoundException;

/**
 * Service Registry for local development (localhost)
 */
@Slf4j
@Component
public class LocalServiceRegistry implements ServiceRegistry {
    
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    private final WebClient webClient = WebClient.create();
    
@PostConstruct
    public void init() {
        // Register local services with localhost URLs
        registerService("auth-service", 
            new ServiceInfo("auth-service", "localhost", 8081));
        registerService("user-profile-service", 
            new ServiceInfo("user-profile-service", "localhost", 8082));
        registerService("post-service", 
            new ServiceInfo("post-service", "localhost", 8083));
        registerService("image-validation-service", 
            new ServiceInfo("image-validation-service", "localhost", 8084));
        
        log.info("Local Service Registry initialized with {} services", services.size());
    }
    
    @Override
    public ServiceInfo getService(String serviceName) {
        ServiceInfo service = services.get(serviceName);
        if (service == null) {
            log.warn("Service {} not found in local registry", serviceName);
            throw new RuntimeException("Service not found: " + serviceName);
        }
        return service;
    }
    
    @Override
    public Map<String, ServiceInfo> getAllServices() {
        return new ConcurrentHashMap<>(services);
    }
    
    @Override
    public void registerService(String name, ServiceInfo serviceInfo) {
        services.put(name, serviceInfo);
        log.info("Registered local service: {} at {}:{}", 
            name, serviceInfo.getUrl(), serviceInfo.getPort());
    }
    
    @Override
    public void deregisterService(String name) {
        ServiceInfo removed = services.remove(name);
        if (removed != null) {
            log.info("Deregistered local service: {}", name);
        }
    }
    
    @Override
    public boolean isHealthy(String serviceName) {
        ServiceInfo service = getService(serviceName);
        try {
            Boolean healthy = webClient.get()
                .uri(service.getHealthCheckUrl())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> response.contains("UP"))
                .timeout(Duration.ofSeconds(2))
                .onErrorReturn(false)
                .block();
            
            return Boolean.TRUE.equals(healthy);
        } catch (Exception e) {
            log.error("Health check failed for service {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getServiceUrl(String serviceName) {
        ServiceInfo service = getService(serviceName);
        return service.getFullUrl();
    }
}