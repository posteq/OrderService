package com.innowise.academy.orderservice.dto.response.order;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long itemId,
        String itemName,
        BigDecimal price,
        Integer quantity
) {
}
