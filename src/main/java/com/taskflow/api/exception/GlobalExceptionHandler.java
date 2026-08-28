package com.taskflow.api.exception;

import jakarta.persistence.OptimisticLockException;
import java.net.URI;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Translates domain and framework exceptions into RFC 7807 problem details so every error
 * response, regardless of origin, follows the same shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), "not-found");
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "conflict");
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbidden(ForbiddenOperationException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), "forbidden");
    }

    @ExceptionHandler(InvalidTaskTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidTaskTransitionException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "invalid-transition");
    }

    @ExceptionHandler({OptimisticLockException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleOptimisticLock() {
        return problem(
                HttpStatus.CONFLICT,
                "The resource was modified by someone else in the meantime. Reload it and try again.",
                "stale-resource");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity() {
        return problem(HttpStatus.CONFLICT, "The request conflicts with an existing resource.", "data-integrity");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password.", "bad-credentials");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), "unauthenticated");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDenied() {
        return problem(HttpStatus.FORBIDDEN, "You don't have permission to perform this action.", "access-denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "One or more fields are invalid.", "validation-error");
        detail.setProperty(
                "errors",
                ex.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getField)
                        .distinct()
                        .toList());
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody() {
        return problem(HttpStatus.BAD_REQUEST, "Request body is missing or not valid JSON.", "malformed-request");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Parameter '%s' has the wrong type.".formatted(ex.getName()),
                "type-mismatch");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail detail =
                problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", "internal-error");
        detail.setProperty("exception", ex.getClass().getSimpleName());
        return detail;
    }

    private ProblemDetail problem(HttpStatus status, String message, String type) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setType(URI.create("https://taskflow.dev/problems/" + type));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
