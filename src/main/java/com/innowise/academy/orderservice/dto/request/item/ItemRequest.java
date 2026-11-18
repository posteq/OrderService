package com.innowise.academy.orderservice.dto.request.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemRequest(@NotBlank
                          String name,
                          @NotNull
                          @DecimalMin("0.0")
                          BigDecimal price) {
}
