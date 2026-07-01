package com.quickbite.order_service.models.dtos;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long id;
    private Long menuItemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal price;
}
