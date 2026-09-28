package org.example.orderservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import org.example.orderservice.models.services.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductGatewayService productGatewayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Setter
    @Value("${notification.kafka.order-created-topic:${order.kafka.topic:${ORDER_CREATED_TOPIC:order-created}}}")
    private String orderCreatedTopic = "order-created";

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        record ItemData(CreateOrderDetailRequest itemRequest, ProductResponse product, BigDecimal subtotal) {}
        List<ItemData> itemsData = new ArrayList<>();

        // 1. Duyệt toàn bộ item của request và gọi product service để lấy thông tin mới nhất
        for (CreateOrderDetailRequest item : request.items()) {
            ProductResponse product = productGatewayService.getProductById(item.productId());
            if (product == null) {
                throw new ProductNotFoundException(item.productId());
            }

            // 2. Không sử dụng giá do client tự truyền, lấy đơn giá từ product service và tính thành tiền
            BigDecimal unitPrice = BigDecimal.valueOf(product.price());
            BigDecimal quantity = BigDecimal.valueOf(item.quantity());
            BigDecimal subtotal = unitPrice.multiply(quantity);

            totalAmount = totalAmount.add(subtotal);
            itemsData.add(new ItemData(item, product, subtotal));
        }

        // 3. Tạo và lưu Order với tên khách hàng hợp lệ, tổng tiền đã tính và trạng thái PENDING
        Order order = Order.builder()
                .customerName(request.customerName())
                .total(totalAmount.doubleValue())
                .status(OrderStatus.PENDING)
                .build();
        Order savedOrder = orderRepository.save(order);

        // 4. Tạo, liên kết và lưu đầy đủ các OrderDetail với đơn hàng tương ứng
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ItemData data : itemsData) {
            OrderDetail orderDetail = OrderDetail.builder()
                    .order(savedOrder)
                    .productId(data.itemRequest().productId())
                    .quantity(data.itemRequest().quantity())
                    .unitPrice(data.product().price())
                    .build();
            orderDetails.add(orderDetail);
        }
        List<OrderDetail> savedDetails = orderDetailRepository.saveAll(orderDetails);

        // 5. Chỉ phát sự kiện khi đơn hàng được tạo thành công, gửi customerEmail đến topic
        String topic = (orderCreatedTopic != null && !orderCreatedTopic.isBlank()) ? orderCreatedTopic : "order-created";
        kafkaTemplate.send(topic, request.customerEmail());

        // 6. Trả về đúng OrderResponse, bao gồm thông tin đơn hàng và danh sách chi tiết
        List<OrderDetailResponse> detailResponses = new ArrayList<>();
        for (int i = 0; i < savedDetails.size(); i++) {
            OrderDetail savedDetail = savedDetails.get(i);
            ItemData data = itemsData.get(i);
            detailResponses.add(new OrderDetailResponse(
                    savedDetail.getId(),
                    savedDetail.getProductId(),
                    data.product().name(),
                    savedDetail.getQuantity(),
                    savedDetail.getUnitPrice(),
                    data.subtotal().doubleValue()
            ));
        }

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getCustomerName(),
                savedOrder.getTotal(),
                savedOrder.getStatus(),
                detailResponses
        );
    }
}
