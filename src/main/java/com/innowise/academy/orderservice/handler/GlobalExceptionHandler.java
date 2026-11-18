package com.innowise.academy.orderservice.handler;

import com.innowise.academy.orderservice.exception.InvalidOrderStateException;
import com.innowise.academy.orderservice.exception.InvalidRequestException;
import com.innowise.academy.orderservice.exception.ItemInUseException;
import com.innowise.academy.orderservice.exception.ItemNotFoundException;
import com.innowise.academy.orderservice.exception.OrderNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({OrderNotFoundException.class, ItemNotFoundException.class})
    public ResponseEntity<ErrorMessage> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(NOT_FOUND)
                .body(new ErrorMessage(NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler({ItemInUseException.class, InvalidOrderStateException.class})
    public ResponseEntity<ErrorMessage> handleConflict(RuntimeException e) {
        return ResponseEntity.status(CONFLICT)
                .body(new ErrorMessage(CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorMessage> handleInvalidRequest(InvalidRequestException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorMessage(BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(" ; "));

        return ResponseEntity.badRequest()
                .body(new ErrorMessage(BAD_REQUEST, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorMessage> handleBadParameter(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorMessage(BAD_REQUEST, "Invalid value of parameter " + e.getName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleUnreadable() {
        return ResponseEntity.badRequest()
                .body(new ErrorMessage(BAD_REQUEST, "Malformed JSON request"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAccessDenied() {
        return ResponseEntity.status(FORBIDDEN)
                .body(new ErrorMessage(FORBIDDEN, "Access denied"));
    }
}
