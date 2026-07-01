package com.quickbite.order_service.models.dtos;

import com.quickbite.order_service.models.entities.OrderStatus;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long restaurantId;
    private String merchantName;
    private Long driverId;
    private Long deliveryAddressId;
    private BigDecimal itemsPrice;
    private BigDecimal shippingFee;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private List<OrderItemResponse> items;
}
