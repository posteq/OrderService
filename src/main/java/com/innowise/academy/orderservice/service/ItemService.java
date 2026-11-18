package com.innowise.academy.orderservice.service;

import com.innowise.academy.orderservice.dto.request.item.ItemRequest;
import com.innowise.academy.orderservice.dto.response.item.ItemResponse;
import com.innowise.academy.orderservice.entity.Item;
import com.innowise.academy.orderservice.exception.ItemInUseException;
import com.innowise.academy.orderservice.exception.ItemNotFoundException;
import com.innowise.academy.orderservice.mapper.ItemMapper;
import com.innowise.academy.orderservice.repository.ItemRepository;
import com.innowise.academy.orderservice.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemMapper mapper;

    private static final Set<String> SORT_FIELDS = Set.of("name","price");

    @Transactional
    public ItemResponse create(ItemRequest request){
        return mapper.toResponse(itemRepository.save(mapper.toEntity(request)));
    }

    @Transactional
    public ItemResponse update(Long id,ItemRequest request){
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
        mapper.updateEntity(request,item);
        return mapper.toResponse(item);
    }

    @Transactional
    public void delete(Long id){
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
        if(orderItemRepository.existsByItem_Id(id)){
            throw new ItemInUseException(id);
        }
        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> get(int page, int size, String sortBy, String sortDir){
        if(!SORT_FIELDS.contains(sortBy)){
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir),sortBy);
        PageRequest pageRequest = PageRequest.of(Math.max(page,0), Math.max(size,1), sort);

        return itemRepository.findAll(pageRequest)
                .map(mapper::toResponse);
    }

}
