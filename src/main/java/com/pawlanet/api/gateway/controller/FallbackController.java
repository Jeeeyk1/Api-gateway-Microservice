package com.pawlanet.api.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, String>>> authServiceFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createErrorResponse("Authentication service is temporarily unavailable")));
    }

    @GetMapping("/users")
    public Mono<ResponseEntity<Map<String, String>>> userServiceFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createErrorResponse("User service is temporarily unavailable")));
    }

    @GetMapping("/posts")
    public Mono<ResponseEntity<Map<String, String>>> postServiceFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createErrorResponse("Post service is temporarily unavailable")));
    }

    @GetMapping("/validation")
    public Mono<ResponseEntity<Map<String, String>>> validationServiceFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createErrorResponse("Image validation service is temporarily unavailable")));
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }
}