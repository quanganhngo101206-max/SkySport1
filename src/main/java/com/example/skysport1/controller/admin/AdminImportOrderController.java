package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.ImportOrder;
import com.example.skysport1.entity.ImportOrderDetail;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.enums.ImportOrderStatus;
import com.example.skysport1.service.ImportOrderService;
import com.example.skysport1.service.ProductDetailService;
import com.example.skysport1.service.StaffService;
import com.example.skysport1.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/import-orders")
@RequiredArgsConstructor
@Slf4j
public class AdminImportOrderController {

    private final ImportOrderService importOrderService;
    private final SupplierService supplierService;
    private final StaffService staffService;
    private final ProductDetailService productDetailService;

    // ── Danh sách phiếu nhập ──────────────────────────────────────────────

    @GetMapping
    public String list(@RequestParam(required = false) Integer status,
                       Model model) {
        List<ImportOrder> orders = (status != null)
                ? importOrderService.findByStatus(status)
                : importOrderService.findAll();

        model.addAttribute("orders", orders);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ImportOrderStatus.values());
        model.addAttribute("title", "Quản lý phiếu nhập");
        model.addAttribute("pageContent", "admin/import-order/list");
        return "layouts/adminlte/layout";
    }

    // ── Chi tiết phiếu nhập ───────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model, RedirectAttributes ra) {
        try {
            ImportOrder order = importOrderService.findById(id);
            List<ImportOrderDetail> details = importOrderService.findDetails(id);

            model.addAttribute("order", order);
            model.addAttribute("details", details);
            model.addAttribute("title", "Chi tiết phiếu nhập");
            model.addAttribute("pageContent", "admin/import-order/detail");
            return "layouts/adminlte/layout";
        } catch (Exception e) {
            log.error("Error loading import order {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", "Không tìm thấy phiếu nhập: " + id);
            return "redirect:/admin/import-orders";
        }
    }

    // ── Form tạo phiếu nhập ───────────────────────────────────────────────

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("productDetails", productDetailService.findAllActive());
        model.addAttribute("title", "Tạo phiếu nhập");
        model.addAttribute("pageContent", "admin/import-order/create");
        return "layouts/adminlte/layout";
    }

    // ── Xử lý tạo phiếu nhập ─────────────────────────────────────────────

    @PostMapping("/create")
    public String create(@RequestParam(required = false) String supplierId,
                         @RequestParam(required = false) String note,
                         @RequestParam("productDetailIds") List<Integer> productDetailIds,
                         @RequestParam("quantities") List<Integer> quantities,
                         @RequestParam("importPrices") List<BigDecimal> importPrices,
                         Authentication auth,
                         RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());

            List<ImportOrderDetail> details = new ArrayList<>();
            for (int i = 0; i < productDetailIds.size(); i++) {
                ImportOrderDetail detail = new ImportOrderDetail();
                detail.setProductDetail(productDetailService.findById(productDetailIds.get(i)));
                detail.setQuantity(quantities.get(i));
                detail.setImportPrice(importPrices.get(i));
                details.add(detail);
            }

            ImportOrder order = importOrderService.create(supplierId, staff.getId(), note, details);
            ra.addFlashAttribute("success", "Đã tạo phiếu nhập " + order.getId());
            return "redirect:/admin/import-orders/" + order.getId();
        } catch (Exception e) {
            log.error("Error creating import order: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/import-orders/create";
        }
    }

    // ── Duyệt phiếu nhập ─────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable String id,
                          @RequestParam(required = false) String note,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            importOrderService.approve(id, staff.getId(), note);
            ra.addFlashAttribute("success", "Đã duyệt phiếu nhập " + id + " — tồn kho đã được cập nhật");
        } catch (Exception e) {
            log.error("Error approving import order {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/import-orders/" + id;
    }

    // ── Từ chối phiếu nhập ───────────────────────────────────────────────

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
            log.error("Error rejecting import order {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/import-orders/" + id;
    }
}