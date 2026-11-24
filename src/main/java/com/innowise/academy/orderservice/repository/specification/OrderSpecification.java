package com.innowise.academy.orderservice.repository.specification;

import com.innowise.academy.orderservice.entity.Order;
import com.innowise.academy.orderservice.entity.Status;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class OrderSpecification {

    public static Specification<Order> hasUserId(Long userId){
        return ((root, query, cb) -> userId == null ?
                null : cb.equal(root.get("userId"),userId));
    }

    public static Specification<Order> hasStatus(Status status){
        return ((root, query, cb) -> status == null ?
                null : cb.equal(root.get("status"),status));
    }

    public static Specification<Order> createdAfter(LocalDate from){
        return ((root, query, cb) -> from == null ?
                null : cb.greaterThanOrEqualTo(root.get("createdAt"),from.atStartOfDay())
        );
    }
    public static Specification<Order> createBefore(LocalDate to){
        return ((root, query, cb) -> to == null ?
                null : cb.lessThan(root.get("createdAt"),to.plusDays(1).atStartOfDay())
        );
    }

}
