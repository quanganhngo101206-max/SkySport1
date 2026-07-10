package com.example.skysport1.controller.staff;

import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.repository.BillRepository;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/orders")
@RequiredArgsConstructor
@Slf4j
public class StaffOrderController {

    private final BillService billService;
    private final StaffService staffService;
    private final BillRepository billRepository;  // ✅ THÊM

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) Integer status,
                       Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bill> pageResult;

        if (status != null) {
            pageResult = billService.findByStatusWithCustomer(status, pageable);
        } else {
            pageResult = billService.findAllWithCustomer(pageable);
        }

        model.addAttribute("bills", pageResult.getContent());
        model.addAttribute("page", pageResult.getNumber());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("status", status);
        model.addAttribute("title", "Quản lý đơn hàng");
        model.addAttribute("pageContent", "staff/order/list");
        return "layouts/staff/layout";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model, RedirectAttributes ra) {
        try {
            Bill bill = billService.findById(id);
            model.addAttribute("bill", bill);
            model.addAttribute("title", "Chi tiết đơn hàng");
            model.addAttribute("pageContent", "staff/order/detail");
            return "layouts/staff/layout";
        } catch (Exception e) {
            log.error("Error loading order {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/staff/orders";
        }
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable String id,
                           @RequestParam(required = false) String note,
                           Authentication auth,
                           RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            billService.confirm(id, staff.getAccount().getId(), note);
            ra.addFlashAttribute("success", "Đã xác nhận đơn hàng " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/orders/" + id;
    }

    @PostMapping("/{id}/ship")
    public String ship(@PathVariable String id,
                        @RequestParam(required = false) String note,
                        Authentication auth,
                        RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            billService.startShipping(id, staff.getAccount().getId(), note);
            ra.addFlashAttribute("success", "Đã bắt đầu giao hàng " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/orders/" + id;
    }

    @PostMapping("/{id}/deliver")
    public String deliver(@PathVariable String id,
                          @RequestParam(required = false) String note,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            billService.markDelivered(id, staff.getAccount().getId(), note);
            ra.addFlashAttribute("success", "Đã xác nhận giao thành công " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable String id,
                          @RequestParam(required = false) String note,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            billService.cancel(id, staff.getAccount().getId(), note);
            ra.addFlashAttribute("success", "Đã hủy đơn hàng " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/orders/" + id;
    }

    // ✅ Customer xin hủy → Staff duyệt (cấp độ đơn)
    @PostMapping("/{id}/approve-cancel")
    public String approveCancel(@PathVariable String id,
                                 @RequestParam(required = false) String note,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            billService.approveCancelRequest(id, staff.getAccount().getId(), note);
            ra.addFlashAttribute("success", "Đã duyệt yêu cầu hủy đơn " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/orders/" + id;
    }

    // ✅ Customer xin hủy → Staff từ chối (cấp độ đơn)
    @PostMapping("/{id}/reject-cancel")
    public String rejectCancel(@PathVariable String id,
                                 @RequestParam(required = false) String note,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            billService.rejectCancelRequest(id, staff.getAccount().getId(), note);
            ra.addFlashAttribute("success", "Đã từ chối yêu cầu hủy đơn " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/orders/" + id;
    }

    // ✅ Duyệt hủy từng sản phẩm trong đơn (BillDetail)
    @PostMapping("/{billId}/details/{billDetailId}/approve-cancel")
    public String approveCancelBillDetail(@PathVariable String billId,
                                             @PathVariable Integer billDetailId,
                                             @RequestParam(required = false) String note,
                                             Authentication auth,
                                             RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            billService.approveCancelBillDetail(billId, billDetailId, staff.getAccount().getId(), note);
            ra.addFlashAttribute("success", "Đã duyệt hủy sản phẩm trong đơn " + billId);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/orders/" + billId;
    }

    // ❌ Từ chối hủy từng sản phẩm trong đơn (BillDetail)
    @PostMapping("/{billId}/details/{billDetailId}/reject-cancel")
    public String rejectCancelBillDetail(@PathVariable String billId,
                                           @PathVariable Integer billDetailId,
                                           @RequestParam(required = false) String note,
                                           Authentication auth,
                                           RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            billService.rejectCancelBillDetail(billId, billDetailId, staff.getAccount().getId(), note);
            ra.addFlashAttribute("success", "Đã từ chối hủy sản phẩm trong đơn " + billId);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/orders/" + billId;
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable String id,
                             @RequestParam(required = false) String note,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            billService.complete(id, staff.getAccount().getId(), note);
            ra.addFlashAttribute("success", "Đã hoàn thành đơn hàng " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/orders/" + id;
    }
}