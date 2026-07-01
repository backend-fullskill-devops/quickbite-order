package com.quickbite.order_service.clients;

import com.quickbite.order_service.models.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${feign.client.config.notification-service.url}")
public interface NotificationFeignClient {
    @PostMapping("/api/v1/notifications")
    ApiResponse<Void> sendNotification(@RequestBody NotificationRequestDto request);
}
