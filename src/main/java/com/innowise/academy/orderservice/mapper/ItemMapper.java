package com.innowise.academy.orderservice.mapper;

import com.innowise.academy.orderservice.dto.request.item.ItemRequest;
import com.innowise.academy.orderservice.dto.response.item.ItemResponse;
import com.innowise.academy.orderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = IGNORE)
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Item toEntity(ItemRequest request);

    ItemResponse toResponse(Item item);

    @Mapping(target = "id",ignore = true)
    @Mapping(target = "createdAt",ignore = true)
    @Mapping(target = "updatedAt",ignore = true)
    void updateEntity(ItemRequest request, @MappingTarget Item item);
}
