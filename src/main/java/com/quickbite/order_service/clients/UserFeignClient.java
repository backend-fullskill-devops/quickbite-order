package com.quickbite.order_service.clients;

import com.quickbite.order_service.models.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@FeignClient(name = "user-service", url = "${feign.client.config.user-service.url}")
public interface UserFeignClient {
        @GetMapping("/api/v1/users/{id}")
        ApiResponse<UserResponseDto> getUserById(@PathVariable("id") Long id);

        @PostMapping("/api/v1/users/{id}/wallet/deduct")
        ApiResponse<Void> deductWallet(
                        @PathVariable("id") Long id,
                        @RequestParam("transactionId") String transactionId,
                        @RequestParam("amount") BigDecimal amount);

        @PostMapping("/api/v1/users/{id}/wallet/refund")
        ApiResponse<Void> refundWallet(
                        @PathVariable("id") Long id,
                        @RequestParam("transactionId") String transactionId,
                        @RequestParam("amount") BigDecimal amount);
}
