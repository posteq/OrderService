package com.innowise.academy.orderservice.mapper;

import com.innowise.academy.orderservice.dto.response.order.OrderItemResponse;
import com.innowise.academy.orderservice.dto.response.order.OrderResponse;
import com.innowise.academy.orderservice.entity.Order;
import com.innowise.academy.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = IGNORE)
public interface OrderMapper{

    @Mapping(target = "itemId",source = "item.id")
    @Mapping(target = "itemName",source = "item.name")
    @Mapping(target = "price",source = "item.price")
    OrderItemResponse toResponse(OrderItem orderItem);

    @Mapping(target = "user",ignore = true)
    @Mapping(target = "userInfoAvailable" , ignore = true)
    OrderResponse toResponse(Order order);
}
