package com.innowise.academy.orderservice.repository;

import com.innowise.academy.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long>, JpaSpecificationExecutor<Order> {

    List<Order> findAllByUserId(Long userId);
}
