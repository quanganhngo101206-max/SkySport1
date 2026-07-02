package com.example.skysport1.controller.admin;

import com.example.skysport1.dto.request.CustomerUpdateRequest;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.exception.DuplicateException;
import com.example.skysport1.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@Slf4j
public class AdminCustomerController {

    private final CustomerService customerService;

    /**
     * Danh sách khách hàng
     */
    @GetMapping
    public String list(Model model) {
        List<Customer> customers = customerService.findAll();
        model.addAttribute("customers", customers);
        model.addAttribute("title", "Danh sách khách hàng");
        model.addAttribute("pageContent", "admin/customer/list");
        return "layouts/adminlte/layout";
    }

    /**
     * Form sửa khách hàng
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable String id, Model model) {
        Customer customer = customerService.findById(id);
        model.addAttribute("customer", customer);
        model.addAttribute("title", "Sửa khách hàng");
        model.addAttribute("pageContent", "admin/customer/edit");
        return "layouts/adminlte/layout";
    }

    /**
     * Cập nhật khách hàng
     */
    @PostMapping("/update")
    public String update(@Valid @ModelAttribute CustomerUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            // Load lại form với lỗi validate hiển thị ngay, không redirect mất dữ liệu
            Customer customer = customerService.findById(request.getId());
            model.addAttribute("customer", customer);
            model.addAttribute("errors", bindingResult.getAllErrors());
            model.addAttribute("title", "Sửa khách hàng");
            model.addAttribute("pageContent", "admin/customer/edit");
            return "layouts/adminlte/layout";
        }
        try {
            customerService.update(request.getId(), request);
            ra.addFlashAttribute("success", "Cập nhật khách hàng thành công!");
            log.info("Cập nhật khách hàng: {} ({})", request.getFullName(), request.getId());
        } catch (DuplicateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/customers/edit/" + request.getId();
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật khách hàng: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/customers/edit/" + request.getId();
        }
        return "redirect:/admin/customers";
    }

    /**
     * Xóa khách hàng (soft delete)
     */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            customerService.delete(id);
            ra.addFlashAttribute("success", "Xóa khách hàng thành công!");
            log.info("Xóa khách hàng: {}", id);
        } catch (Exception e) {
            log.error("Lỗi khi xóa khách hàng: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }
}