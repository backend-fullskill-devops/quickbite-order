package com.quickbite.order_service.clients;

import lombok.Data;

@Data
public class RestaurantResponseDto {
    private Long id;
    private String name;
    private Long ownerId;
    private boolean open;
}
