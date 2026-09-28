package org.example.orderservice.models.dto.responses;

public record OrderDetailResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        Double unitPrice,
        Double subtotal
) {
}
