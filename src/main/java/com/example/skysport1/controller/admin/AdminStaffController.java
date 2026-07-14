package com.example.skysport1.controller.admin;

import com.example.skysport1.dto.request.StaffChangePasswordRequest;
import com.example.skysport1.dto.request.StaffCreateRequest;
import com.example.skysport1.dto.request.StaffUpdateRequest;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.exception.DuplicateException;
import com.example.skysport1.service.StaffService;
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
@RequestMapping("/admin/staffs")
@RequiredArgsConstructor
@Slf4j
public class AdminStaffController {

    private final StaffService staffService;

    /**
     * Danh sách nhân viên
     */
    @GetMapping
    public String list(Model model) {
        List<Staff> staffs = staffService.findAll();
        model.addAttribute("staffs", staffs);
        model.addAttribute("title", "Danh sách nhân viên");
        model.addAttribute("pageContent", "admin/staff/list");
        return "layouts/adminlte/layout";
    }

    /**
     * Form thêm nhân viên
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("staffRequest", new StaffCreateRequest());
        model.addAttribute("title", "Thêm nhân viên");
        model.addAttribute("pageContent", "admin/staff/create");
        return "layouts/adminlte/layout";
    }

    /**
     * Lưu nhân viên mới
     */
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("staffRequest") StaffCreateRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors());
            model.addAttribute("title", "Thêm nhân viên");
            model.addAttribute("pageContent", "admin/staff/create");
            return "layouts/adminlte/layout";
        }
        try {
            Staff staff = staffService.create(request);
            ra.addFlashAttribute("success", "Thêm nhân viên thành công!");
            log.info("Thêm nhân viên mới: {} ({}), username: {}", staff.getFullName(), staff.getId(), request.getUsername());
            return "redirect:/admin/staffs";
        } catch (DuplicateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/staffs/create";
        } catch (Exception e) {
            log.error("Lỗi khi thêm nhân viên: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/staffs/create";
        }
    }

    /**
     * Form sửa nhân viên (Hướng B: bind theo StaffUpdateRequest)
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable String id, Model model) {
        StaffUpdateRequest dto = staffService.findUpdateRequestById(id);
        model.addAttribute("staffRequest", dto);
        model.addAttribute("title", "Sửa nhân viên");
        model.addAttribute("pageContent", "admin/staff/edit");
        return "layouts/adminlte/layout";
    }

    /**
     * Cập nhật nhân viên (Hướng B: bind theo StaffUpdateRequest)
     */
    @PostMapping("/update")
    public String update(@Valid @ModelAttribute StaffUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("staffRequest", request);
            model.addAttribute("errors", bindingResult.getAllErrors());
            model.addAttribute("title", "Sửa nhân viên");
            model.addAttribute("pageContent", "admin/staff/edit");
            return "layouts/adminlte/layout";
        }
        try {
            Staff staff = staffService.update(request.getId(), request);
            ra.addFlashAttribute("success", "Cập nhật nhân viên thành công!");
            log.info("Cập nhật nhân viên: {} ({})", staff.getFullName(), staff.getId());
            return "redirect:/admin/staffs";
        } catch (DuplicateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/staffs/edit/" + request.getId();
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật nhân viên: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/staffs/edit/" + request.getId();
        }
    }

    /**
     * Đổi mật khẩu nhân viên — endpoint RIÊNG khỏi update thông tin cơ bản,
     * để tránh admin vô tình đổi mật khẩu người khác khi chỉ sửa SĐT/email.
     */
    @PostMapping("/{id}/change-password")
    public String changePassword(@PathVariable String id,
                                 @Valid @ModelAttribute StaffChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/staffs/edit/" + id;
        }
        try {
            staffService.changePassword(id, request.getNewPassword());
            ra.addFlashAttribute("success", "Đổi mật khẩu nhân viên thành công!");
            log.info("Admin đổi mật khẩu nhân viên: {}", id);
        } catch (Exception e) {
            log.error("Lỗi khi đổi mật khẩu nhân viên {}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/staffs/edit/" + id;
    }

    /**
     * Xóa nhân viên (soft delete)
     */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            staffService.delete(id);
            ra.addFlashAttribute("success", "Xóa nhân viên thành công!");
            log.info("Xóa nhân viên: {}", id);
        } catch (Exception e) {
            log.error("Lỗi khi xóa nhân viên: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/staffs";
    }
}