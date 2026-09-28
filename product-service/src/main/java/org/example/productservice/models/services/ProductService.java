package org.example.productservice.models.services;

import org.example.productservice.models.entities.Product;

public interface ProductService {
    Product getProductById(Long id);
}
