package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/profile")
@RequiredArgsConstructor
@Slf4j
public class AdminProfileController {

    private final StaffService staffService;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    // ── Xem thông tin cá nhân ────────────────────────────────────────────

    @GetMapping
    public String profile(Authentication auth, Model model) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            Account account = accountService.findByUsernameWithRole(auth.getName());
            model.addAttribute("staff", staff);
            model.addAttribute("account", account);
        } catch (Exception e) {
            log.warn("Could not load profile for {}: {}", auth.getName(), e.getMessage());
            model.addAttribute("error", "Không tải được thông tin tài khoản");
        }
        model.addAttribute("title", "Thông tin cá nhân");
        model.addAttribute("pageContent", "admin/profile/index");
        return "layouts/adminlte/layout";
    }

    // ── Đổi mật khẩu ─────────────────────────────────────────────────────

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        try {
            Account account = accountService.findByUsername(auth.getName());

            if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
                ra.addFlashAttribute("error", "Mật khẩu hiện tại không đúng");
                return "redirect:/admin/profile";
            }
            if (!newPassword.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
                return "redirect:/admin/profile";
            }
            if (newPassword.length() < 6) {
                ra.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự");
                return "redirect:/admin/profile";
            }

            accountService.updatePassword(auth.getName(), passwordEncoder.encode(newPassword));
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
            log.info("Password changed for user {}", auth.getName());
        } catch (Exception e) {
            log.error("Error changing password for {}: {}", auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/profile";
    }
}