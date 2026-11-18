package com.innowise.academy.orderservice.service;

import com.innowise.academy.orderservice.dto.request.order.OrderCreateRequest;
import com.innowise.academy.orderservice.dto.request.order.OrderFilter;
import com.innowise.academy.orderservice.dto.request.order.OrderItemRequest;
import com.innowise.academy.orderservice.dto.request.order.OrderUpdateRequest;
import com.innowise.academy.orderservice.dto.response.UserInfo;
import com.innowise.academy.orderservice.dto.response.order.OrderResponse;
import com.innowise.academy.orderservice.entity.Status;
import com.innowise.academy.orderservice.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderWithUserServiceTest {

    private static final CurrentUser OWNER = new CurrentUser(7L, false, "owner-token");
    private static final CurrentUser ADMIN = new CurrentUser(99L, true, "admin-token");
    private static final UserInfo ANN = new UserInfo(7L, "Ann", "Lee", "ann@mail.com");

    @Mock
    private OrderService orderService;
    @Mock
    private UserProviderService userProviderService;

    @InjectMocks
    private OrderWithUserService service;

    private static OrderResponse order(Long id, Long userId) {
        return new OrderResponse(id, userId, Status.CREATED, new BigDecimal("10.00"),
                LocalDateTime.of(2025, 1, 1, 10, 0),
                List.of(), null, false);
    }

    @Test
    void get_addsTheProfile_andOwnerAsksForHisOwnProfile() {
        when(orderService.get(5L, OWNER)).thenReturn(order(5L, 7L));
        when(userProviderService.getUserInfo(7L, "owner-token")).thenReturn(ANN);

        OrderResponse response = service.get(5L, OWNER);

        assertThat(response.user()).isEqualTo(ANN);
        assertThat(response.userInfoAvailable()).isTrue();
    }

    @Test
    void get_adminViewingSomebodyElseOrder_asksForTheProfileByIdWithHisOwnToken() {
        when(orderService.get(5L, ADMIN)).thenReturn(order(5L, 7L));
        when(userProviderService.getUserInfo(7L,  "admin-token")).thenReturn(ANN);

        OrderResponse response = service.get(5L, ADMIN);

        assertThat(response.userInfoAvailable()).isTrue();
    }

    @Test
    void get_whenTheProfileCannotBeObtained_theOrderIsStillReturned_withTheFlagOff() {
        when(orderService.get(5L, OWNER)).thenReturn(order(5L, 7L));
        when(userProviderService.getUserInfo(7L, "owner-token"))
                .thenThrow(new RestClientException("User Service is down"));

        OrderResponse response = service.get(5L, OWNER);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.user()).isNull();
        assertThat(response.userInfoAvailable()).isFalse();
    }

    @Test
    void search_asksForEachOwnerOnlyOnce() {
        Page<OrderResponse> page = new PageImpl<>(List.of(order(1L, 7L),
                order(2L, 7L),
                order(3L, 8L)));
        OrderFilter filter = new OrderFilter(null, null, null, null);

        when(orderService.search(filter, 0, 10,"createdAt","ASC"))
                .thenReturn(page);
        when(userProviderService.getUserInfo(eq(7L), any()))
                .thenReturn(ANN);
        when(userProviderService.getUserInfo(eq(8L), any()))
                .thenThrow(new RestClientException("boom"));

        Page<OrderResponse> result = service.search(filter, 0, 10,"createdAt","ASC", ADMIN);

        assertThat(result.getContent()).extracting(OrderResponse::userInfoAvailable)
                .containsExactly(true, true, false);
        verify(userProviderService, times(1))
                .getUserInfo(eq(7L), any());
        verify(userProviderService, times(1))
                .getUserInfo(eq(8L), any());
    }

    @Test
    void findByUser_asksForTheProfileOnce_forAllOrders() {
        when(orderService.getAllByUser(7L))
                .thenReturn(List.of(order(1L, 7L), order(2L, 7L)));
        when(userProviderService.getUserInfo(7L,  "admin-token")).thenReturn(ANN);

        List<OrderResponse> result = service.findByUser(7L, ADMIN);

        assertThat(result).hasSize(2)
                .allMatch(OrderResponse::userInfoAvailable);
        verify(userProviderService, times(1))
                .getUserInfo(7L,  "admin-token");
    }

    @Test
    void create_usesTheIdOfTheCaller_andAddsTheProfile() {
        OrderCreateRequest request = new OrderCreateRequest(List.of(new OrderItemRequest(1L, 2)));
        when(orderService.create(7L, request)).thenReturn(order(5L, 7L));
        when(userProviderService.getUserInfo(7L,  "owner-token")).thenReturn(ANN);

        OrderResponse response = service.create(request, OWNER);

        assertThat(response.user()).isEqualTo(ANN);
    }

    @Test
    void update_addsTheProfile() {
        OrderUpdateRequest request = new OrderUpdateRequest(Status.PAID, null);
        when(orderService.update(5L, request)).thenReturn(order(5L, 7L));
        when(userProviderService.getUserInfo(7L,  "admin-token")).thenReturn(ANN);

        assertThat(service.update(5L, request, ADMIN).userInfoAvailable()).isTrue();
    }

    @Test
    void delete_doesNotCallTheUserService() {
        service.delete(5L, OWNER);

        verify(orderService).delete(5L, OWNER);
        verifyNoInteractions(userProviderService);
    }
}
