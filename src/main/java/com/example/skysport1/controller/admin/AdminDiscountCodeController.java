package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.DiscountCode;
import com.example.skysport1.enums.DiscountType;
import com.example.skysport1.service.DiscountCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/discount-codes")
@RequiredArgsConstructor
@Slf4j
public class AdminDiscountCodeController {

    private final DiscountCodeService discountCodeService;

    // ── Danh sách ────────────────────────────────────────────────────────

    @GetMapping
    public String list(Model model) {
        model.addAttribute("discountCodes", discountCodeService.findAll());
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("title", "Quản lý mã giảm giá");
        model.addAttribute("pageContent", "admin/discount-code/list");
        return "layouts/adminlte/layout";
    }

    // ── Form tạo mới ─────────────────────────────────────────────────────

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("discountCode", new DiscountCode());
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("title", "Tạo mã giảm giá");
        model.addAttribute("pageContent", "admin/discount-code/create");
        return "layouts/adminlte/layout";
    }

    // ── Lưu mã giảm giá ──────────────────────────────────────────────────

    @PostMapping("/save")
    public String save(@RequestParam String code,
                       @RequestParam String name,
                       @RequestParam Integer discountType,
                       @RequestParam BigDecimal discountValue,
                       @RequestParam(required = false) BigDecimal minOrderValue,
                       @RequestParam(required = false) BigDecimal maxDiscountValue,
                       @RequestParam(required = false) Integer quantity,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startDate,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endDate,
                       Authentication auth,
                       RedirectAttributes ra) {
        try {
            DiscountCode dc = DiscountCode.builder()
                    .code(code.trim().toUpperCase())
                    .name(name)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .minOrderValue(minOrderValue)
                    .maxDiscountValue(maxDiscountValue)
                    .quantity(quantity)
                    .startDate(startDate)
                    .endDate(endDate)
                    .createdBy(auth.getName())
                    .updatedBy(auth.getName())
                    .build();
            discountCodeService.save(dc);
            ra.addFlashAttribute("success", "Đã tạo mã giảm giá " + dc.getCode());
            log.info("DiscountCode '{}' created by {}", dc.getCode(), auth.getName());
        } catch (Exception e) {
            log.error("Error saving discount code: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/discount-codes/create";
        }
        return "redirect:/admin/discount-codes";
    }

    // ── Xóa (soft delete) ────────────────────────────────────────────────

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            discountCodeService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa mã giảm giá");
        } catch (Exception e) {
            log.error("Error deleting discount code {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/discount-codes";
    }
}