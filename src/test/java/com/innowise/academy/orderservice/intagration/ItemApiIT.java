package com.innowise.academy.orderservice.intagration;

import com.innowise.academy.orderservice.entity.Item;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemApiIT extends BaseIT {

    @Test
    void admin_CanCreateItem() throws Exception {
        String payload = """
                {"name": "Mouse", "price": 19.99}
                """;

        mockMvc.perform(post("/api/items")
                        .with(adminJwt(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.price").value(19.99));
    }

    @Test
    void regularUser_CannotModifyCatalog() throws Exception {
        Item item = createItem("Mouse", "19.99");
        String payload = """
                {"name": "Mouse", "price": 19.99}
                """;

        mockMvc.perform(post("/api/items")
                        .with(userJwt(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/items/{id}", item.getId())
                        .with(userJwt(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/items/{id}", item.getId())
                        .with(userJwt(10L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_CanDeleteUnusedItem() throws Exception {
        Item item = createItem("Cable", "3.00");

        mockMvc.perform(delete("/api/items/{id}", item.getId())
                        .with(adminJwt(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void admin_CannotDeleteItemUsedInOrder() throws Exception {
        Item item = createItem("Mouse", "19.99");
        stubUserProfile(10L, "Ann");
        createOrder(10L, item.getId(), 1);

        mockMvc.perform(delete("/api/items/{id}", item.getId())
                        .with(adminJwt(1L)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_NonExistentItem_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/items/{id}", 99999L)
                        .with(adminJwt(1L)))
                .andExpect(status().isNotFound());
    }
}

