package org.example.orderservice.models.dto.responses;

import org.example.orderservice.models.constants.OrderStatus;

import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        Double total,
        OrderStatus status,
        List<OrderDetailResponse> items
) {
}
