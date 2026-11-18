package com.innowise.academy.orderservice.dto.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull
        Long itemId,
        @NotNull
        @Min(1)
        Integer quantity
) {
}
