package com.innowise.academy.orderservice.service;

import com.innowise.academy.orderservice.client.UserServiceClient;
import com.innowise.academy.orderservice.config.CacheConfig;
import com.innowise.academy.orderservice.dto.response.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProviderService {
    private final UserServiceClient client;

    @Cacheable(cacheNames = CacheConfig.USERS_CACHE, key = "#userId")
    public UserInfo getUserInfo(Long userId,String token){
        return client.getProfile(userId,token);
    }
}
