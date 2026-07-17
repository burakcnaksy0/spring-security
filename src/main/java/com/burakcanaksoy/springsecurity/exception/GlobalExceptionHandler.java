package com.burakcanaksoy.springsecurity.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Object> handleEmailNotVerifiedException(EmailNotVerifiedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "FORBIDDEN");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<Object> handleAlreadyExistsException(AlreadyExistsException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<Map<String, Object>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, ServletWebRequest request) {
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        List<FieldError> fieldErrors = ex.getFieldErrors();

        for (FieldError error : fieldErrors) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("message", error.getDefaultMessage());
            map.put("field", error.getField());
            map.put("path", request.getRequest().getRequestURI());
            map.put("rejected value", error.getRejectedValue());
            map.put("annotation", error.getCode());
            map.put("time", LocalDateTime.now());

            list.add(map);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(list);
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<List<Map<String, Object>>> handleConstraintViolationException(ConstraintViolationException ex, ServletWebRequest request) {
        List<Map<String, Object>> errors = new ArrayList<>();
        Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();

        for (ConstraintViolation<?> cv : constraintViolations) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("message", cv.getMessage());
            map.put("path", request.getRequest().getRequestURI());
            map.put("invalid value", cv.getInvalidValue());
            map.put("time", LocalDateTime.now());
            map.put("property", cv.getPropertyPath().toString());

            errors.add(map);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }


}
