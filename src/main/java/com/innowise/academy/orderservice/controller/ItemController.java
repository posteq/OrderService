package com.innowise.academy.orderservice.controller;

import com.innowise.academy.orderservice.dto.request.item.ItemRequest;
import com.innowise.academy.orderservice.dto.response.item.ItemResponse;
import com.innowise.academy.orderservice.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService service;

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping()
    public ResponseEntity<Page<ItemResponse>> getItems(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(defaultValue = "name") String sortBy,
                                                       @RequestParam(defaultValue = "ASC") String sortDir){
        return ResponseEntity.ok()
                .body(service.get(page,size,sortBy,sortDir));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ItemResponse> create(@RequestBody @Valid ItemRequest request){
        ItemResponse created = service.create(request);
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(uri)
                .body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.noContent()
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ItemResponse> update(@PathVariable Long id,
                                               @RequestBody @Valid ItemRequest request){
        return ResponseEntity.ok()
                .body(service.update(id,request));
    }
}
