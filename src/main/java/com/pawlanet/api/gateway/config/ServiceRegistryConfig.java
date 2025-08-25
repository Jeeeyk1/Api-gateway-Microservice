package com.pawlanet.api.gateway.config;

import com.pawlanet.api.gateway.registry.DockerServiceRegistry;
import com.pawlanet.api.gateway.registry.KubernetesServiceRegistry;
import com.pawlanet.api.gateway.registry.LocalServiceRegistry;
import com.pawlanet.api.gateway.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for Service Registry based on active profile
 */
@Configuration
@Slf4j
public class ServiceRegistryConfig {
    
    @Value("${spring.profiles.active:local}")
    private String activeProfile;
    
    @Bean
    @Profile("local")
    public ServiceRegistry localServiceRegistry() {
        log.info("Creating Local Service Registry");
        return new LocalServiceRegistry();
    }
    
    @Bean
    @Profile("docker")
    public ServiceRegistry dockerServiceRegistry() {
        log.info("Creating Docker Service Registry");
        return new DockerServiceRegistry();
    }
    
    @Bean
    @Profile("kubernetes")
    public ServiceRegistry kubernetesServiceRegistry() {
        log.info("Creating Kubernetes Service Registry");
        return new KubernetesServiceRegistry();
    }
    
    // Default fallback
    @Bean
    @Profile("!local & !docker & !kubernetes")
    public ServiceRegistry defaultServiceRegistry() {
        log.warn("No specific profile detected, using Local Service Registry as default");
        return new LocalServiceRegistry();
    }
}