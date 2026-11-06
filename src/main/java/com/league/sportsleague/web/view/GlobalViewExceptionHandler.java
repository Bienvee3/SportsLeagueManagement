package com.league.sportsleague.web.view;

import com.league.sportsleague.domain.exception.DuplicateTeamException;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles exceptions thrown by Thymeleaf view controllers.
 * Scope: web.view package only; REST exceptions are handled by RestExceptionHandler.
 */
@ControllerAdvice(basePackages = "com.league.sportsleague.web.view")
public class GlobalViewExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalViewExceptionHandler.class);

    // ── Domain exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex, Model model) {
        model.addAttribute("errorTitle", "Not Found");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    /** Redirects back to the page the user came from so context is preserved. */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public String handleInvalidState(InvalidStateTransitionException ex,
                                     RedirectAttributes ra,
                                     HttpServletRequest request) {
        ra.addFlashAttribute("errorMsg", "Invalid operation: " + ex.getMessage());
        return redirectToReferer(request);
    }

    /** Let Spring Security handle access denied — produces 403, not 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public void rethrowAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class,
                       DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleDomainOrPersistence(Exception ex, Model model) {
        model.addAttribute("errorTitle", "Invalid Request");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "A data error occurred.");
        return "error/500";
    }

    @ExceptionHandler(DuplicateTeamException.class)
    public String handleDuplicateTeam(DuplicateTeamException ex,
                                      RedirectAttributes ra,
                                      HttpServletRequest request) {
        ra.addFlashAttribute("errorMsg", "Cannot register team: " + ex.getMessage());
        return redirectToReferer(request);
    }

    // ── Spring MVC exceptions (would otherwise become 500) ───────────────────

    /**
     * Non-UUID path variable (e.g. GET /leagues/abc) — returns 400 instead of 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException ex, Model model) {
        model.addAttribute("errorTitle", "Bad Request");
        model.addAttribute("errorMessage",
                "Invalid value for parameter '" + ex.getName() + "'.");
        return "error/500";
    }

    /**
     * Wrong HTTP method on a view route (e.g. GET on a POST-only endpoint) — returns 405.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, Model model) {
        model.addAttribute("errorTitle", "Method Not Allowed");
        model.addAttribute("errorMessage", "HTTP method not supported for this endpoint.");
        return "error/500";
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        log.error("Unhandled exception in view layer", ex);
        model.addAttribute("errorTitle", "Internal Server Error");
        model.addAttribute("errorMessage", "An unexpected error occurred.");
        return "error/500";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String redirectToReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isBlank() ? referer : "/");
    }
}
