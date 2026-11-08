package com.hotelbooking.hotel.exception;

import com.hotelbooking.hotel.dto.ErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDTO handleConflict(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return new ErrorDTO(
                409,
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
    }
}