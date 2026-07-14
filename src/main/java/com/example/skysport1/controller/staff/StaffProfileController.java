package com.example.skysport1.controller.staff;

import com.example.skysport1.dto.request.ChangePasswordRequest;
import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Hồ sơ cá nhân của Staff (tự phục vụ): xem thông tin cơ bản + tự đổi mật
 * khẩu của chính mình. Cố ý KHÔNG cho sửa fullName/phone/email/... ở đây —
 * những trường đó vẫn do Admin quản lý qua AdminStaffController để tránh
 * 2 nơi cùng sửa 1 dữ liệu. Trước khi có controller này, Staff hoàn toàn
 * không có cách tự đổi mật khẩu của chính mình (chỉ Admin đổi hộ được).
 */
@Controller
@RequestMapping("/staff/profile")
@RequiredArgsConstructor
@Slf4j
public class StaffProfileController {

    private final StaffService staffService;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String profile(Authentication auth, Model model) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            // Dùng findByUsernameWithRole (LEFT JOIN FETCH role) thay vì
            // findByUsername thường, vì Account.role là LAZY và template có
            // thể hiển thị account.role.name — tránh LazyInitializationException
            // do spring.jpa.open-in-view=false.
            Account account = accountService.findByUsernameWithRole(auth.getName());
            model.addAttribute("staff", staff);
            model.addAttribute("account", account);
        } catch (Exception e) {
            log.warn("Could not load staff profile for {}: {}", auth.getName(), e.getMessage());
            model.addAttribute("error", "Không tải được thông tin tài khoản");
        }
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        model.addAttribute("title", "Hồ sơ cá nhân");
        model.addAttribute("pageContent", "staff/profile/index");
        return "layouts/staff/layout";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin đổi mật khẩu");
            return "redirect:/staff/profile";
        }

        try {
            Account account = accountService.findByUsername(auth.getName());

            if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
                ra.addFlashAttribute("error", "Mật khẩu hiện tại không đúng");
                return "redirect:/staff/profile";
            }
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
                return "redirect:/staff/profile";
            }

            accountService.updatePassword(auth.getName(), passwordEncoder.encode(request.getNewPassword()));
            log.info("Staff {} changed their own password", auth.getName());
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            return "redirect:/staff/logout";
        } catch (Exception e) {
            log.error("Error changing password for staff {}: {}", auth.getName(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/profile";
        }
    }
}