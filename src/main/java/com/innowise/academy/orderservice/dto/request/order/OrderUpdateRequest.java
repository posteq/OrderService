package com.innowise.academy.orderservice.dto.request.order;

import com.innowise.academy.orderservice.entity.Status;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderUpdateRequest(
        Status status,
        @Size(min = 1)
        List<OrderItemRequest> items
) {
}
