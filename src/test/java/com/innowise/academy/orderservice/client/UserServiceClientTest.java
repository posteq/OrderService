package com.innowise.academy.orderservice.client;

import com.innowise.academy.orderservice.dto.response.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class UserServiceClientTest {


    private static final String PROFILE = "{\"id\":7,\"firstname\":\"Ann\",\"surname\":\"Lee\"," +
            "\"email\":\"ann@mail.com\"," +
            "\"birthday\":\"1990-01-01\",\"active\":true,\"cards\":[]}";

    public static final String BASE_URL="http://user-service/api/users/7";

    private MockRestServiceServer server;
    private UserServiceClient client;

    @BeforeEach
    void setUp(){
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UserServiceClient(builder.build());
    }

    @Test
    void owner_readsHisProfile_withHisToken(){
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization","Bearer token"))
                .andRespond(withSuccess(PROFILE, MediaType.APPLICATION_JSON));

        UserInfo profile = client.getProfile(7L, "token");

        assertThat(profile).isEqualTo(new UserInfo(7L,"Ann", "Lee","ann@mail.com"));
        server.verify();
    }

    @Test
    void admin_readsAnyProfile_withHisToken(){
        server.expect(requestTo(BASE_URL))
                .andExpect(header("Authorization","Bearer admin-token"))
                .andRespond(withSuccess(PROFILE, MediaType.APPLICATION_JSON));

        UserInfo profile = client.getProfile(7L, "admin-token");

        assertThat(profile.firstname()).isEqualTo("Ann");
        server.verify();
    }

    @Test
    void serverError_circuitBreakerCountIt(){
        server.expect(requestTo(BASE_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getProfile(7L,"token"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void notFound(){
        server.expect(requestTo(BASE_URL))
                .andRespond(withStatus(NOT_FOUND));

        assertThatThrownBy(() -> client.getProfile(7L,"token"))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }
}
