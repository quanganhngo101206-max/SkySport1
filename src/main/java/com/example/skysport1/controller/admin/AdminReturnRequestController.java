package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.ReturnRequest;
import com.example.skysport1.entity.ReturnRequestDetail;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.service.ReturnRequestService;
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
@RequestMapping("/admin/return-requests")
@RequiredArgsConstructor
@Slf4j
public class AdminReturnRequestController {

    private final ReturnRequestService returnRequestService;
    private final StaffService staffService;

    // ── Danh sách ────────────────────────────────────────────────────────

    @GetMapping
    public String list(@RequestParam(required = false) Integer status,
                       Model model) {
        List<ReturnRequest> requests = (status != null)
                ? returnRequestService.findByStatus(status)
                : returnRequestService.findAll();

        model.addAttribute("requests", requests);
        model.addAttribute("status", status);
        model.addAttribute("title", "Quản lý hoàn trả");
        model.addAttribute("pageContent", "admin/return-request/list");
        return "layouts/adminlte/layout";
    }

    // ── Chi tiết ─────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model, RedirectAttributes ra) {
        try {
            ReturnRequest request = returnRequestService.findById(id);
            List<ReturnRequestDetail> details = returnRequestService.findDetails(id);
            model.addAttribute("request", request);
            model.addAttribute("details", details);
            model.addAttribute("title", "Chi tiết yêu cầu hoàn trả");
            model.addAttribute("pageContent", "admin/return-request/detail");
            return "layouts/adminlte/layout";
        } catch (Exception e) {
            log.error("Error loading return request {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", "Không tìm thấy yêu cầu hoàn trả: " + id);
            return "redirect:/admin/return-requests";
        }
    }

    // ── Duyệt ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable String id,
                          @RequestParam(required = false) String note,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            returnRequestService.approve(id, staff.getId(), note);
            ra.addFlashAttribute("success", "Đã duyệt yêu cầu hoàn trả " + id);
        } catch (Exception e) {
            log.error("Error approving return request {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/return-requests/" + id;
    }

    // ── Từ chối ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable String id,
                         @RequestParam(required = false) String note,
                         Authentication auth,
                         RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            returnRequestService.reject(id, staff.getId(), note);
            ra.addFlashAttribute("success", "Đã từ chối yêu cầu hoàn trả " + id);
        } catch (Exception e) {
            log.error("Error rejecting return request {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/return-requests/" + id;
    }

    // ── Xác nhận hoàn tiền ───────────────────────────────────────────────

    @PostMapping("/{id}/confirm-refund")
    public String confirmRefund(@PathVariable String id,
                                @RequestParam(required = false) String note,
                                Authentication auth,
                                RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            returnRequestService.confirmRefund(id, staff.getId(), note);
            ra.addFlashAttribute("success", "Đã xác nhận hoàn tiền cho yêu cầu " + id);
        } catch (Exception e) {
            log.error("Error confirming refund for {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/return-requests/" + id;
    }
}