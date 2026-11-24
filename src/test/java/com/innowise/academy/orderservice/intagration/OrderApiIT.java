package com.innowise.academy.orderservice.intagration;

import com.innowise.academy.orderservice.entity.Item;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderApiIT extends BaseIT {

    @Test
    void createOrder_CalculatesTotal_AttachesProfile() throws Exception {
        Item mouse = createItem("Mouse", "19.99");
        Item pad = createItem("Pad", "5.01");
        stubUserProfile(10L, "Ann");

        String body = """
                {
                  "items": [
                    {"itemId": %d, "quantity": 2},
                    {"itemId": %d, "quantity": 1}
                  ]
                }
                """.formatted(mouse.getId(), pad.getId());

        mockMvc.perform(post("/api/orders")
                        .with(userJwt(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(44.99))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.user.firstname").value("Ann"))
                .andExpect(jsonPath("$.userInfoAvailable").value(true));
    }

    @Test
    void getOrderById_RespectsOwnershipAndRoles() throws Exception {
        Item mouse = createItem("Mouse", "10.00");
        stubUserProfile(10L, "Ann");
        Long orderId = createOrder(10L, mouse.getId(), 1);

        mockMvc.perform(get("/api/orders/{id}", orderId).with(userJwt(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].itemName").value("Mouse"));

        mockMvc.perform(get("/api/orders/{id}", orderId).with(adminJwt(1L)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/{id}", orderId).with(userJwt(11L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchOrdersFilters_ByUserRoleAndPagination() throws Exception {
        Item mouse = createItem("Mouse", "10.00");
        stubUserProfile(10L, "Ann");
        stubUserProfile(11L, "Bob");

        createOrder(10L, mouse.getId(), 1);
        createOrder(10L, mouse.getId(), 2);
        createOrder(11L, mouse.getId(), 3);

        mockMvc.perform(get("/api/orders").with(userJwt(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/orders").with(adminJwt(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));

        mockMvc.perform(get("/api/orders").param("size", "1").with(adminJwt(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void searchOrdersFiltersBy_StatusAndDateRange() throws Exception {
        Item mouse = createItem("Mouse", "10.00");
        stubUserProfile(10L, "Ann");

        Long janOrder = createOrder(10L, mouse.getId(), 1);
        Long junCreated = createOrder(10L, mouse.getId(), 1);
        Long junPaid = createOrder(10L, mouse.getId(), 1);

        updateOrderMeta(janOrder, "PAID", "2025-01-10 12:00:00");
        updateOrderMeta(junCreated, "CREATED", "2025-06-10 12:00:00");
        updateOrderMeta(junPaid, "PAID", "2025-06-20 12:00:00");

        mockMvc.perform(get("/api/orders")
                        .param("status", "PAID")
                        .with(userJwt(10L)))
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/orders")
                        .param("startDate", "2025-06-01")
                        .param("endDate", "2025-06-15")
                        .with(userJwt(10L)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(junCreated));

        mockMvc.perform(get("/api/orders")
                        .param("status", "PAID")
                        .param("startDate", "2025-06-01")
                        .with(userJwt(10L)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(junPaid));

        mockMvc.perform(get("/api/orders")
                        .param("startDate", "2025-06-20")
                        .param("endDate", "2025-06-20")
                        .with(userJwt(10L)))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void updateOrder_ReplacesItems_RecalculatesTotal() throws Exception {
        Item mouse = createItem("Mouse", "10.00");
        Item pad = createItem("Pad", "2.50");
        stubUserProfile(10L, "Ann");
        Long orderId = createOrder(10L, mouse.getId(), 3);

        String updateBody = """
                {"items": [{"itemId": %d, "quantity": 4}]}
                """.formatted(pad.getId());

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .with(adminJwt(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(10.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].itemName").value("Pad"));

        Integer itemsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE order_id = ?", Integer.class, orderId);
        assertThat(itemsCount).isEqualTo(1);
    }

    @Test
    void deleteOrder_PerformsSoftDelete() throws Exception {
        Item mouse = createItem("Mouse", "10.00");
        stubUserProfile(10L, "Ann");
        Long orderId = createOrder(10L, mouse.getId(), 2);


        mockMvc.perform(delete("/api/orders/{id}", orderId).with(userJwt(11L)))
                .andExpect(status().isNotFound());


        mockMvc.perform(delete("/api/orders/{id}", orderId).with(userJwt(10L)))
                .andExpect(status().isNoContent());


        mockMvc.perform(get("/api/orders/{id}", orderId).with(userJwt(10L)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders").with(userJwt(10L)))
                .andExpect(jsonPath("$.content.length()").value(0));


        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM orders WHERE id = ?", Boolean.class, orderId);
        Integer itemsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE order_id = ?", Integer.class, orderId);

        assertThat(isDeleted).isTrue();
        assertThat(itemsCount).isEqualTo(1);
    }

    private void updateOrderMeta(Long id, String status, String createdAt) {
        jdbcTemplate.update(
                "UPDATE orders SET status = ?, created_at = ? WHERE id = ?",
                status, Timestamp.valueOf(createdAt), id
        );
    }
}
