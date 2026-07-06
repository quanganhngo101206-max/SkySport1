package com.example.skysport1.config;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class CsrfAdvice {

    @ModelAttribute("_csrf")
    public CsrfToken getCsrfToken(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            // Fallback: tạo token giả nếu CSRF bị tắt
            return new CsrfToken() {
                @Override public String getHeaderName() { return "X-CSRF-TOKEN"; }
                @Override public String getParameterName() { return "_csrf"; }
                @Override public String getToken() { return "csrf-disabled"; }
            };
        }
        return token;
    }
}