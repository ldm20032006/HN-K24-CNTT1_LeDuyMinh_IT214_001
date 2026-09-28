package org.example.orderservice.models.services.impl;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.example.orderservice.clients.ProductClient;
import org.example.orderservice.exceptions.ProductNotFoundException;
import org.example.orderservice.exceptions.ProductServiceException;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductGatewayServiceTest {

    private ProductClient productClient;
    private ProductGatewayService productGatewayService;

    @BeforeEach
    void setUp() {
        productClient = mock(ProductClient.class);
        productGatewayService = new ProductGatewayService(productClient);
    }

    @Test
    @DisplayName("Phương thức getProductById phải được gắn annotation @CircuitBreaker với name=productService và fallbackMethod")
    void getProductById_HasCircuitBreakerAnnotation() throws NoSuchMethodException {
        CircuitBreaker circuitBreaker = ProductGatewayService.class
                .getMethod("getProductById", Long.class)
                .getAnnotation(CircuitBreaker.class);

        assertNotNull(circuitBreaker, "Phải có annotation @CircuitBreaker");
        assertEquals("productService", circuitBreaker.name());
        assertEquals("getProductByIdFallback", circuitBreaker.fallbackMethod());
    }

    @Test
    @DisplayName("getProductById trả về ProductResponse khi dữ liệu hợp lệ")
    void getProductById_ReturnsProduct_WhenValid() {
        ProductResponse expected = new ProductResponse(1L, "Laptop", 1200.0);
        when(productClient.getProductById(1L)).thenReturn(expected);

        ProductResponse actual = productGatewayService.getProductById(1L);

        assertEquals(expected, actual);
        verify(productClient).getProductById(1L);
    }

    @Test
    @DisplayName("getProductById ném ProductNotFoundException khi sản phẩm trả về null hoặc id null")
    void getProductById_ThrowsProductNotFoundException_WhenNullOrInvalid() {
        when(productClient.getProductById(1L)).thenReturn(null);
        assertThrows(ProductNotFoundException.class, () -> productGatewayService.getProductById(1L));

        when(productClient.getProductById(2L)).thenReturn(new ProductResponse(null, "No ID", 10.0));
        assertThrows(ProductNotFoundException.class, () -> productGatewayService.getProductById(2L));
    }

    @Test
    @DisplayName("Fallback ném ProductNotFoundException khi throwable là ProductNotFoundException")
    void fallback_ThrowsProductNotFoundException_WhenProductNotFound() {
        ProductNotFoundException exception = new ProductNotFoundException(5L);

        ProductNotFoundException thrown = assertThrows(
                ProductNotFoundException.class,
                () -> productGatewayService.getProductByIdFallback(5L, exception)
        );

        assertSame(exception, thrown);
    }

    @Test
    @DisplayName("Fallback ném ProductNotFoundException khi throwable là FeignException.NotFound (404)")
    void fallback_ThrowsProductNotFoundException_WhenFeignNotFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/products/5",
                Collections.emptyMap(), null, new RequestTemplate());
        FeignException.NotFound feignNotFound = new FeignException.NotFound("Not found", request, null, null);

        ProductNotFoundException thrown = assertThrows(
                ProductNotFoundException.class,
                () -> productGatewayService.getProductByIdFallback(5L, feignNotFound)
        );

        assertTrue(thrown.getMessage().contains("5"));
    }

    @Test
    @DisplayName("Fallback ném ProductServiceException khi product service lỗi hoặc không khả dụng")
    void fallback_ThrowsProductServiceException_WhenServiceUnavailableOrError() {
        RuntimeException serviceDownException = new RuntimeException("Connection refused");

        ProductServiceException thrown = assertThrows(
                ProductServiceException.class,
                () -> productGatewayService.getProductByIdFallback(10L, serviceDownException)
        );

        assertNotNull(thrown.getMessage());
        assertSame(serviceDownException, thrown.getCause());
    }
}
