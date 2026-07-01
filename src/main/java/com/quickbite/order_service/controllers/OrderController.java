package com.quickbite.order_service.controllers;

import com.quickbite.order_service.models.dtos.ApiResponse;
import com.quickbite.order_service.models.dtos.OrderRequest;
import com.quickbite.order_service.models.dtos.OrderResponse;
import com.quickbite.order_service.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptOrder(@PathVariable Long id) {
        OrderResponse response = orderService.acceptOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order accepted by restaurant", response));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrder(@PathVariable Long id) {
        OrderResponse response = orderService.rejectOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order rejected by restaurant", response));
    }

    @PutMapping("/{id}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(@PathVariable Long id) {
        OrderResponse response = orderService.shipOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order is shipping", response));
    }

    @PutMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> deliverOrder(@PathVariable Long id) {
        OrderResponse response = orderService.deliverOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order delivered successfully", response));
    }
}
