package org.example.orderservice.models.dto.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderDetailRequest(
        @NotNull(message = "Product id is required")
        @Min(value = 1, message = "Product id must be greater than 0")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than 0")
        Integer quantity
) {
}
