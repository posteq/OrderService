package com.innowise.academy.orderservice.dto.response;

public record UserInfo(
        Long id,
        String firstname,
        String surname,
        String email
) {
}
