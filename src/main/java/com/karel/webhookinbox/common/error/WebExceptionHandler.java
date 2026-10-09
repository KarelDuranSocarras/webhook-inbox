package com.karel.webhookinbox.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(basePackages = {
        "com.karel.webhookinbox.inbox.view",
        "com.karel.webhookinbox.webhook.view"
})
public class WebExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ModelAndView handleNotFound(ResourceNotFoundException ex) {
        return errorView(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    ModelAndView handleValidation(ValidationException ex) {
        return errorView(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ModelAndView errorView(HttpStatus status, String message) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("message", message);
        return modelAndView;
    }
}
