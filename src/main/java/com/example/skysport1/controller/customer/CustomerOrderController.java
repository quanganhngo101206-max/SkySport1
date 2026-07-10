package com.example.skysport1.controller.customer;

import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.OrderStatusHistory;
import com.example.skysport1.repository.OrderStatusHistoryRepository;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer/orders")
@RequiredArgsConstructor
@Slf4j
public class CustomerOrderController {

    private final BillService billService;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    /**
     * Danh sách đơn hàng của customer
     */
    @GetMapping
    public String list(Authentication auth, Model model) {
        String customerId = getCurrentCustomerId(auth);
        List<Bill> bills = billService.findByCustomerId(customerId);

        model.addAttribute("bills", bills);
        model.addAttribute("title", "Lịch sử đơn hàng");
        model.addAttribute("pageContent", "customer/order/list");
        return "layouts/customer/layout";
    }

    /**
     * Chi tiết đơn hàng
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            String customerId = getCurrentCustomerId(auth);
            Bill bill = billService.findById(id);

            // Kiểm tra đơn hàng thuộc customer
            if (bill.getCustomer() == null || !bill.getCustomer().getId().equals(customerId)) {
                ra.addFlashAttribute("error", "Bạn không có quyền xem đơn hàng này");
                return "redirect:/customer/orders";
            }

            // Lấy timeline theo thứ tự insert (IDENTITY id) để ổn định
            List<OrderStatusHistory> timeline =
                    orderStatusHistoryRepository.findByBillIdOrderByIdAsc(id);

            model.addAttribute("bill", bill);
            model.addAttribute("timeline", timeline);
            model.addAttribute("title", "Chi tiết đơn hàng #" + id);
            model.addAttribute("pageContent", "customer/order/detail");
            return "layouts/customer/layout";

        } catch (Exception e) {
            log.error("Error loading order detail: {}", e.getMessage());
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/customer/orders";
        }
    }

    /**
     * Hủy đơn (customer)
     * - PENDING(1): hủy ngay (cancel cũ)
     * - CONFIRMED(2): customer request hủy (chuyển sang CANCEL_REQUESTED)
     */
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable String id,
                          Authentication auth,
                          @RequestParam(required = false) String note,
                          RedirectAttributes ra) {
        try {
            String customerId = getCurrentCustomerId(auth);
            Bill bill = billService.findById(id);

            // Kiểm tra quyền: bill phải thuộc customer
            if (bill.getCustomer() == null || !bill.getCustomer().getId().equals(customerId)) {
                ra.addFlashAttribute("error", "Bạn không có quyền hủy đơn hàng này");
                return "redirect:/customer/orders/" + id;
            }

            int status = bill.getStatus();

            if (status == com.example.skysport1.enums.OrderStatus.PENDING.getValue()) {
                billService.cancel(id, null, note);
                ra.addFlashAttribute("success", "Đã hủy đơn hàng " + id);
            } else if (status == com.example.skysport1.enums.OrderStatus.CONFIRMED.getValue()) {
                billService.requestCancel(id, note != null ? note : "Khách hàng hủy đơn");
                ra.addFlashAttribute("success", "Đã gửi yêu cầu hủy đơn hàng " + id);
            } else {
                ra.addFlashAttribute("error", "Đơn hàng không thể hủy ở trạng thái hiện tại");
            }

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/customer/orders/" + id;
    }

    /**
     * Hủy từng sản phẩm trong đơn (customer)
     * POST: /customer/orders/{billId}/details/{billDetailId}/cancel
     */
    @PostMapping("/{billId}/details/{billDetailId}/cancel")
    public String cancelBillDetail(@PathVariable String billId,
                                     @PathVariable Integer billDetailId,
                                     Authentication auth,
                                     @RequestParam(required = false) String note,
                                     RedirectAttributes ra) {
        try {
            String customerId = getCurrentCustomerId(auth);
            Bill bill = billService.findById(billId);

            // Quyền: bill phải thuộc customer
            if (bill.getCustomer() == null || !bill.getCustomer().getId().equals(customerId)) {
                ra.addFlashAttribute("error", "Bạn không có quyền hủy sản phẩm trong đơn này");
                return "redirect:/customer/orders/" + billId;
            }

            int status = bill.getStatus();

            if (status == com.example.skysport1.enums.OrderStatus.PENDING.getValue()) {
                billService.cancelBillDetail(billId, billDetailId, note != null ? note : "Khách hàng hủy sản phẩm");
                ra.addFlashAttribute("success", "Đã hủy sản phẩm trong đơn " + billId);
            } else if (status == com.example.skysport1.enums.OrderStatus.CONFIRMED.getValue()) {
                billService.requestCancelBillDetail(
                        billId,
                        billDetailId,
                        note != null ? note : "Khách hàng hủy sản phẩm"
                );
                ra.addFlashAttribute("success", "Đã gửi yêu cầu hủy sản phẩm trong đơn " + billId + ". Vui lòng chờ shop xác nhận");
            } else {
                ra.addFlashAttribute("error", "Sản phẩm không thể hủy ở trạng thái hiện tại");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/customer/orders/" + billId;
    }

    private String getCurrentCustomerId(Authentication auth) {
        String username = auth.getName();
        Account account = accountService.findByUsername(username);
        return customerService.findByAccountId(account.getId()).getId();
    }
}