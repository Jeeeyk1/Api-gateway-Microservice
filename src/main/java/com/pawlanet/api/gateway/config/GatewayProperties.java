package com.pawlanet.api.gateway.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "gateway")
@Data
public class GatewayProperties {
    
    private Redis redis = new Redis();
    private RateLimit rateLimit = new RateLimit();
    private Security security = new Security();
    private Map<String, Service> services;
    
    @Data
    public static class Redis {
        private int maxRetries = 3;
        private long ttl = 3600; // seconds
        private boolean enableCaching = true;
    }
    
    @Data
    public static class RateLimit {
        private int defaultLimit = 100;
        private int burstCapacity = 200;
        private int premiumLimit = 1000;
        private int premiumBurst = 2000;
        private boolean enabled = true;
    }
    
    @Data
    public static class Security {
        private boolean enableCsrf = false;
        private boolean enableAuth = true;
        private String[] publicPaths = {"/api/v1/auth/**", "/actuator/**"};
    }
    
    @Data
    public static class Service {
        private String url;
        private int timeout = 5000;
        private int retries = 3;
        private boolean circuitBreakerEnabled = true;
    }
}
