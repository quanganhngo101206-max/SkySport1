package com.example.skysport1.controller.staff;

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
@RequestMapping("/staff/returns")
@RequiredArgsConstructor
@Slf4j
public class StaffReturnController {

    private final ReturnRequestService returnRequestService;
    private final StaffService staffService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer status, Model model) {
        List<ReturnRequest> requests = (status != null)
                ? returnRequestService.findByStatus(status)
                : returnRequestService.findAll();

        model.addAttribute("requests", requests);
        model.addAttribute("status", status);
        model.addAttribute("title", "Quản lý hoàn trả");
        model.addAttribute("pageContent", "staff/return/list");
        return "layouts/staff/layout";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model, RedirectAttributes ra) {
        try {
            ReturnRequest request = returnRequestService.findById(id);
            List<ReturnRequestDetail> details = returnRequestService.findDetails(id);
            model.addAttribute("request", request);
            model.addAttribute("details", details);
            model.addAttribute("title", "Chi tiết hoàn trả");
            model.addAttribute("pageContent", "staff/return/detail");
            return "layouts/staff/layout";
        } catch (Exception e) {
            log.error("Error loading return request {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", "Không tìm thấy yêu cầu hoàn trả");
            return "redirect:/staff/returns";
        }
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable String id,
                          @RequestParam(required = false) String note,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            returnRequestService.approve(id, staff.getId(), note);
            ra.addFlashAttribute("success", "Đã duyệt hoàn trả " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/returns/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable String id,
                         @RequestParam(required = false) String note,
                         Authentication auth,
                         RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            returnRequestService.reject(id, staff.getId(), note);
            ra.addFlashAttribute("success", "Đã từ chối hoàn trả " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/returns/" + id;
    }

    @PostMapping("/{id}/confirm-refund")
    public String confirmRefund(@PathVariable String id,
                                @RequestParam(required = false) String note,
                                Authentication auth,
                                RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            returnRequestService.confirmRefund(id, staff.getId(), note);
            ra.addFlashAttribute("success", "Đã xác nhận hoàn tiền cho " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/returns/" + id;
    }
}