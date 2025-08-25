package com.pawlanet.api.gateway.registry;


import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service Registry for Kubernetes environment
 * Uses Kubernetes Service Discovery API
 */
@Slf4j
@Component
public class KubernetesServiceRegistry implements ServiceRegistry {
    
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    private final WebClient webClient = WebClient.create();
    private KubernetesClient kubernetesClient;
    
    @Value("${kubernetes.namespace:pawlanet-apps}")
    private String namespace;
    
    @Value("${kubernetes.service.suffix:.svc.cluster.local}")
    private String serviceSuffix;
    
    @PostConstruct
    public void init() {
        try {
            // Initialize Kubernetes client
            kubernetesClient = new DefaultKubernetesClient();
            
            // Discover services in the namespace
            discoverServices();
            
            // Watch for service changes
            watchServices();
            
            log.info("Kubernetes Service Registry initialized with {} services", services.size());
        } catch (Exception e) {
            log.error("Failed to initialize Kubernetes Service Registry", e);
            // Fallback to static configuration
            registerStaticServices();
        }
    }
    
    private void discoverServices() {
        try {
            List<Service> k8sServices = kubernetesClient
                .services()
                .inNamespace(namespace)
                .list()
                .getItems();
            
            for (Service k8sService : k8sServices) {
                String serviceName = k8sService.getMetadata().getName();
                
                // Filter for our application services
                if (isApplicationService(serviceName)) {
                    Integer port = k8sService.getSpec().getPorts().get(0).getPort();
                    String clusterIP = k8sService.getSpec().getClusterIP();
                    
                    ServiceInfo serviceInfo = new ServiceInfo(
                        serviceName,
                        serviceName + "." + namespace + serviceSuffix,
                        port
                    );
                    
                    // Add Kubernetes metadata
                    Map<String, String> metadata = new ConcurrentHashMap<>();
                    metadata.put("namespace", namespace);
                    metadata.put("clusterIP", clusterIP);
                    metadata.put("uid", k8sService.getMetadata().getUid());
                    serviceInfo.setMetadata(metadata);
                    
                    services.put(serviceName, serviceInfo);
                    log.info("Discovered Kubernetes service: {} at {}:{}", 
                        serviceName, serviceInfo.getUrl(), port);
                }
            }
        } catch (Exception e) {
            log.error("Failed to discover Kubernetes services", e);
        }
    }
    
    private void watchServices() {
        try {
            kubernetesClient.services()
                .inNamespace(namespace)
                .watch(new ServiceWatcher(this));
        } catch (Exception e) {
            log.error("Failed to watch Kubernetes services", e);
        }
    }
    
    private boolean isApplicationService(String serviceName) {
        return serviceName.endsWith("-service") || 
               serviceName.equals("api-gateway");
    }
    
    private void registerStaticServices() {
        // Fallback static configuration for Kubernetes
        registerService("auth-service", 
            new ServiceInfo("auth-service", 
                "auth-service." + namespace + serviceSuffix, 8081));
        registerService("user-profile-service", 
            new ServiceInfo("user-profile-service", 
                "user-profile-service." + namespace + serviceSuffix, 8082));
        registerService("post-service", 
            new ServiceInfo("post-service", 
                "post-service." + namespace + serviceSuffix, 8083));
        registerService("image-validation-service", 
            new ServiceInfo("image-validation-service", 
                "image-validation-service." + namespace + serviceSuffix, 8084));
    }
    
    @Override
    public ServiceInfo getService(String serviceName) {
        ServiceInfo service = services.get(serviceName);
        if (service == null) {
            // Try to discover the service dynamically
            Service k8sService = kubernetesClient
                .services()
                .inNamespace(namespace)
                .withName(serviceName)
                .get();
            
            if (k8sService != null) {
                Integer port = k8sService.getSpec().getPorts().get(0).getPort();
                service = new ServiceInfo(
                    serviceName,
                    serviceName + "." + namespace + serviceSuffix,
                    port
                );
                services.put(serviceName, service);
            } else {
                throw new RuntimeException("Service not found in Kubernetes: " + serviceName);
            }
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
        log.info("Registered Kubernetes service: {} at {}:{}", 
            name, serviceInfo.getUrl(), serviceInfo.getPort());
    }
    
    @Override
    public void deregisterService(String name) {
        ServiceInfo removed = services.remove(name);
        if (removed != null) {
            log.info("Deregistered Kubernetes service: {}", name);
        }
    }
    
    @Override
    public boolean isHealthy(String serviceName) {
        ServiceInfo service = getService(serviceName);
        try {
            String healthUrl = String.format("http://%s:%d/actuator/health", 
                service.getUrl(), service.getPort());
            
            Boolean healthy = webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> response.contains("UP"))
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(false)
                .block();
            
            return Boolean.TRUE.equals(healthy);
        } catch (Exception e) {
            log.debug("Health check failed for Kubernetes service {}: {}", 
                serviceName, e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getServiceUrl(String serviceName) {
        ServiceInfo service = getService(serviceName);
        // Return full Kubernetes DNS name
        return String.format("http://%s:%d", service.getUrl(), service.getPort());
    }
    
    @jakarta.annotation.PreDestroy
    public void shutdown() {
        if (kubernetesClient != null) {
            kubernetesClient.close();
        }
    }
}
