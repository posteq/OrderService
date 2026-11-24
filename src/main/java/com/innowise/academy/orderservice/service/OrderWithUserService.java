package com.innowise.academy.orderservice.service;

import com.innowise.academy.orderservice.dto.request.order.OrderCreateRequest;
import com.innowise.academy.orderservice.dto.request.order.OrderFilter;
import com.innowise.academy.orderservice.dto.request.order.OrderUpdateRequest;
import com.innowise.academy.orderservice.dto.response.UserInfo;
import com.innowise.academy.orderservice.dto.response.order.OrderResponse;
import com.innowise.academy.orderservice.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderWithUserService {

    private final OrderService orderService;
    private final UserProviderService userProviderService;

    public OrderResponse get(Long orderId, CurrentUser caller){
        OrderResponse order = orderService.get(orderId, caller);
        return withUser(order,caller);
    }

    public OrderResponse create(OrderCreateRequest order, CurrentUser caller){
        return withUser(orderService.create(caller.userId(), order),caller);
    }

    public List<OrderResponse> findByUser(Long userId, CurrentUser caller){
        Optional<UserInfo> user = fetchUser(userId, caller);

        return orderService.getAllByUser(userId)
                .stream()
                .map( order -> attach(order,user))
                .toList();
    }

    public Page<OrderResponse> search(OrderFilter filter, int page, int size, String sortBy, String sortDir, CurrentUser caller){
        Page<OrderResponse> orders = orderService.search(filter, page, size, sortBy, sortDir);
        Map<Long, Optional<UserInfo>> users = new HashMap<>();
        return orders.map(order -> attach(order,
                users.computeIfAbsent(order.userId(), id -> fetchUser(id, caller))));

    }

    public void delete(Long orderId, CurrentUser caller){
        orderService.delete(orderId,caller);
    }

    public OrderResponse update(Long orderId, OrderUpdateRequest order, CurrentUser caller){
        return withUser(orderService.update(orderId,order),caller);
    }

    private OrderResponse withUser(OrderResponse order, CurrentUser user) {
        return attach(order,fetchUser(order.userId(),user));
    }

    private OrderResponse attach(OrderResponse order, Optional<UserInfo> user) {
        return user.map(info -> order.getWithUser(info,true))
                .orElseGet(() -> order.getWithUser(null,false));
    }

    private Optional<UserInfo> fetchUser(Long userId, CurrentUser user){
        try {
            return Optional.ofNullable(userProviderService.getUserInfo(userId, user.token()));
        }catch (RuntimeException e){
            return Optional.empty();
        }
    }
}
