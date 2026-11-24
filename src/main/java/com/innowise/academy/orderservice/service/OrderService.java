package com.innowise.academy.orderservice.service;

import com.innowise.academy.orderservice.dto.request.order.OrderCreateRequest;
import com.innowise.academy.orderservice.dto.request.order.OrderFilter;
import com.innowise.academy.orderservice.dto.request.order.OrderItemRequest;
import com.innowise.academy.orderservice.dto.request.order.OrderUpdateRequest;
import com.innowise.academy.orderservice.dto.response.order.OrderResponse;
import com.innowise.academy.orderservice.entity.Item;
import com.innowise.academy.orderservice.entity.Order;
import com.innowise.academy.orderservice.entity.OrderItem;
import com.innowise.academy.orderservice.entity.Status;
import com.innowise.academy.orderservice.exception.InvalidOrderStateException;
import com.innowise.academy.orderservice.exception.InvalidRequestException;
import com.innowise.academy.orderservice.exception.ItemNotFoundException;
import com.innowise.academy.orderservice.exception.OrderNotFoundException;
import com.innowise.academy.orderservice.mapper.OrderMapper;
import com.innowise.academy.orderservice.repository.ItemRepository;
import com.innowise.academy.orderservice.repository.OrderItemRepository;
import com.innowise.academy.orderservice.repository.OrderRepository;
import com.innowise.academy.orderservice.repository.specification.OrderSpecification;
import com.innowise.academy.orderservice.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {


    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper mapper;

    private static final Set<String> SORT_FIELDS = Set.of("userId","status","createdAt");

    @Transactional
    public OrderResponse create(Long userId, OrderCreateRequest request){
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(Status.CREATED);
        addItems(order,request.items());
        return mapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long orderId, CurrentUser user){
        Order order = findAllowedToUser(orderId, user);
        return mapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllByUser(Long userId){
        return orderRepository.findAllByUserId(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long orderId, CurrentUser user){
        Order order = findAllowedToUser(orderId, user);
        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> search(OrderFilter filter, int page, int size, String sortBy, String sortDir){
        LocalDate from = filter.startDate();
        LocalDate to = filter.finishDate();
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("startDate must not be after endDate");
        }

        if(!SORT_FIELDS.contains(sortBy)){
            throw new InvalidRequestException("Unsupported sort field: " + sortBy);
        }

        Specification<Order> orderSpecification = Specification.where(
                OrderSpecification.hasUserId(filter.userId()))
                .and(OrderSpecification.hasStatus(filter.status()))
                .and(OrderSpecification.createBefore(filter.finishDate()))
                .and(OrderSpecification.createdAfter(filter.startDate()));

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir),sortBy);
        PageRequest pageRequest = PageRequest.of(Math.max(page,0), Math.max(size,1), sort);

        return orderRepository.findAll(orderSpecification, pageRequest)
                .map(mapper::toResponse);
    }

    @Transactional
    public OrderResponse update(Long orderId, OrderUpdateRequest request){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if(request.items() != null){
            if(order.getStatus() != Status.CREATED){
                throw new InvalidOrderStateException("Items can be changed only while the order is CREATED");
            }
            replaceItems(order,request.items());
        }
        if(request.status() != null){
            if(request.status().ordinal() < order.getStatus().ordinal()){
                throw new InvalidOrderStateException("Status cannot move back to " + request.status());
            }
            order.setStatus(request.status());
        }
        return mapper.toResponse(order);
    }

    private void replaceItems(Order order, List<OrderItemRequest> request) {
        List<OrderItem> old = new ArrayList<>(order.getItems());
        order.getItems().clear();
        orderItemRepository.deleteAll(old);
        addItems(order,request);
    }


    private Order findAllowedToUser(Long orderId, CurrentUser user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if(!user.admin() && !order.getUserId().equals(user.userId())){
            throw new OrderNotFoundException(orderId);
        }
        return order;
    }

    private void addItems(Order order, List<OrderItemRequest> request) {
        Map<Long, Integer> itemsQuantity = new LinkedHashMap<>();
        for (OrderItemRequest i : request) {
            itemsQuantity.merge(i.itemId(), i.quantity(), Integer::sum);
        }

        Map<Long, Item> itemMap = itemRepository.findAllById(itemsQuantity.keySet())
                .stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));

        BigDecimal totalPrice = calculateTotalPrice(order, itemsQuantity, itemMap);
        order.setTotalPrice(totalPrice);
    }

    private static BigDecimal calculateTotalPrice(Order order, Map<Long, Integer> itemsQuantity, Map<Long, Item> itemMap) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (Map.Entry<Long,Integer> entry : itemsQuantity.entrySet()) {
            Long itemId = entry.getKey();
            Integer itemQuantity = entry.getValue();
            Item item = itemMap.get(itemId);

            if (item == null) {
                throw new ItemNotFoundException(itemId);
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(item);
            orderItem.setQuantity(itemQuantity);
            order.getItems().add(orderItem);

            totalPrice = totalPrice.add(item.getPrice().multiply(BigDecimal.valueOf(itemQuantity)));
        }
        return totalPrice;
    }
}
