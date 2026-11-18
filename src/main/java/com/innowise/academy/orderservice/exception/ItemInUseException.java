package com.innowise.academy.orderservice.exception;

public class ItemInUseException extends RuntimeException {
    public ItemInUseException(Long id) {
        super("Item - "+ id+" used in order and cannot be deleted");
    }
}
