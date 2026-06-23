package com.quickbite.order_service.configs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Order Service API", version = "v1", description = "API documentation for Order Service"))
public class SwaggerConfig {
    // Swagger UI is always enabled.
}
