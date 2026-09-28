package org.example.orderservice.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.orderservice.models.constants.OrderStatus;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "total")
    private Double total;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

}
