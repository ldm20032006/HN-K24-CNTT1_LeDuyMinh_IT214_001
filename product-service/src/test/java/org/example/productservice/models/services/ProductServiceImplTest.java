package org.example.productservice.models.services;

import org.example.productservice.exceptions.ProductNotFoundException;
import org.example.productservice.models.entities.Product;
import org.example.productservice.models.repositories.ProductRepository;
import org.example.productservice.models.services.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {

    private ProductRepository productRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productService = new ProductServiceImpl(productRepository);
    }

    @Test
    void getProductByIdReturnsProductWhenItExists() {
        Product product = new Product(1L, "Laptop", "Laptop văn phòng", new BigDecimal("15000000.00"), 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(1L);

        assertSame(product, result);
        verify(productRepository).findById(1L);
    }

    @Test
    void getProductByIdThrowsExceptionWhenItDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(99L)
        );

        assertEquals("Không tìm thấy sản phẩm với id: 99", exception.getMessage());
    }

    @Test
    void getProductByIdUsesProductsCacheWithIdAsKey() throws NoSuchMethodException {
        Cacheable cacheable = ProductServiceImpl.class
                .getMethod("getProductById", Long.class)
                .getAnnotation(Cacheable.class);

        assertEquals("products", cacheable.cacheNames()[0]);
        assertEquals("#id", cacheable.key());
        assertTrue(cacheable.sync());
        assertTrue(Serializable.class.isAssignableFrom(Product.class));
    }
}
