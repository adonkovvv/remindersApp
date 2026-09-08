package com.adonkov.reminders.common;

import com.adonkov.reminders.auth.AuthException;
import com.adonkov.reminders.reminder.ReminderNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReminderNotFoundException.class)
    public ProblemDetail handleNotFound(ReminderNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Reminder not found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(AuthException.class)
    public ProblemDetail handleAuth(AuthException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatus());
        problem.setTitle("Authentication failed");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    /**
     * Two people (or two tabs) edited the same reminder at once and the second write lost.
     * Surfaced as a conflict so the client can refetch rather than silently clobbering.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentEdit(OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Conflicting update");
        problem.setDetail("This reminder was changed by someone else. Reload it and try again.");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("One or more fields are invalid");
        problem.setProperty("errors", errors);
        return problem;
    }
}
