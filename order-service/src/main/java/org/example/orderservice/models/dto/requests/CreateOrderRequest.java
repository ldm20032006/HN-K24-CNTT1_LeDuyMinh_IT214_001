package org.example.orderservice.models.dto.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 255, message = "Customer name must not exceed 255 characters")
        String customerName,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email is invalid")
        String customerEmail,

        @NotEmpty(message = "Order must contain at least one item")
        List<@Valid CreateOrderDetailRequest> items
) {
}
