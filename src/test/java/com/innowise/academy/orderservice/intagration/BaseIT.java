package com.innowise.academy.orderservice.intagration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.innowise.academy.orderservice.entity.Item;
import com.innowise.academy.orderservice.repository.ItemRepository;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
abstract class BaseIT {

    protected static final WireMockServer userServiceMock = new WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort()
    );

    static {
        userServiceMock.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.user-service.url", userServiceMock::baseUrl);
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected CacheManager cacheManager;
    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE order_items, orders, items RESTART IDENTITY CASCADE");
        var cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.clear();
        }

        circuitBreakerRegistry.circuitBreaker("userService").reset();
        userServiceMock.resetAll();
    }

    protected RequestPostProcessor userJwt(long userId) {
        return jwt().jwt(j -> j.claim("user_id", userId))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    protected RequestPostProcessor adminJwt(long userId) {
        return jwt().jwt(j -> j.claim("user_id", userId))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    protected void stubUserProfile(long id, String firstname) {
        String json = """
                {
                  "id": %d,
                  "firstname": "%s",
                  "surname": "Lee",
                  "email": "%s@mail.com",
                  "birthday": "1990-01-01",
                  "active": true,
                  "cards": []
                }
                """.formatted(id, firstname, firstname.toLowerCase());

        userServiceMock.stubFor(get(urlEqualTo("/api/users/" + id))
                .willReturn(okJson(json)));
    }

    protected Item createItem(String name, String price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return itemRepository.save(item);
    }

    protected Long createOrder(long userId, long itemId, int quantity) throws Exception {
        String body = """
                {"items": [{"itemId": %d, "quantity": %d}]}
                """.formatted(itemId, quantity);

        String response = mockMvc.perform(post("/api/orders")
                        .with(userJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.parse(response).read("$.id", Long.class);
    }
}

