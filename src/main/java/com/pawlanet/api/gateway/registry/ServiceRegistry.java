package com.pawlanet.api.gateway.registry;


import lombok.Data;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for service discovery across different environments
 */
public interface ServiceRegistry {
    
    /**
     * Get service URL by service name
     */
    ServiceInfo getService(String serviceName);
    
    /**
     * Get all registered services
     */
    Map<String, ServiceInfo> getAllServices();
    
    /**
     * Register a new service
     */
    void registerService(String name, ServiceInfo serviceInfo);
    
    /**
     * Deregister a service
     */
    void deregisterService(String name);
    
    /**
     * Health check for a service
     */
    boolean isHealthy(String serviceName);
    
    /**
     * Get service URL with load balancing
     */
    String getServiceUrl(String serviceName);
    
    @Data
    class ServiceInfo {
        private String name;
        private String url;
        private String healthCheckUrl;
        private int port;
        private String protocol = "http";
        private boolean secure = false;
        private Map<String, String> metadata;
        
        public ServiceInfo(String name, String url, int port) {
            this.name = name;
            this.url = url;
            this.port = port;
            this.healthCheckUrl = url + "/actuator/health";
        }
        
        public String getFullUrl() {
            return protocol + "://" + url + ":" + port;
        }
    }
}