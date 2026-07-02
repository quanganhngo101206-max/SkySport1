package com.example.skysport1.controller;

import com.example.skysport1.dto.request.RegisterRequest;
import com.example.skysport1.exception.AppException;
import com.example.skysport1.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AccountService accountService;

    // ============================================
    // CUSTOMER LOGIN - /login
    // ============================================
    @GetMapping("/login")
    public String customerLoginPage(@RequestParam(required = false) String error,
                                    @RequestParam(required = false) String logout,
                                    @RequestParam(required = false) String success,
                                    Model model) {
        if (error != null) {
            model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu");
        }
        if (logout != null) {
            model.addAttribute("message", "Đăng xuất thành công");
        }
        if (success != null) {
            model.addAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
        }
        return "auth/login";
    }

    // ============================================
    // ADMIN LOGIN - /admin/login
    // ============================================
    @GetMapping("/admin/login")
    public String adminLoginPage(@RequestParam(required = false) String error,
                                 @RequestParam(required = false) String logout,
                                 Model model) {
        if (error != null) {
            model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu");
        }
        if (logout != null) {
            model.addAttribute("message", "Đăng xuất thành công");
        }
        return "admin/login";
    }

    // ============================================
    // STAFF LOGIN - /staff/login
    // ============================================
    @GetMapping("/staff/login")
    public String staffLoginPage(@RequestParam(required = false) String error,
                                 @RequestParam(required = false) String logout,
                                 Model model) {
        if (error != null) {
            model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu");
        }
        if (logout != null) {
            model.addAttribute("message", "Đăng xuất thành công");
        }
        return "staff/login";
    }

    // ============================================
    // CUSTOMER REGISTER - /dang-ky
    // ============================================
    @GetMapping("/dang-ky")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/dang-ky")
    public String register(@Valid @ModelAttribute RegisterRequest request,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            accountService.register(request);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login?success=true";
        } catch (AppException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    // ============================================
    // 403 - Access Denied
    // ============================================
    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}