package com.quickbite.order_service.models.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "delivery_address_id", nullable = false)
    private Long deliveryAddressId;

    @Column(name = "items_price", nullable = false)
    private BigDecimal itemsPrice;

    @Column(name = "shipping_fee", nullable = false)
    private BigDecimal shippingFee;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStatusHistory> statusHistory;
}
