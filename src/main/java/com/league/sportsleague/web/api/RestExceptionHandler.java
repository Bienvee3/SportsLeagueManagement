package com.league.sportsleague.web.api;

import com.league.sportsleague.domain.exception.DuplicateTeamException;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.league.sportsleague.web.api")
public class RestExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail handleNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:sports-league:error:not-found"));
        problem.setTitle("Resource Not Found");
        return problem;
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    ProblemDetail handleConflict(InvalidStateTransitionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:sports-league:error:invalid-state-transition"));
        problem.setTitle("Invalid State Transition");
        return problem;
    }

    @ExceptionHandler(com.league.sportsleague.domain.exception.ChronicleUnavailableException.class)
    ProblemDetail handleChronicleUnavailable(com.league.sportsleague.domain.exception.ChronicleUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setType(URI.create("urn:sports-league:error:ai-unavailable"));
        problem.setTitle("AI Provider Unavailable");
        return problem;
    }

    @ExceptionHandler(DuplicateTeamException.class)
    ProblemDetail handleDuplicate(DuplicateTeamException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:sports-league:error:duplicate-team"));
        problem.setTitle("Duplicate Team");
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:sports-league:error:conflict"));
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:sports-league:error:bad-request"));
        problem.setTitle("Bad Request");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> violations = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String field = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
                    return Map.of("field", field, "message", error.getDefaultMessage());
                })
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setType(URI.create("urn:sports-league:error:validation"));
        problem.setTitle("Validation Error");
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    void rethrowAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create("urn:sports-league:error:internal"));
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
