package org.example.orderservice.models.services.impl;

import org.example.orderservice.exceptions.ProductNotFoundException;
import org.example.orderservice.models.constants.OrderStatus;
import org.example.orderservice.models.dto.requests.CreateOrderDetailRequest;
import org.example.orderservice.models.dto.requests.CreateOrderRequest;
import org.example.orderservice.models.dto.responses.OrderDetailResponse;
import org.example.orderservice.models.dto.responses.OrderResponse;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.example.orderservice.models.entities.Order;
import org.example.orderservice.models.entities.OrderDetail;
import org.example.orderservice.models.repositories.OrderDetailRepository;
import org.example.orderservice.models.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    private OrderRepository orderRepository;
    private OrderDetailRepository orderDetailRepository;
    private ProductGatewayService productGatewayService;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderDetailRepository = mock(OrderDetailRepository.class);
        productGatewayService = mock(ProductGatewayService.class);
        //noinspection unchecked
        kafkaTemplate = mock(KafkaTemplate.class);
        orderService = new OrderServiceImpl(orderRepository, orderDetailRepository, productGatewayService, kafkaTemplate);
    }

    @Test
    @DisplayName("Tạo đơn hàng thành công: tính đúng tổng tiền, lưu PENDING, liên kết OrderDetail, gửi kafka event và trả về OrderResponse")
    void createOrder_Success() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyen Van A",
                "vana@example.com",
                List.of(
                        new CreateOrderDetailRequest(1L, 2),
                        new CreateOrderDetailRequest(2L, 3)
                )
        );

        when(productGatewayService.getProductById(1L))
                .thenReturn(new ProductResponse(1L, "Laptop Dell", 1500.0));
        when(productGatewayService.getProductById(2L))
                .thenReturn(new ProductResponse(2L, "Chuột không dây", 25.5));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        when(orderDetailRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<OrderDetail> details = invocation.getArgument(0);
            long idCounter = 1L;
            for (OrderDetail detail : details) {
                detail.setId(idCounter++);
            }
            return details;
        });

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert: 1500.0 * 2 + 25.5 * 3 = 3000.0 + 76.5 = 3076.5
        assertEquals(100L, response.id());
        assertEquals("Nguyen Van A", response.customerName());
        assertEquals(3076.5, response.total());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(2, response.items().size());

        OrderDetailResponse item1 = response.items().get(0);
        assertEquals(1L, item1.id());
        assertEquals(1L, item1.productId());
        assertEquals("Laptop Dell", item1.productName());
        assertEquals(2, item1.quantity());
        assertEquals(1500.0, item1.unitPrice());
        assertEquals(3000.0, item1.subtotal());

        OrderDetailResponse item2 = response.items().get(1);
        assertEquals(2L, item2.id());
        assertEquals(2L, item2.productId());
        assertEquals("Chuột không dây", item2.productName());
        assertEquals(3, item2.quantity());
        assertEquals(25.5, item2.unitPrice());
        assertEquals(76.5, item2.subtotal());

        // Verify order saved with valid details
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals("Nguyen Van A", savedOrder.getCustomerName());
        assertEquals(3076.5, savedOrder.getTotal());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());

        // Verify order details saved with link to saved order
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderDetail>> detailsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderDetailRepository).saveAll(detailsCaptor.capture());
        List<OrderDetail> savedDetails = detailsCaptor.getValue();
        assertEquals(2, savedDetails.size());
        assertEquals(savedOrder, savedDetails.get(0).getOrder());
        assertEquals(savedOrder, savedDetails.get(1).getOrder());

        // Verify Kafka event published to topic with customer email
        verify(kafkaTemplate).send("order-created", "vana@example.com");
    }

    @Test
    @DisplayName("Ném ProductNotFoundException khi sản phẩm không tồn tại, không lưu order và không gửi Kafka")
    void createOrder_ThrowsException_WhenProductNotFound() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyen Van A",
                "vana@example.com",
                List.of(new CreateOrderDetailRequest(999L, 1))
        );

        when(productGatewayService.getProductById(999L)).thenReturn(null);

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
        verify(orderDetailRepository, never()).saveAll(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Phương thức createOrder phải có annotation @Transactional")
    void createOrder_HasTransactionalAnnotation() throws NoSuchMethodException {
        Transactional transactional = OrderServiceImpl.class
                .getMethod("createOrder", CreateOrderRequest.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional, "createOrder phải được gắn annotation @Transactional");
    }
}
