package com.pawlanet.api.gateway.registry;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kubernetes Service Watcher for dynamic service discovery
 */
@Slf4j
@RequiredArgsConstructor
public class ServiceWatcher implements Watcher<Service> {
    
    private final KubernetesServiceRegistry registry;
    
    @Override
    public void eventReceived(Action action, Service service) {
        String serviceName = service.getMetadata().getName();
        
        switch (action) {
            case ADDED:
                log.info("Kubernetes service added: {}", serviceName);
                handleServiceAdded(service);
                break;
            case MODIFIED:
                log.info("Kubernetes service modified: {}", serviceName);
                handleServiceModified(service);
                break;
            case DELETED:
                log.info("Kubernetes service deleted: {}", serviceName);
                handleServiceDeleted(service);
                break;
            default:
                log.debug("Unhandled action {} for service {}", action, serviceName);
        }
    }
    
    @Override
    public void onClose(WatcherException e) {
        if (e != null) {
            log.error("Kubernetes watcher closed with error", e);
        } else {
            log.info("Kubernetes watcher closed");
        }
    }
    
    private void handleServiceAdded(Service service) {
        String serviceName = service.getMetadata().getName();
        Integer port = service.getSpec().getPorts().get(0).getPort();
        String namespace = service.getMetadata().getNamespace();
        
        ServiceRegistry.ServiceInfo serviceInfo = new ServiceRegistry.ServiceInfo(
            serviceName,
            serviceName + "." + namespace + ".svc.cluster.local",
            port
        );
        
        registry.registerService(serviceName, serviceInfo);
    }
    
    private void handleServiceModified(Service service) {
        // Re-register with updated information
        handleServiceAdded(service);
    }
    
    private void handleServiceDeleted(Service service) {
        String serviceName = service.getMetadata().getName();
        registry.deregisterService(serviceName);
    }
}