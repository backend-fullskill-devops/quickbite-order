package com.quickbite.order_service.clients;

import lombok.Data;

@Data
public class UserResponseDto {
    private Long id;
    private String fullName;
    private String username;
    private String role;
}
