package com.innowise.academy.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.innowise.academy.orderservice.client.UserServiceClient;
import com.innowise.academy.orderservice.dto.response.UserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProviderServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private UserProviderService provider;

    @Test
    void getUserInfo_asksTheClient() {
        UserInfo ann = new UserInfo(7L, "Ann", "Lee", "ann@mail.com");
        when(userServiceClient.getProfile(7L, "token")).thenReturn(ann);

        assertThat(provider.getUserInfo(7L, "token")).isEqualTo(ann);
    }
}
