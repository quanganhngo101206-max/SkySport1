package com.example.skysport1.controller.staff;

import com.example.skysport1.entity.ImportOrder;
import com.example.skysport1.entity.ImportOrderDetail;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.enums.ImportOrderStatus;
import com.example.skysport1.repository.ImportOrderRepository;
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
@RequestMapping("/staff/imports")
@RequiredArgsConstructor
@Slf4j
public class StaffImportController {

    private final ImportOrderService importOrderService;
    private final StaffService staffService;
    private final ImportOrderRepository importOrderRepository;
    private final SupplierService supplierService;
    private final ProductDetailService productDetailService;

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

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("productDetails", productDetailService.findAllActive());
        model.addAttribute("title", "Tạo phiếu nhập");
        model.addAttribute("pageContent", "staff/import/create");
        return "layouts/staff/layout";
    }

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
            ra.addFlashAttribute("success", "Đã tạo phiếu nhập " + order.getId() + " — chờ admin duyệt");
            return "redirect:/staff/imports/" + order.getId();
        } catch (Exception e) {
            log.error("Error creating import order: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/imports/create";
        }
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

    // Lưu ý: KHÔNG đặt endpoint approve/reject ở đây. Staff chỉ có quyền tạo
    // phiếu nhập; duyệt/từ chối là quyền riêng của Admin (AdminImportOrderController)
    // để tránh việc staff tự duyệt phiếu do chính mình tạo (self-approval).

    // ── Sửa phiếu nhập (chỉ phiếu do chính mình tạo, còn Chờ duyệt) ────────

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable String id, Model model,
                           Authentication auth, RedirectAttributes ra) {
        try {
            ImportOrder order = importOrderService.findById(id);
            if (order.getStatus() != ImportOrderStatus.PENDING.getValue()) {
                ra.addFlashAttribute("error", "Chỉ có thể sửa phiếu nhập đang ở trạng thái Chờ duyệt");
                return "redirect:/staff/imports/" + id;
            }
            if (!canEdit(order, auth)) {
                ra.addFlashAttribute("error", "Bạn chỉ có thể sửa phiếu nhập do chính mình tạo");
                return "redirect:/staff/imports/" + id;
            }

            List<ImportOrderDetail> details = importOrderService.findDetails(id);
            model.addAttribute("order", order);
            model.addAttribute("details", details);
            model.addAttribute("suppliers", supplierService.findAll());
            model.addAttribute("productDetails", productDetailService.findAllActive());
            model.addAttribute("title", "Sửa phiếu nhập");
            model.addAttribute("pageContent", "staff/import/edit");
            return "layouts/staff/layout";
        } catch (Exception e) {
            log.error("Error loading import order {} for edit: {}", id, e.getMessage());
            ra.addFlashAttribute("error", "Không tìm thấy phiếu nhập");
            return "redirect:/staff/imports";
        }
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable String id,
                         @RequestParam(required = false) String supplierId,
                         @RequestParam(required = false) String note,
                         @RequestParam("productDetailIds") List<Integer> productDetailIds,
                         @RequestParam("quantities") List<Integer> quantities,
                         @RequestParam("importPrices") List<BigDecimal> importPrices,
                         Authentication auth,
                         RedirectAttributes ra) {
        try {
            ImportOrder order = importOrderService.findById(id);
            if (!canEdit(order, auth)) {
                ra.addFlashAttribute("error", "Bạn chỉ có thể sửa phiếu nhập do chính mình tạo");
                return "redirect:/staff/imports/" + id;
            }

            Staff staff = staffService.findByAccountUsername(auth.getName());

            List<ImportOrderDetail> details = new ArrayList<>();
            for (int i = 0; i < productDetailIds.size(); i++) {
                ImportOrderDetail detail = new ImportOrderDetail();
                detail.setProductDetail(productDetailService.findById(productDetailIds.get(i)));
                detail.setQuantity(quantities.get(i));
                detail.setImportPrice(importPrices.get(i));
                details.add(detail);
            }

            importOrderService.update(id, supplierId, note, details, staff.getId());
            ra.addFlashAttribute("success", "Đã cập nhật phiếu nhập " + id);
            return "redirect:/staff/imports/" + id;
        } catch (Exception e) {
            log.error("Error updating import order {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/imports/edit/" + id;
        }
    }

    /**
     * Chỉ staff tạo ra phiếu mới được sửa. Admin đang mượn giao diện Staff
     * (route /staff/** cho phép cả ROLE_ADMIN) thì luôn được phép, kể cả khi
     * Admin không có Staff record tương ứng.
     */
    private boolean canEdit(ImportOrder order, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;

        if (order.getStaff() == null) return false;
        try {
            Staff current = staffService.findByAccountUsername(auth.getName());
            return order.getStaff().getId().equals(current.getId());
        } catch (Exception e) {
            return false;
        }
    }
}