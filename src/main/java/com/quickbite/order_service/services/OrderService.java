package com.quickbite.order_service.services;

import com.quickbite.order_service.clients.*;
import com.quickbite.order_service.models.dtos.*;
import com.quickbite.order_service.models.entities.*;
import com.quickbite.order_service.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    
    private final UserFeignClient userFeignClient;
    private final RestaurantFeignClient restaurantFeignClient;
    private final NotificationFeignClient notificationFeignClient;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}, restaurant: {}", request.getCustomerId(), request.getRestaurantId());

        // 1. Check restaurant status
        ApiResponse<Boolean> statusRes = restaurantFeignClient.getRestaurantStatus(request.getRestaurantId());
        if (statusRes == null || !Boolean.TRUE.equals(statusRes.getData())) {
            log.warn("Restaurant is closed or not found: {}", request.getRestaurantId());
            throw new RuntimeException("Restaurant is currently closed or not found.");
        }

        // 2. Fetch restaurant details
        ApiResponse<RestaurantResponseDto> restRes = restaurantFeignClient.getRestaurantById(request.getRestaurantId());
        if (restRes == null || restRes.getData() == null) {
            log.warn("Restaurant details not found for ID: {}", request.getRestaurantId());
            throw new RuntimeException("Restaurant details not found.");
        }
        RestaurantResponseDto restaurantDto = restRes.getData();

        // 3. Fetch customer details
        ApiResponse<UserResponseDto> userRes = userFeignClient.getUserById(request.getCustomerId());
        if (userRes == null || userRes.getData() == null) {
            log.warn("Customer details not found for ID: {}", request.getCustomerId());
            throw new RuntimeException("Customer details not found.");
        }
        UserResponseDto customerDto = userRes.getData();

        // 4. Fetch menu items to create snapshot and calculate prices
        ApiResponse<List<MenuItemResponseDto>> menuRes = restaurantFeignClient.getMenuItems(request.getRestaurantId());
        if (menuRes == null || menuRes.getData() == null) {
            log.warn("Menu items not found for restaurant ID: {}", request.getRestaurantId());
            throw new RuntimeException("Menu items not found.");
        }
        List<MenuItemResponseDto> menuItems = menuRes.getData();
        Map<Long, MenuItemResponseDto> menuItemMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItemResponseDto::getId, m -> m));

        BigDecimal itemsPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .customerName(customerDto.getFullName())
                .restaurantId(request.getRestaurantId())
                .merchantName(restaurantDto.getName())
                .deliveryAddressId(request.getDeliveryAddressId())
                .shippingFee(new BigDecimal("15000")) // Flat shipping fee
                .status(OrderStatus.PENDING)
                .build();

        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItemResponseDto menuItem = menuItemMap.get(itemReq.getMenuItemId());
            if (menuItem == null) {
                log.warn("Menu item {} does not belong to restaurant {}", itemReq.getMenuItemId(), request.getRestaurantId());
                throw new RuntimeException("Invalid menu item: " + itemReq.getMenuItemId());
            }
            if (!menuItem.isAvailable()) {
                log.warn("Menu item {} is currently unavailable", itemReq.getMenuItemId());
                throw new RuntimeException("Menu item is unavailable: " + menuItem.getName());
            }

            BigDecimal itemTotal = menuItem.getBasePrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            itemsPrice = itemsPrice.add(itemTotal);

            orderItems.add(OrderItem.builder()
                    .order(order)
                    .menuItemId(menuItem.getId())
                    .itemName(menuItem.getName())
                    .quantity(itemReq.getQuantity())
                    .price(menuItem.getBasePrice())
                    .build());
        }

        order.setItems(orderItems);
        order.setItemsPrice(itemsPrice);
        order.setTotalPrice(itemsPrice.add(order.getShippingFee()));

        // Save order (cascades order items)
        Order savedOrder = orderRepository.save(order);

        // Add initial status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .status(OrderStatus.PENDING)
                .note("Đơn hàng được khởi tạo thành công.")
                .changedAt(LocalDateTime.now())
                .build();
        statusHistoryRepository.save(history);

        // 5. Payment: Deduct Wallet (Saga step)
        String transactionId = "ORDER-" + savedOrder.getId() + "-DEDUCT";
        try {
            ApiResponse<Void> deductRes = userFeignClient.deductWallet(
                    savedOrder.getCustomerId(),
                    transactionId,
                    savedOrder.getTotalPrice()
            );

            if (deductRes == null || !Boolean.TRUE.equals(deductRes.getSuccess())) {
                String errorMsg = deductRes != null ? deductRes.getMessage() : "Unknown payment error";
                log.warn("Payment failed for Order ID {}: {}", savedOrder.getId(), errorMsg);
                failOrder(savedOrder, "Payment failed: " + errorMsg);
                throw new RuntimeException("Payment failed: " + errorMsg);
            }

            log.info("Payment succeeded for Order ID {}", savedOrder.getId());

            // Send notification to customer
            sendNotificationSilently(savedOrder.getCustomerId(),
                    "Đặt đơn hàng thành công",
                    "Đơn hàng #" + savedOrder.getId() + " trị giá " + savedOrder.getTotalPrice() + "đ đã được thanh toán và đang chờ nhà hàng xác nhận.",
                    "IN_APP"
            );

        } catch (Exception e) {
            log.error("Exception during payment for Order ID " + savedOrder.getId(), e);
            if (savedOrder.getStatus() == OrderStatus.PENDING) {
                failOrder(savedOrder, "Payment failed due to internal error: " + e.getMessage());
            }
            throw new RuntimeException("Order creation failed: " + e.getMessage());
        }

        return mapToOrderResponse(savedOrder);
    }

    private void failOrder(Order order, String reason) {
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.FAILED)
                .note(reason)
                .changedAt(LocalDateTime.now())
                .build();
        statusHistoryRepository.save(history);
        log.warn("Order {} marked as FAILED due to: {}", order.getId(), reason);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse acceptOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only PENDING orders can be accepted. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.ACCEPTED);
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.ACCEPTED)
                .note("Nhà hàng đã xác nhận chuẩn bị món.")
                .changedAt(LocalDateTime.now())
                .build();
        statusHistoryRepository.save(history);

        log.info("Order ID {} accepted by restaurant", order.getId());

        sendNotificationSilently(order.getCustomerId(),
                "Đơn hàng được chấp nhận",
                "Nhà hàng đã xác nhận đơn hàng #" + order.getId() + " và đang chuẩn bị món ăn.",
                "IN_APP"
        );

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse rejectOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only PENDING orders can be rejected. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .note("Nhà hàng từ chối nhận đơn.")
                .changedAt(LocalDateTime.now())
                .build();
        statusHistoryRepository.save(history);

        log.warn("Order ID {} rejected by restaurant. Starting compensating transaction (refund)...", order.getId());

        // Compensating Transaction (Refund)
        String refundTxId = "ORDER-" + order.getId() + "-REFUND";
        try {
            ApiResponse<Void> refundRes = userFeignClient.refundWallet(
                    order.getCustomerId(),
                    refundTxId,
                    order.getTotalPrice()
            );
            if (refundRes == null || !Boolean.TRUE.equals(refundRes.getSuccess())) {
                log.error("Compensating transaction (refund) failed for Order ID {}: {}", order.getId(),
                        refundRes != null ? refundRes.getMessage() : "unknown error");
            } else {
                log.info("Compensating transaction (refund) succeeded for Order ID {}", order.getId());
            }
        } catch (Exception ex) {
            log.error("Compensating transaction (refund) encountered error for Order ID " + order.getId(), ex);
        }

        sendNotificationSilently(order.getCustomerId(),
                "Đơn hàng bị hủy",
                "Đơn hàng #" + order.getId() + " của bạn đã bị nhà hàng từ chối. Số tiền " + order.getTotalPrice() + "đ đã được hoàn lại vào ví.",
                "IN_APP"
        );

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse shipOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new RuntimeException("Only ACCEPTED orders can start shipping. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPING);
        order.setDriverId(999L); // Simulated driver
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.SHIPPING)
                .note("Tài xế đã nhận đơn hàng và đang giao đi.")
                .changedAt(LocalDateTime.now())
                .build();
        statusHistoryRepository.save(history);

        log.info("Order ID {} is now SHIPPING", order.getId());

        sendNotificationSilently(order.getCustomerId(),
                "Đơn hàng đang được giao",
                "Tài xế (ID 999) đã nhận đơn hàng #" + order.getId() + " và đang trên đường giao tới bạn.",
                "IN_APP"
        );

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse deliverOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new RuntimeException("Only SHIPPING orders can be delivered. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.DELIVERED)
                .note("Giao hàng thành công.")
                .changedAt(LocalDateTime.now())
                .build();
        statusHistoryRepository.save(history);

        log.info("Order ID {} delivered successfully", order.getId());

        sendNotificationSilently(order.getCustomerId(),
                "Giao hàng thành công",
                "Đơn hàng #" + order.getId() + " đã được giao thành công. Chúc bạn ngon miệng!",
                "IN_APP"
        );

        return mapToOrderResponse(order);
    }

    private void sendNotificationSilently(Long userId, String title, String content, String type) {
        try {
            NotificationRequestDto requestDto = NotificationRequestDto.builder()
                    .userId(userId)
                    .title(title)
                    .content(content)
                    .type(type)
                    .build();
            notificationFeignClient.sendNotification(requestDto);
        } catch (Exception ex) {
            log.error("Failed to send notification via Feign for user " + userId, ex);
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            itemResponses = order.getItems().stream()
                    .map(item -> OrderItemResponse.builder()
                            .id(item.getId())
                            .menuItemId(item.getMenuItemId())
                            .itemName(item.getItemName())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .build())
                    .collect(Collectors.toList());
        }

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .restaurantId(order.getRestaurantId())
                .merchantName(order.getMerchantName())
                .driverId(order.getDriverId())
                .deliveryAddressId(order.getDeliveryAddressId())
                .itemsPrice(order.getItemsPrice())
                .shippingFee(order.getShippingFee())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .items(itemResponses)
                .build();
    }
}
