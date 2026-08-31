package com.kiezmarkt.listing.error;

import com.kiezmarkt.listing.dto.Problem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps every exception this service can throw to an RFC 9457
 * {@code application/problem+json} response, per {@code contracts/api.yaml}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<Problem> handleProblem(ProblemException ex, HttpServletRequest request) {
        return problem(ex.getStatus(), ex.getType(), ex.getTitle(), ex.getDetail(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::describe)
                .orElse("Validation failed.");
        return problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION_FAILED, "Validation failed", detail, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Problem> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse("Validation failed.");
        return problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION_FAILED, "Validation failed", detail, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Problem> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION_FAILED, "Validation failed",
                "The request body is malformed: " + rootMessage(ex), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Problem> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION_FAILED, "Validation failed",
                ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Problem> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION_FAILED, "Validation failed",
                ex.getName() + " has an invalid value.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleUnexpected(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "about:blank", "Internal Server Error",
                ex.getMessage(), request);
    }

    private String describe(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private String rootMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? ex.getMessage() : cause.getMessage();
    }

    private ResponseEntity<Problem> problem(HttpStatus status, String type, String title, String detail, HttpServletRequest request) {
        Problem problem = new Problem(type, title, status.value(), detail, request.getRequestURI());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
