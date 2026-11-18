package com.innowise.academy.orderservice.dto.response.item;

import java.math.BigDecimal;

public record ItemResponse(
        Long id,
        String name,
        BigDecimal price
) {
}
