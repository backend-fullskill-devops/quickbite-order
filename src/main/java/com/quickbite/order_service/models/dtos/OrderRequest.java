package com.quickbite.order_service.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;

    @NotNull(message = "Delivery address ID is required")
    private Long deliveryAddressId;

    @NotEmpty(message = "Order items list cannot be empty")
    private List<@Valid OrderItemRequest> items;
}
