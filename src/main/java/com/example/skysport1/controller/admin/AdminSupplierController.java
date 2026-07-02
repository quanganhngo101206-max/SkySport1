package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.Supplier;
import com.example.skysport1.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/suppliers")
@RequiredArgsConstructor
@Slf4j
public class AdminSupplierController {

    private final SupplierService supplierService;

    // ── Danh sách ────────────────────────────────────────────────────────

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       Model model) {
        model.addAttribute("suppliers",
                keyword != null && !keyword.isBlank()
                        ? supplierService.search(keyword)
                        : supplierService.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("title", "Quản lý nhà cung cấp");
        model.addAttribute("pageContent", "admin/supplier/list");
        return "layouts/adminlte/layout";
    }

    // ── Form tạo mới ─────────────────────────────────────────────────────

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("title", "Thêm nhà cung cấp");
        model.addAttribute("pageContent", "admin/supplier/create");
        return "layouts/adminlte/layout";
    }

    // ── Form sửa ─────────────────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("supplier", supplierService.findById(id));
            model.addAttribute("title", "Sửa nhà cung cấp");
            model.addAttribute("pageContent", "admin/supplier/edit");
            return "layouts/adminlte/layout";
        } catch (Exception e) {
            log.error("Error loading supplier {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", "Không tìm thấy nhà cung cấp: " + id);
            return "redirect:/admin/suppliers";
        }
    }

    // ── Lưu mới ──────────────────────────────────────────────────────────

    @PostMapping("/save")
    public String save(@ModelAttribute Supplier supplier,
                       Authentication auth,
                       RedirectAttributes ra) {
        try {
            supplier.setCreatedBy(auth.getName());
            supplier.setUpdatedBy(auth.getName());
            supplierService.save(supplier);
            ra.addFlashAttribute("success", "Đã thêm nhà cung cấp: " + supplier.getName());
            log.info("Supplier '{}' created by {}", supplier.getName(), auth.getName());
        } catch (Exception e) {
            log.error("Error saving supplier: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    // ── Cập nhật ─────────────────────────────────────────────────────────

    @PostMapping("/update")
    public String update(@ModelAttribute Supplier supplier,
                         Authentication auth,
                         RedirectAttributes ra) {
        try {
            supplier.setUpdatedBy(auth.getName());
            supplierService.update(supplier.getId(), supplier);
            ra.addFlashAttribute("success", "Đã cập nhật nhà cung cấp: " + supplier.getName());
            log.info("Supplier '{}' updated by {}", supplier.getId(), auth.getName());
        } catch (Exception e) {
            log.error("Error updating supplier: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    // ── Xóa (soft delete) ────────────────────────────────────────────────

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            supplierService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa nhà cung cấp");
        } catch (Exception e) {
            log.error("Error deleting supplier {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }
}