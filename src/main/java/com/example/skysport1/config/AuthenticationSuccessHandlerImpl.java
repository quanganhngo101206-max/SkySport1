package com.example.skysport1.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        Collection<SimpleGrantedAuthority> authorities =
                (Collection<SimpleGrantedAuthority>) authentication.getAuthorities();

        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaff = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));

        if (isAdmin) {
            response.sendRedirect("/admin");
        } else if (isStaff) {
            response.sendRedirect("/staff");
        } else {
            response.sendRedirect("/");
        }
    }
}