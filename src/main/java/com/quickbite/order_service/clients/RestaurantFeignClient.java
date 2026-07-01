package com.quickbite.order_service.clients;

import com.quickbite.order_service.models.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "restaurant-service", url = "${feign.client.config.restaurant-service.url}")
public interface RestaurantFeignClient {
    @GetMapping("/api/v1/restaurants/{id}")
    ApiResponse<RestaurantResponseDto> getRestaurantById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/restaurants/{id}/status")
    ApiResponse<Boolean> getRestaurantStatus(@PathVariable("id") Long id);

    @GetMapping("/api/v1/restaurants/{id}/menu-items")
    ApiResponse<List<MenuItemResponseDto>> getMenuItems(@PathVariable("id") Long id);
}
