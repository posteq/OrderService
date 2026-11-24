package com.innowise.academy.orderservice.intagration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.academy.orderservice.entity.Item;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileIT extends BaseIT {

    @Test
    void profileIsCached_AfterFirstRequest() throws Exception {
        Long orderId = createOrder_withCleanState(10L);
        stubUserProfile(10L, "Ann");


        mockMvc.perform(get("/api/orders/{id}", orderId).with(userJwt(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.firstname").value("Ann"));


        mockMvc.perform(get("/api/orders/{id}", orderId).with(userJwt(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.firstname").value("Ann"));

        mockMvc.perform(get("/api/orders/{id}", orderId).with(userJwt(10L)))
                .andExpect(status().isOk());

        userServiceMock.verify(1, getRequestedFor(urlEqualTo("/api/users/10")));
    }

    @Test
    void returnsFallback_WhenUserService_IsDown() throws Exception {
        Long orderId = createOrder_withCleanState(10L);
        userServiceMock.stubFor(WireMock.get(urlEqualTo("/api/users/10"))
                .willReturn(serverError()));


        mockMvc.perform(get("/api/orders/{id}", orderId).with(userJwt(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userInfoAvailable").value(false))
                .andExpect(jsonPath("$.user").isEmpty());
    }

    @Test
    void circuitBreakerOpens_AfterRepeatedFailures() throws Exception {
        Long orderId = createOrder_withCleanState(10L);
        userServiceMock.stubFor(WireMock.get(urlEqualTo("/api/users/10"))
                .willReturn(serverError()));


        var circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");

        for (int i = 0; i < 8; i++) {
            mockMvc.perform(get("/api/orders/{id}", orderId).with(userJwt(10L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userInfoAvailable").value(false));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        int callsBefore = userServiceMock.getAllServeEvents().size();
        mockMvc.perform(get("/api/orders/{id}", orderId).with(userJwt(10L)))
                .andExpect(status().isOk());

        assertThat(userServiceMock.getAllServeEvents()).hasSize(callsBefore);
    }

    private Long createOrder_withCleanState(long userId) throws Exception {
        Item mouse = createItem("Mouse", "10.00");
        stubUserProfile(userId, "Ann");
        Long orderId = createOrder(userId, mouse.getId(), 1);

        Objects.requireNonNull(cacheManager.getCache("users")).clear();
        circuitBreakerRegistry.circuitBreaker("userService").reset();
        userServiceMock.resetAll();

        return orderId;
    }
}
