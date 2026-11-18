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
import com.innowise.academy.orderservice.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final CurrentUser OWNER = new CurrentUser(7L, false, "token");
    private static final CurrentUser STRANGER = new CurrentUser(8L, false, "token");
    private static final CurrentUser ADMIN = new CurrentUser(99L, true, "token");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ItemRepository itemRepository;

    private OrderService service;

    private static final Item mouse = item(1L, "Mouse", "10.50");
    private static final Item pad = item(2L, "Pad", "2.50");

    @BeforeEach
    void setUp() {
        service = new OrderService(orderItemRepository, orderRepository,
                itemRepository, Mappers.getMapper(OrderMapper.class));
    }


    private static Item item(Long id, String name, String price) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return item;
    }

    private static Order order(Long id, Long userId, Status status, Item item, int quantity) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        OrderItem line = new OrderItem();
        line.setOrder(order);
        line.setItem(item);
        line.setQuantity(quantity);
        order.setItems(new ArrayList<>(List.of(line)));
        return order;
    }

    @Test
    void totalComesFromDatabasePrices_andSameItemLinesAreMerged() {
        Item pad = item(2L, "Pad", "3.00");
        when(itemRepository.findAllById(any())).thenReturn(List.of(mouse, pad));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = service.create(7L, new OrderCreateRequest(List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1),
                new OrderItemRequest(1L, 3))));

        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo(Status.CREATED);
        assertThat(response.totalPrice()).isEqualByComparingTo("55.50");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).itemId()).isEqualTo(1L);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(response.userInfoAvailable()).isFalse();
    }

    @Test
    void unknownItem_throws_andNothingIsSaved() {
        when(itemRepository.findAllById(any()))
                .thenReturn(List.of(mouse));

        assertThatThrownBy(() -> service.create(7L,
                new OrderCreateRequest(List.of(
                        new OrderItemRequest(1L, 1),
                        new OrderItemRequest(2L, 1)))
        ))
                .isInstanceOf(ItemNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }


    @Test
    void owner_andAdmin_canSeeTheOrder() {
        when(orderRepository.findById(5L))
                .thenReturn(Optional.of(order(5L, 7L, Status.CREATED, mouse, 1)));

        assertThat(service.get(5L, OWNER).id()).isEqualTo(5L);
        assertThat(service.get(5L, ADMIN).id()).isEqualTo(5L);
    }

    @Test
    void orderOfAnotherUser_isNotFound_forAUser() {
        when(orderRepository.findById(5L))
                .thenReturn(Optional.of(order(5L, 7L, Status.CREATED, mouse, 1)));

        assertThatThrownBy(() -> service.get(5L, STRANGER))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void missingOrder_isNotFound() {
        when(orderRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(5L, ADMIN))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void buildsASortedPage_withNormalizedPageSettings() {
        when(orderRepository.findAll(ArgumentMatchers.<Specification<Order>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order(5L, 7L, Status.PAID, mouse, 1))));

        Page<OrderResponse> page = service.search(
                new OrderFilter(7L, Status.PAID, LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 12, 31)),
                1, 1000,"createdAt","ASC");

        assertThat(page.getContent()).hasSize(1);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(orderRepository).findAll(ArgumentMatchers.<Specification<Order>>any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(1000);
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")
                .getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void startAfterEnd_isRejected() {
        OrderFilter filter = new OrderFilter(null, null, LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 1, 1));

        assertThatThrownBy(() -> service.search(filter, 0, 10,"createdAt","ASC"))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(orderRepository);
    }

    @Test
    void findByUser_returnsTheOrdersOfThatUser() {
        when(orderRepository.findAllByUserId(7L))
                .thenReturn(List.of(order(5L, 7L, Status.CREATED, mouse, 1),
                        order(6L, 7L, Status.PAID, mouse, 2)));

        assertThat(service.getAllByUser(7L)).extracting(OrderResponse::id).containsExactly(5L, 6L);
    }

    @Test
    void statusMovesForward() {
        Order existing = order(5L, 7L, Status.CREATED, mouse, 1);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(existing));

        OrderResponse response = service.update(5L, new OrderUpdateRequest(Status.PAID, null));

        assertThat(response.status()).isEqualTo(Status.PAID);
    }

    @Test
    void statusCannotGoBack() {
        when(orderRepository.findById(5L))
                .thenReturn(Optional.of(order(5L, 7L, Status.PAID, mouse, 1)));

        assertThatThrownBy(() -> service.update(5L, new OrderUpdateRequest(Status.CREATED, null)))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void itemsAreReplaced_andTheTotalIsRecalculated_whileTheOrderIsCreated() {
        Order existing = order(5L, 7L, Status.CREATED, mouse, 1);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(itemRepository.findAllById(any())).thenReturn(List.of(pad));

        OrderResponse response = service.update(5L,
                new OrderUpdateRequest(null, List.of(new OrderItemRequest(2L, 4))));

        assertThat(response.totalPrice()).isEqualByComparingTo("10.00");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).itemId()).isEqualTo(2L);

        verify(orderItemRepository).deleteAll(argThat((List<OrderItem> old) -> old.size() == 1));
    }

    @Test
    void itemsCannotBeChanged_afterTheOrderIsPaid() {
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order(5L, 7L, Status.PAID, mouse, 1)));

        assertThatThrownBy(() -> service.update(5L,
                new OrderUpdateRequest(null, List.of(new OrderItemRequest(2L, 1)))))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderItemRepository, never()).deleteAll(any());
    }

    @Test
    void missingOrder_throws() {
        when(orderRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(5L, new OrderUpdateRequest(Status.PAID, null)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void ownerAndAdmin_canDelete() {
        Order existing = order(5L, 7L, Status.CREATED, mouse, 1);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.delete(5L, OWNER);
        service.delete(5L, ADMIN);

        org.mockito.Mockito.verify(orderRepository, org.mockito.Mockito.times(2)).delete(existing);
    }

    @Test
    void stranger_getsNotFound_andNothingIsDeleted() {
        when(orderRepository.findById(5L))
                .thenReturn(Optional.of(order(5L, 7L, Status.CREATED, mouse, 1)));

        assertThatThrownBy(() -> service.delete(5L, STRANGER))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).delete((Order) any());
    }
}
