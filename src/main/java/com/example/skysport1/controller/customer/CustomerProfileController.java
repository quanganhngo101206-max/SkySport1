package com.example.skysport1.controller.customer;

import com.example.skysport1.dto.request.ChangePasswordRequest;
import com.example.skysport1.dto.request.ProfileUpdateRequest;
import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.exception.DuplicateException;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer/profile")
@RequiredArgsConstructor
@Slf4j
public class CustomerProfileController {

    private final CustomerService customerService;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Xem thông tin cá nhân
     */
    @GetMapping
    public String profile(Authentication auth, Model model) {
        try {
            String username = auth.getName();
            Account account = accountService.findByUsername(username);
            Customer customer = customerService.findByAccountId(account.getId());
            model.addAttribute("customer", customer);
            model.addAttribute("account", account);
        } catch (Exception e) {
            log.error("Error loading profile: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải thông tin cá nhân");
        }
        model.addAttribute("title", "Hồ sơ của tôi");
        model.addAttribute("pageContent", "customer/profile/index");
        return "layouts/customer/layout";
    }

    /**
     * Cập nhật thông tin cá nhân
     */
    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute ProfileUpdateRequest request,
                                BindingResult bindingResult,
                                Authentication auth,
                                Model model,
                                RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors());
            model.addAttribute("title", "Hồ sơ của tôi");
            model.addAttribute("pageContent", "customer/profile/index");
            return "layouts/customer/layout";
        }

        try {
            String username = auth.getName();
            Account account = accountService.findByUsername(username);
            Customer customer = customerService.findByAccountId(account.getId());

            com.example.skysport1.dto.request.CustomerUpdateRequest updateRequest =
                    new com.example.skysport1.dto.request.CustomerUpdateRequest();
            updateRequest.setId(customer.getId());
            updateRequest.setFullName(request.getFullName());
            updateRequest.setPhone(request.getPhone());
            updateRequest.setEmail(request.getEmail());
            updateRequest.setGender(request.getGender());
            updateRequest.setDob(request.getDob());
            // Khách hàng tự sửa hồ sơ không được đổi status của chính mình
            updateRequest.setStatus(null);

            customerService.update(customer.getId(), updateRequest);
            ra.addFlashAttribute("success", "Cập nhật thông tin thành công!");
            log.info("Profile updated for customer {}", customer.getId());
        } catch (DuplicateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            ra.addFlashAttribute("error", "Không thể cập nhật thông tin: " + e.getMessage());
        }
        return "redirect:/customer/profile";
    }

    /**
     * Trang đổi mật khẩu
     */
    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        model.addAttribute("title", "Đổi mật khẩu");
        model.addAttribute("pageContent", "customer/profile/change-password");
        return "layouts/customer/layout";
    }

    /**
     * Xử lý đổi mật khẩu
     */
    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 Model model,
                                 RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Đổi mật khẩu");
            model.addAttribute("pageContent", "customer/profile/change-password");
            return "layouts/customer/layout";
        }

        try {
            String username = auth.getName();
            Account account = accountService.findByUsername(username);

            // Kiểm tra mật khẩu hiện tại
            if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
                model.addAttribute("error", "Mật khẩu hiện tại không đúng");
                model.addAttribute("title", "Đổi mật khẩu");
                model.addAttribute("pageContent", "customer/profile/change-password");
                return "layouts/customer/layout";
            }

            // Kiểm tra mật khẩu mới khớp
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                model.addAttribute("error", "Mật khẩu xác nhận không khớp");
                model.addAttribute("title", "Đổi mật khẩu");
                model.addAttribute("pageContent", "customer/profile/change-password");
                return "layouts/customer/layout";
            }

            // Cập nhật mật khẩu
            accountService.updatePassword(username, passwordEncoder.encode(request.getNewPassword()));
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");

            // Logout và redirect về login
            return "redirect:/logout";

        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", "Đổi mật khẩu");
            model.addAttribute("pageContent", "customer/profile/change-password");
            return "layouts/customer/layout";
        }
    }
}