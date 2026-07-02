package com.example.skysport1.controller.staff;

import com.example.skysport1.entity.ImportOrder;
import com.example.skysport1.entity.ImportOrderDetail;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.enums.ImportOrderStatus;
import com.example.skysport1.repository.ImportOrderRepository;
import com.example.skysport1.service.ImportOrderService;
import com.example.skysport1.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/staff/imports")
@RequiredArgsConstructor
@Slf4j
public class StaffImportController {

    private final ImportOrderService importOrderService;
    private final StaffService staffService;
    private final ImportOrderRepository importOrderRepository;

    @GetMapping
    public String list(@RequestParam(required = false) Integer status, Model model) {
        List<ImportOrder> orders = (status != null)
                ? importOrderRepository.findByStatusWithSupplier(status)
                : importOrderRepository.findAllWithSupplier();

        model.addAttribute("orders", orders);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ImportOrderStatus.values());
        model.addAttribute("title", "Quản lý phiếu nhập");
        model.addAttribute("pageContent", "staff/import/list");
        return "layouts/staff/layout";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model, RedirectAttributes ra) {
        try {
            ImportOrder order = importOrderService.findById(id);
            List<ImportOrderDetail> details = importOrderService.findDetails(id);
            model.addAttribute("order", order);
            model.addAttribute("details", details);
            model.addAttribute("title", "Chi tiết phiếu nhập");
            model.addAttribute("pageContent", "staff/import/detail");
            return "layouts/staff/layout";
        } catch (Exception e) {
            log.error("Error loading import order {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", "Không tìm thấy phiếu nhập");
            return "redirect:/staff/imports";
        }
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable String id,
                          @RequestParam(required = false) String note,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            importOrderService.approve(id, staff.getId(), note);
            ra.addFlashAttribute("success", "Đã duyệt phiếu nhập " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/imports/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable String id,
                         @RequestParam(required = false) String note,
                         Authentication auth,
                         RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            importOrderService.reject(id, staff.getId(), note);
            ra.addFlashAttribute("success", "Đã từ chối phiếu nhập " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/imports/" + id;
    }
}