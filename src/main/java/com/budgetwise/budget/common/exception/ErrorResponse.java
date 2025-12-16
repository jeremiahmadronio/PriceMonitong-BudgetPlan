package com.budgetwise.budget.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response format for all API errors.
 *
 * Every error response contains:
 * - status: HTTP status code (404, 500, etc.)
 * - error: Error type (NOT_FOUND, VALIDATION_ERROR, etc.)
 * - message: Human-readable error message
 * - path: API endpoint that caused the error
 * - timestamp: When the error occurred
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}