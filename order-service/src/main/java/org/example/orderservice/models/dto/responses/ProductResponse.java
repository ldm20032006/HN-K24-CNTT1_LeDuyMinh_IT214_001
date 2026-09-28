package org.example.orderservice.models.dto.responses;

public record ProductResponse(
        Long id,
        String name,
        Double price
) {
}
