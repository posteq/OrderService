package com.innowise.academy.orderservice.client;

import com.innowise.academy.orderservice.dto.response.UserInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class UserServiceClient {
    private final RestClient client;

    @CircuitBreaker(name = "userService")
    public UserInfo getProfile(Long userId,String token){
        return client.get()
                .uri("/api/users/{userId}",userId)
                .headers(header -> header.setBearerAuth(token))
                .retrieve()
                .body(UserInfo.class);
    }
}
