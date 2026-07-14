package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Supplier;
import com.example.skysport1.repository.SupplierRepository;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/suppliers")
@RequiredArgsConstructor
@Slf4j
public class AdminSupplierController {

    private final SupplierService supplierService;
    private final AccountService accountService;
    private final SupplierRepository supplierRepository;

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
    public String save(@Valid @ModelAttribute Supplier supplier,
                       BindingResult bindingResult,
                       Authentication auth,
                       RedirectAttributes ra) {
        // 1. Validate định dạng (bắt buộc tên, đúng định dạng SĐT/email) —
        // khai báo bằng @NotBlank/@Pattern ngay trên entity Supplier.
        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .findFirst().orElse("Dữ liệu không hợp lệ");
            log.warn("Validation failed when saving supplier: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", firstError);
            return "redirect:/admin/suppliers";
        }

        // 2. Validate nghiệp vụ: không cho trùng tên/SĐT/email với NCC khác
        // (JSR-380 ở bước 1 không kiểm tra được việc này vì cần query DB).
        String duplicateError = checkDuplicate(supplier, null);
        if (duplicateError != null) {
            ra.addFlashAttribute("error", duplicateError);
            return "redirect:/admin/suppliers";
        }

        try {
            String accountId = resolveAccountId(auth);
            supplier.setCreatedBy(accountId);
            supplier.setUpdatedBy(accountId);
            supplierService.save(supplier);
            ra.addFlashAttribute("success", "Đã thêm nhà cung cấp: " + supplier.getName());
            log.info("Supplier '{}' created by {}", supplier.getName(), accountId);
        } catch (Exception e) {
            log.error("Error saving supplier: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    // ── Cập nhật ─────────────────────────────────────────────────────────

    @PostMapping("/update")
    public String update(@Valid @ModelAttribute Supplier supplier,
                         BindingResult bindingResult,
                         Authentication auth,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .findFirst().orElse("Dữ liệu không hợp lệ");
            log.warn("Validation failed when updating supplier: {}", bindingResult.getAllErrors());
            ra.addFlashAttribute("error", firstError);
            return "redirect:/admin/suppliers";
        }

        // Loại trừ chính bản ghi đang sửa (supplier.getId()) khỏi check trùng.
        String duplicateError = checkDuplicate(supplier, supplier.getId());
        if (duplicateError != null) {
            ra.addFlashAttribute("error", duplicateError);
            return "redirect:/admin/suppliers";
        }

        try {
            String accountId = resolveAccountId(auth);
            supplier.setUpdatedBy(accountId);
            supplierService.update(supplier.getId(), supplier);
            ra.addFlashAttribute("success", "Đã cập nhật nhà cung cấp: " + supplier.getName());
            log.info("Supplier '{}' updated by {}", supplier.getId(), accountId);
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

    /**
     * @param excludeId null khi tạo mới; = id đang sửa khi update (để không
     *                  tự báo trùng với chính bản ghi đó).
     * @return null nếu không trùng, ngược lại trả về thông báo lỗi.
     */
    private String checkDuplicate(Supplier supplier, String excludeId) {
        boolean nameDup = excludeId == null
                ? supplierRepository.existsByNameIgnoreCaseAndDeleteFlagFalse(supplier.getName())
                : supplierRepository.existsByNameIgnoreCaseAndDeleteFlagFalseAndIdNot(supplier.getName(), excludeId);
        if (nameDup) {
            return "Tên nhà cung cấp \"" + supplier.getName() + "\" đã tồn tại";
        }

        if (supplier.getPhone() != null && !supplier.getPhone().isBlank()) {
            boolean phoneDup = excludeId == null
                    ? supplierRepository.existsByPhone(supplier.getPhone())
                    : supplierRepository.existsByPhoneAndIdNot(supplier.getPhone(), excludeId);
            if (phoneDup) {
                return "Số điện thoại \"" + supplier.getPhone() + "\" đã được dùng cho nhà cung cấp khác";
            }
        }

        if (supplier.getEmail() != null && !supplier.getEmail().isBlank()) {
            boolean emailDup = excludeId == null
                    ? supplierRepository.existsByEmail(supplier.getEmail())
                    : supplierRepository.existsByEmailAndIdNot(supplier.getEmail(), excludeId);
            if (emailDup) {
                return "Email \"" + supplier.getEmail() + "\" đã được dùng cho nhà cung cấp khác";
            }
        }

        return null;
    }

    /**
     * created_by/updated_by của Supplier có ràng buộc FOREIGN KEY trỏ về
     * dbo.Account.id (constraint FK__Supplier__create__0B5CAFEA) — nên KHÔNG
     * được lưu thẳng auth.getName() (username đăng nhập, vd "admin1") vào đó,
     * mà phải resolve ra đúng Account.id (vd "ACC001") trước. Trước đây code
     * lưu thẳng username nên mọi lần tạo/sửa Supplier đều bị SQL Server chặn
     * với lỗi "INSERT statement conflicted with the FOREIGN KEY constraint".
     */
    private String resolveAccountId(Authentication auth) {
        Account account = accountService.findByUsername(auth.getName());
        return account.getId();
    }
}