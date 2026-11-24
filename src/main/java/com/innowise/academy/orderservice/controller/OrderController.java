package com.innowise.academy.orderservice.controller;

import com.innowise.academy.orderservice.dto.request.order.OrderCreateRequest;
import com.innowise.academy.orderservice.dto.request.order.OrderFilter;
import com.innowise.academy.orderservice.dto.request.order.OrderUpdateRequest;
import com.innowise.academy.orderservice.dto.response.order.OrderResponse;
import com.innowise.academy.orderservice.entity.Status;
import com.innowise.academy.orderservice.security.CurrentUser;
import com.innowise.academy.orderservice.service.OrderWithUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderWithUserService service;

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, JwtAuthenticationToken token){
        service.delete(id, CurrentUser.from(token));
        return ResponseEntity.noContent()
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> update(@PathVariable Long id,
                                                @RequestBody @Valid OrderUpdateRequest request,
                                                JwtAuthenticationToken token){
        return ResponseEntity.ok()
                .body(service.update(id,request, CurrentUser.from(token)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getByUserId(@PathVariable Long userId,
                                                         JwtAuthenticationToken token){
        return ResponseEntity.ok()
                .body(service.findByUser(userId, CurrentUser.from(token)));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getByOrderId(@PathVariable Long id,
                                                      JwtAuthenticationToken token){
        return ResponseEntity.ok()
                .body(service.get(id, CurrentUser.from(token)));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody @Valid OrderCreateRequest request,
                                                JwtAuthenticationToken token){
        OrderResponse created = service.create(request, CurrentUser.from(token));

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(uri)
                .body(created);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> search(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(defaultValue = "createdAt")  String sortBy,
                                                      @RequestParam(defaultValue = "ASC")  String sortDir,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                      @RequestParam(required = false) Status status,
                                                      JwtAuthenticationToken token){
        CurrentUser user = CurrentUser.from(token);
        Long id = user.admin() ? null : user.userId();
        OrderFilter filter = new OrderFilter(id, status, startDate, endDate);
        return ResponseEntity.ok()
                .body(service.search(filter, page, size, sortBy, sortDir, CurrentUser.from(token)));
    }
}
