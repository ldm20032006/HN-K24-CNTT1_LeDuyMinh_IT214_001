package org.example.orderservice.models.repositories;

import org.example.orderservice.models.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
