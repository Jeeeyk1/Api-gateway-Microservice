package com.pawlanet.api.gateway.registry;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service Registry for Docker Compose environment
 * Uses Docker container names for service discovery
 */
@Slf4j
@Component
public class DockerServiceRegistry implements ServiceRegistry {
    
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    private final WebClient webClient = WebClient.create();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @Value("${docker.network.name:pawlanet-network}")
    private String dockerNetwork;
    
    @PostConstruct
    public void init() {
        // Register Docker services using container names
        registerService("auth-service", 
            new ServiceInfo("auth-service", "auth-service", 8081));
        registerService("user-profile-service", 
            new ServiceInfo("user-profile-service", "user-profile-service", 8082));
        registerService("post-service", 
            new ServiceInfo("post-service", "post-service", 8083));
        registerService("image-validation-service", 
            new ServiceInfo("image-validation-service", "image-validation-service", 8084));
        
        log.info("Docker Service Registry initialized with {} services", services.size());
        
        // Start health check scheduler
        startHealthCheckScheduler();
    }
    
    private void startHealthCheckScheduler() {
        scheduler.scheduleWithFixedDelay(() -> {
            services.forEach((name, service) -> {
                boolean healthy = isHealthy(name);
                if (!healthy) {
                    log.warn("Service {} is unhealthy in Docker environment", name);
                }
            });
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    @Override
    public ServiceInfo getService(String serviceName) {
        ServiceInfo service = services.get(serviceName);
        if (service == null) {
            log.warn("Service {} not found in Docker registry", serviceName);
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
        // In Docker, add network information
        Map<String, String> metadata = new ConcurrentHashMap<>();
        metadata.put("network", dockerNetwork);
        metadata.put("container", name);
        serviceInfo.setMetadata(metadata);
        
        services.put(name, serviceInfo);
        log.info("Registered Docker service: {} at container {}:{}", 
            name, serviceInfo.getUrl(), serviceInfo.getPort());
    }
    
    @Override
    public void deregisterService(String name) {
        ServiceInfo removed = services.remove(name);
        if (removed != null) {
            log.info("Deregistered Docker service: {}", name);
        }
    }
    
    @Override
    public boolean isHealthy(String serviceName) {
        ServiceInfo service = getService(serviceName);
        try {
            // Docker internal DNS resolution
            String healthUrl = String.format("http://%s:%d/actuator/health", 
                service.getUrl(), service.getPort());
            
            Boolean healthy = webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> response.contains("UP"))
                .timeout(Duration.ofSeconds(3))
                .onErrorReturn(false)
                .block();
            
            return Boolean.TRUE.equals(healthy);
        } catch (Exception e) {
            log.debug("Health check failed for Docker service {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getServiceUrl(String serviceName) {
        ServiceInfo service = getService(serviceName);
        // In Docker, use container name as hostname
        return String.format("http://%s:%d", service.getUrl(), service.getPort());
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}