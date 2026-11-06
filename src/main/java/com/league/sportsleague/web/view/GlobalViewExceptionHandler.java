package com.league.sportsleague.web.view;

import com.league.sportsleague.domain.exception.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles exceptions thrown by Thymeleaf view controllers.
 * Scope: web.view package only; REST exceptions are handled by RestExceptionHandler.
 */
@ControllerAdvice(basePackages = "com.league.sportsleague.web.view")
public class GlobalViewExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex, Model model) {
        model.addAttribute("errorTitle", "Not Found");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public String handleInvalidState(InvalidStateTransitionException ex,
                                     RedirectAttributes ra) {
        ra.addFlashAttribute("flashMsg", "Invalid operation: " + ex.getMessage());
        return "redirect:/";
    }

    /** Let Spring Security handle access denied — produces 403, not 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public void rethrowAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    @ExceptionHandler({IllegalArgumentException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleDomainOrPersistence(Exception ex, Model model) {
        model.addAttribute("errorTitle", "Invalid Request");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "A data error occurred.");
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        model.addAttribute("errorTitle", "Internal Server Error");
        model.addAttribute("errorMessage", "An unexpected error occurred.");
        return "error/500";
    }
}
