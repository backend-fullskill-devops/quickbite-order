package com.quickbite.order_service.clients;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDto {
    private Long userId;
    private String title;
    private String content;
    private String type; // IN_APP, EMAIL, SMS
}
