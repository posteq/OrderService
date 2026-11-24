package com.innowise.academy.orderservice.repository;

import com.innowise.academy.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {
    boolean existsByItem_Id(Long id);
}
