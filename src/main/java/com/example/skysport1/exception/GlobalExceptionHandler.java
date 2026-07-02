package com.example.skysport1.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(AppException.class)
    public String handleAppException(AppException ex, Model model) {
        log.error("AppException: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(DuplicateException.class)
    public String handleDuplicateException(DuplicateException ex, Model model) {
        log.error("DuplicateException: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleMethodArgumentNotValid(MethodArgumentNotValidException ex, Model model) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String message = (fieldErrors == null || fieldErrors.isEmpty())
                ? ""
                : fieldErrors.stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("<br/>"));

        model.addAttribute("error", (message == null || message.isBlank()) ? "Dữ liệu không hợp lệ" : message);
        return "error/error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResource(NoResourceFoundException ex) {
        // Bỏ qua favicon.ico và static resource không tìm thấy — không cần log
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        model.addAttribute("error", "Đã xảy ra lỗi, vui lòng thử lại.");
        return "error/error";
    }
}
