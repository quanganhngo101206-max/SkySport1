package com.example.skysport1.controller.customer;

import com.example.skysport1.dto.request.AddressRequest;
import com.example.skysport1.dto.request.ChangePasswordRequest;
import com.example.skysport1.dto.request.ProfileUpdateRequest;
import com.example.skysport1.dto.request.CustomerUpdateRequest;
import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.AddressShipping;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.exception.DuplicateException;
import com.example.skysport1.repository.AddressShippingRepository;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/customer/profile")
@RequiredArgsConstructor
@Slf4j
public class CustomerProfileController {

    private final CustomerService customerService;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final AddressShippingRepository addressShippingRepository;

    /**
     * Xem thông tin cá nhân + danh sách địa chỉ
     */
    @GetMapping
    public String profile(Authentication auth, Model model) {
        try {
            String username = auth.getName();
            Account account = accountService.findByUsername(username);
            Customer customer = customerService.findByAccountId(account.getId());

            model.addAttribute("customer", customer);
            model.addAttribute("account", account);

            String customerId = customer.getId();
            List<AddressShipping> addresses = addressShippingRepository.findByCustomerId(customerId);

            // default lên đầu
            addresses.sort(Comparator.comparing((AddressShipping a) -> Boolean.TRUE.equals(a.getIsDefault()))
                    .reversed()
                    .thenComparing(a -> a.getId() == null ? 0 : a.getId()));

            model.addAttribute("addresses", addresses);
            model.addAttribute("addressRequest", new AddressRequest());

            AddressShipping defaultAddress = addresses.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                    .findFirst()
                    .orElse(addresses.stream().findFirst().orElse(null));

            model.addAttribute("defaultAddress", defaultAddress);
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

            CustomerUpdateRequest updateRequest = new CustomerUpdateRequest();
            updateRequest.setId(customer.getId());
            updateRequest.setFullName(request.getFullName());
            updateRequest.setPhone(request.getPhone());
            updateRequest.setEmail(request.getEmail());
            updateRequest.setGender(request.getGender());
            updateRequest.setDob(request.getDob());
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

    // ===== AddressShipping: CRUD (full, không đổi schema DB) =====

    @PostMapping("/address/add")
    public String addAddress(@Valid @ModelAttribute AddressRequest request,
                               BindingResult bindingResult,
                               Authentication auth,
                               RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", "Vui lòng kiểm tra thông tin địa chỉ");
            return "redirect:/customer/profile";
        }

        try {
            String username = auth.getName();
            Account account = accountService.findByUsername(username);
            Customer customer = customerService.findByAccountId(account.getId());
            String customerId = customer.getId();

            AddressShipping created = AddressShipping.builder()
                    .customer(customer)
                    .receiverName(request.getReceiverName())
                    .receiverPhone(request.getReceiverPhone())
                    .address(request.getAddress())
                    .isDefault(false)
                    .build();

            // set default nếu được chọn
            if (Boolean.TRUE.equals(request.getIsDefault())) {
                List<AddressShipping> old = addressShippingRepository.findByCustomerId(customerId);
                for (AddressShipping a : old) {
                    a.setIsDefault(false);
                }
                addressShippingRepository.saveAll(old);
                created.setIsDefault(true);
            }

            AddressShipping saved = addressShippingRepository.save(created);

            // nếu lần đầu tiên mà chưa có default thì auto default
            if (!Boolean.TRUE.equals(request.getIsDefault()) && saved != null) {
                List<AddressShipping> all = addressShippingRepository.findByCustomerId(customerId);
                boolean hasDefault = all.stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsDefault()));
                if (!hasDefault) {
                    saved.setIsDefault(true);
                    addressShippingRepository.save(saved);
                }
            }

            ra.addFlashAttribute("success", "Thêm địa chỉ thành công!");
        } catch (Exception e) {
            log.error("Error add address: {}", e.getMessage());
            ra.addFlashAttribute("error", "Không thể thêm địa chỉ: " + e.getMessage());
        }

        return "redirect:/customer/profile";
    }

    @PostMapping("/address/update")
    public String updateAddress(@Valid @ModelAttribute AddressRequest request,
                                  BindingResult bindingResult,
                                  Authentication auth,
                                  RedirectAttributes ra) {
        if (bindingResult.hasErrors() || request.getId() == null) {
            ra.addFlashAttribute("error", "Vui lòng kiểm tra thông tin địa chỉ");
            return "redirect:/customer/profile";
        }

        try {
            String username = auth.getName();
            Account account = accountService.findByUsername(username);
            Customer customer = customerService.findByAccountId(account.getId());
            String customerId = customer.getId();

            AddressShipping existing = addressShippingRepository.findById(request.getId()).orElse(null);
            if (existing == null || existing.getCustomer() == null || existing.getCustomer().getId() == null
                    || !existing.getCustomer().getId().equals(customerId)) {
                ra.addFlashAttribute("error", "Không tìm thấy địa chỉ hợp lệ");
                return "redirect:/customer/profile";
            }

            existing.setReceiverName(request.getReceiverName());
            existing.setReceiverPhone(request.getReceiverPhone());
            existing.setAddress(request.getAddress());

            if (Boolean.TRUE.equals(request.getIsDefault())) {
                List<AddressShipping> old = addressShippingRepository.findByCustomerId(customerId);
                for (AddressShipping a : old) {
                    a.setIsDefault(false);
                }
                addressShippingRepository.saveAll(old);
                existing.setIsDefault(true);
            }

            addressShippingRepository.save(existing);
            ra.addFlashAttribute("success", "Cập nhật địa chỉ thành công!");
        } catch (Exception e) {
            log.error("Error update address: {}", e.getMessage());
            ra.addFlashAttribute("error", "Không thể cập nhật địa chỉ: " + e.getMessage());
        }

        return "redirect:/customer/profile";
    }

    @PostMapping("/address/delete")
    public String deleteAddress(@RequestParam("id") Integer id,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        if (id == null) {
            ra.addFlashAttribute("error", "Địa chỉ không hợp lệ");
            return "redirect:/customer/profile";
        }

        try {
            String username = auth.getName();
            Account account = accountService.findByUsername(username);
            Customer customer = customerService.findByAccountId(account.getId());
            String customerId = customer.getId();

            AddressShipping existing = addressShippingRepository.findById(id).orElse(null);
            if (existing == null || existing.getCustomer() == null || existing.getCustomer().getId() == null
                    || !existing.getCustomer().getId().equals(customerId)) {
                ra.addFlashAttribute("error", "Không tìm thấy địa chỉ hợp lệ");
                return "redirect:/customer/profile";
            }

            boolean wasDefault = Boolean.TRUE.equals(existing.getIsDefault());
            addressShippingRepository.delete(existing);

            if (wasDefault) {
                List<AddressShipping> remain = addressShippingRepository.findByCustomerId(customerId);
                if (!remain.isEmpty()) {
                    AddressShipping newDefault = remain.get(0);
                    newDefault.setIsDefault(true);
                    addressShippingRepository.save(newDefault);
                }
            }

            ra.addFlashAttribute("success", "Xóa địa chỉ thành công!");
        } catch (Exception e) {
            log.error("Error delete address: {}", e.getMessage());
            ra.addFlashAttribute("error", "Không thể xóa địa chỉ: " + e.getMessage());
        }

        return "redirect:/customer/profile";
    }

    // ===== Đổi mật khẩu =====

    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("changePasswordRequest", new com.example.skysport1.dto.request.ChangePasswordRequest());
        model.addAttribute("title", "Đổi mật khẩu");
        model.addAttribute("pageContent", "customer/profile/change-password");
        return "layouts/customer/layout";
    }

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

            if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
                model.addAttribute("error", "Mật khẩu hiện tại không đúng");
                model.addAttribute("title", "Đổi mật khẩu");
                model.addAttribute("pageContent", "customer/profile/change-password");
                return "layouts/customer/layout";
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                model.addAttribute("error", "Mật khẩu xác nhận không khớp");
                model.addAttribute("title", "Đổi mật khẩu");
                model.addAttribute("pageContent", "customer/profile/change-password");
                return "layouts/customer/layout";
            }

            accountService.updatePassword(username, passwordEncoder.encode(request.getNewPassword()));
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
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