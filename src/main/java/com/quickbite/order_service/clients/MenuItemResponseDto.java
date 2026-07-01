package com.quickbite.order_service.clients;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MenuItemResponseDto {
    private Long id;
    private String name;
    private BigDecimal basePrice;
    private boolean available;
}
