package com.quickbite.order_service.models.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {
    @NotNull(message = "Menu item ID is required")
    private Long menuItemId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
