package com.innowise.academy.orderservice.dto.response.order;

import com.innowise.academy.orderservice.dto.response.UserInfo;
import com.innowise.academy.orderservice.entity.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        Status status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        UserInfo user,
        boolean userInfoAvailable
) {
    public OrderResponse getWithUser(UserInfo user,boolean userInfoAvailable){
        return new OrderResponse(id,userId,status,totalPrice,createdAt,items,user,userInfoAvailable);
    }
}
