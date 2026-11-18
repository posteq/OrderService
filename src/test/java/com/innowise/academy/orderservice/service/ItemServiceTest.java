package com.innowise.academy.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.academy.orderservice.dto.request.item.ItemRequest;
import com.innowise.academy.orderservice.dto.response.item.ItemResponse;
import com.innowise.academy.orderservice.entity.Item;
import com.innowise.academy.orderservice.exception.ItemInUseException;
import com.innowise.academy.orderservice.exception.ItemNotFoundException;
import com.innowise.academy.orderservice.mapper.ItemMapper;
import com.innowise.academy.orderservice.repository.ItemRepository;
import com.innowise.academy.orderservice.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private OrderItemRepository orderItemRepository;

    private ItemService service;

    @BeforeEach
    void setUp() {
        service = new ItemService(itemRepository, orderItemRepository, Mappers.getMapper(ItemMapper.class));
    }

    private static Item item(Long id, String name, String price) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return item;
    }

    @Test
    void create_savesItem_andReturnsIt() {
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocation -> {
            Item saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ItemResponse response = service.create(new ItemRequest("Mouse", new BigDecimal("19.99")));

        assertThat(response).isEqualTo(new ItemResponse(1L, "Mouse", new BigDecimal("19.99")));
    }

    @Test
    void getItems_returnsPage_withNormalizedPageSettings() {
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item(1L, "Mouse", "19.99"))));

        Page<ItemResponse> page = service.get(3, 1000,"name","ASC");

        assertThat(page.getContent()).containsExactly(new ItemResponse(1L, "Mouse", new BigDecimal("19.99")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(3);
        assertThat(captor.getValue().getPageSize()).isEqualTo(1000);
    }

    @Test
    void update_changesNameAndPrice() {
        Item existing = item(1L, "Mouse", "19.99");
        when(itemRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        ItemResponse response = service.update(1L, new ItemRequest("Gaming mouse", new BigDecimal("29.99")));

        assertThat(response)
                .isEqualTo(new ItemResponse(1L, "Gaming mouse", new BigDecimal("29.99")));
        assertThat(existing.getName())
                .isEqualTo("Gaming mouse");
    }

    @Test
    void update_unknownItem_throws() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, new ItemRequest("X", BigDecimal.ONE)))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void delete_unusedItem_isDeleted() {
        Item existing = item(1L, "Mouse", "19.99");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderItemRepository.existsByItem_Id(1L)).thenReturn(false);

        service.delete(1L);

        verify(itemRepository).delete(existing);
    }

    @Test
    void delete_itemUsedInOrders_throws() {
        when(itemRepository.findById(1L))
                .thenReturn(Optional.of(item(1L, "Mouse", "19.99")));
        when(orderItemRepository.existsByItem_Id(1L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ItemInUseException.class);

        verify(itemRepository, never()).delete(any());
    }

    @Test
    void delete_unknownItem_throws() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ItemNotFoundException.class);
    }
}
