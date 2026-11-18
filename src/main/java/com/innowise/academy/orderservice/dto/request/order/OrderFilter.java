package com.innowise.academy.orderservice.dto.request.order;

import com.innowise.academy.orderservice.entity.Status;

import java.time.LocalDate;

public record OrderFilter(
        Long userId,
        Status status,
        LocalDate startDate,
        LocalDate finishDate
) {
}
