package org.example.orderservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.clients.ProductClient;
import org.example.orderservice.exceptions.ProductNotFoundException;
import org.example.orderservice.exceptions.ProductServiceException;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductGatewayService {

    private final ProductClient productClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    public ProductResponse getProductById(Long productId) {
        ProductResponse response = productClient.getProductById(productId);
        if (response == null || response.id() == null) {
            throw new ProductNotFoundException(productId);
        }
        return response;
    }

    public ProductResponse getProductByIdFallback(Long productId, Throwable throwable) {
        log.error("ProductGatewayService fallback triggered for productId {}: {}", productId, throwable.getMessage());
        if (throwable instanceof ProductNotFoundException notFoundException) {
            throw notFoundException;
        }
        if (throwable instanceof FeignException.NotFound || (throwable instanceof FeignException fe && fe.status() == 404)) {
            throw new ProductNotFoundException(productId);
        }
        throw new ProductServiceException("Product service is unavailable or returned an error: " + throwable.getMessage(), throwable);
    }
}
