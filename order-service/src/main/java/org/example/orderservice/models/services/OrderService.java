package org.example.orderservice.models.services;

import org.example.orderservice.models.dto.requests.CreateOrderRequest;
import org.example.orderservice.models.dto.responses.OrderResponse;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);
}
