package com.innowise.academy.orderservice.exception;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String e) {
        super(e);
    }
}
