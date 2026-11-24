package com.innowise.academy.orderservice.exception;

public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(String e) {
        super(e);
    }
}
