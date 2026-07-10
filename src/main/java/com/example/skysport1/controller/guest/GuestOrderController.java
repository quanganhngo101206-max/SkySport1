package com.example.skysport1.controller.guest;

import com.example.skysport1.dto.request.TrackGuestByContactRequest;
import com.example.skysport1.dto.request.TrackOrderRequest;
import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.OrderStatusHistory;
import com.example.skysport1.repository.OrderStatusHistoryRepository;
import com.example.skysport1.service.BillService;
import com.example.skysport1.util.SimpleRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Guest order lookup/cancel bị giới hạn tốc độ (rate limit) theo IP để giảm
 * thiểu nguy cơ brute-force mã đơn (bill id sinh tuần tự, dễ đoán) kết hợp
 * với việc chỉ xác thực bằng email/SĐT trần (IDOR). Đây là biện pháp giảm
 * thiểu tạm thời; giải pháp triệt để hơn là dùng token ngẫu nhiên/ký cho
 * từng đơn guest, nhưng cần thêm cột DB (hiện schema do bên ngoài quản lý,
 * ddl-auto=none, không có migration đi kèm trong repo này).
 */
@Controller
@RequestMapping("/guest")
@RequiredArgsConstructor
@Slf4j
public class GuestOrderController {

    private final BillService billService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final SimpleRateLimiter rateLimiter;

    private static final int LOOKUP_MAX_ATTEMPTS = 20;
    private static final int CANCEL_MAX_ATTEMPTS = 8;
    private static final long WINDOW_SECONDS = 300; // 5 phút

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Trang tra cứu đơn hàng
     */
    @GetMapping("/track")
    public String trackPage(Model model) {
        model.addAttribute("trackRequest", new TrackOrderRequest());
        model.addAttribute("trackByContactRequest", new TrackGuestByContactRequest());
        model.addAttribute("title", "Tra cứu đơn hàng");
        return "guest/track";
    }

    /**
     * Xử lý tra cứu đơn hàng
     */
    @PostMapping("/track")
    public String trackOrder(@Valid @ModelAttribute("trackRequest") TrackOrderRequest request,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes ra,
                             HttpServletRequest httpRequest
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("trackByContactRequest", new TrackGuestByContactRequest());
            model.addAttribute("title", "Tra cứu đơn hàng");
            return "guest/track";
        }

        if (!rateLimiter.allow("track:" + clientIp(httpRequest), LOOKUP_MAX_ATTEMPTS, WINDOW_SECONDS)) {
            model.addAttribute("error", "Bạn đã thử quá nhiều lần, vui lòng thử lại sau ít phút");
            model.addAttribute("trackByContactRequest", new TrackGuestByContactRequest());
            model.addAttribute("title", "Tra cứu đơn hàng");
            return "guest/track";
        }

        try {
            Bill bill = billService.findById(request.getOrderId());

            // Kiểm tra email khớp với guest_email hoặc email của customer
            boolean emailMatches = false;

            if (bill.getGuestEmail() != null && bill.getGuestEmail().equalsIgnoreCase(request.getEmail())) {
                emailMatches = true;
            }

            if (bill.getCustomer() != null && bill.getCustomer().getEmail() != null
                    && bill.getCustomer().getEmail().equalsIgnoreCase(request.getEmail())) {
                emailMatches = true;
            }

            if (!emailMatches) {
                model.addAttribute("error", "Không tìm thấy đơn hàng với mã và email này");
                model.addAttribute("trackByContactRequest", new TrackGuestByContactRequest());
                model.addAttribute("title", "Tra cứu đơn hàng");
                return "guest/track";
            }

            // Lấy timeline theo thứ tự insert (IDENTITY id) để ổn định
            List<OrderStatusHistory> timeline =
                    orderStatusHistoryRepository.findByBillIdOrderByIdAsc(request.getOrderId());

            model.addAttribute("bill", bill);
            model.addAttribute("timeline", timeline);
            model.addAttribute("title", "Chi tiết đơn hàng #" + request.getOrderId());
            return "guest/order-detail";

        } catch (Exception e) {
            log.error("Error tracking order: {}", e.getMessage());
            model.addAttribute("error", "Không tìm thấy đơn hàng với mã và email này");
            model.addAttribute("trackByContactRequest", new TrackGuestByContactRequest());
            model.addAttribute("title", "Tra cứu đơn hàng");
            return "guest/track";
        }
    }

    /**
     * Xử lý tra cứu đơn hàng theo SĐT / Email (có thể trả về nhiều đơn)
     */
    @PostMapping("/track-by-contact")
    public String trackByContact(@Valid @ModelAttribute("trackByContactRequest") TrackGuestByContactRequest request,
                                 BindingResult bindingResult,
                                 Model model,
                                 HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("trackRequest", new TrackOrderRequest());
            model.addAttribute("title", "Tra cứu đơn hàng");
            return "guest/track";
        }

        if (!rateLimiter.allow("track-contact:" + clientIp(httpRequest), LOOKUP_MAX_ATTEMPTS, WINDOW_SECONDS)) {
            model.addAttribute("error", "Bạn đã thử quá nhiều lần, vui lòng thử lại sau ít phút");
            model.addAttribute("trackRequest", new TrackOrderRequest());
            model.addAttribute("title", "Tra cứu đơn hàng");
            return "guest/track";
        }

        String contact = request.getContact().trim();
        List<Bill> bills = billService.findGuestBillsByContact(contact);

        if (bills.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy đơn hàng nào với thông tin này");
            model.addAttribute("trackRequest", new TrackOrderRequest());
            model.addAttribute("title", "Tra cứu đơn hàng");
            return "guest/track";
        }

        model.addAttribute("bills", bills);
        model.addAttribute("contact", contact);
        model.addAttribute("title", "Kết quả tra cứu đơn hàng");
        return "guest/track-result";
    }

    /**
     * Xem chi tiết đơn hàng.
     * Param "email" thực chất nhận cả email lẫn số điện thoại (contact) —
     * dùng chung cho link trong mail (email) và luồng tra cứu theo SĐT/Email.
     */
    @GetMapping("/order/{id}")
    public String orderDetail(@PathVariable String id,
                              @RequestParam String email,
                              Model model,
                              RedirectAttributes ra,
                              HttpServletRequest httpRequest) {
        if (!rateLimiter.allow("order-detail:" + clientIp(httpRequest), LOOKUP_MAX_ATTEMPTS, WINDOW_SECONDS)) {
            ra.addFlashAttribute("error", "Bạn đã thử quá nhiều lần, vui lòng thử lại sau ít phút");
            return "redirect:/guest/track";
        }
        try {
            Bill bill = billService.findById(id);

            // Kiểm tra email hoặc số điện thoại người nhận
            boolean emailMatches = false;

            if (bill.getGuestEmail() != null && bill.getGuestEmail().equalsIgnoreCase(email)) {
                emailMatches = true;
            }

            if (bill.getCustomer() != null && bill.getCustomer().getEmail() != null
                    && bill.getCustomer().getEmail().equalsIgnoreCase(email)) {
                emailMatches = true;
            }

            if (bill.getReceiverPhone() != null && bill.getReceiverPhone().equals(email)) {
                emailMatches = true;
            }

            if (!emailMatches) {
                ra.addFlashAttribute("error", "Không có quyền xem đơn hàng này");
                return "redirect:/guest/track";
            }

            // Lấy timeline theo thứ tự insert (IDENTITY id) để ổn định
            List<OrderStatusHistory> timeline =
                    orderStatusHistoryRepository.findByBillIdOrderByIdAsc(id);

            model.addAttribute("bill", bill);
            model.addAttribute("timeline", timeline);
            model.addAttribute("title", "Chi tiết đơn hàng #" + id);
            return "guest/order-detail";

        } catch (Exception e) {
            log.error("Error loading guest order detail: {}", e.getMessage());
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/guest/track";
        }
    }

    /**
     * Hủy đơn (guest)
     * - PENDING(1): hủy ngay
     * - CONFIRMED(2): gửi request hủy (chuyển CANCEL_REQUESTED)
     *
     * POST: /guest/order/{id}/cancel?email=...
     */
    @PostMapping("/order/{id}/cancel")
    public String cancel(@PathVariable String id,
                         @RequestParam String email,
                         @RequestParam(required = false) String note,
                         RedirectAttributes ra,
                         HttpServletRequest httpRequest) {
        if (!rateLimiter.allow("cancel:" + clientIp(httpRequest), CANCEL_MAX_ATTEMPTS, WINDOW_SECONDS)) {
            ra.addFlashAttribute("error", "Bạn đã thử quá nhiều lần, vui lòng thử lại sau ít phút");
            return "redirect:/guest/track";
        }
        try {
            Bill bill = billService.findById(id);

            // Kiểm tra email hoặc số điện thoại quyền hủy
            boolean emailMatches = false;

            if (bill.getGuestEmail() != null && bill.getGuestEmail().equalsIgnoreCase(email)) {
                emailMatches = true;
            }

            if (bill.getCustomer() != null && bill.getCustomer().getEmail() != null
                    && bill.getCustomer().getEmail().equalsIgnoreCase(email)) {
                emailMatches = true;
            }

            if (bill.getReceiverPhone() != null && bill.getReceiverPhone().equals(email)) {
                emailMatches = true;
            }

            if (!emailMatches) {
                ra.addFlashAttribute("error", "Không có quyền hủy đơn hàng này");
                return "redirect:/guest/order/" + id + "?email=" + email;
            }

            int status = bill.getStatus();

            if (status == com.example.skysport1.enums.OrderStatus.PENDING.getValue()) {
                // PENDING(1) -> hủy ngay
                billService.cancel(id, null, note);
                ra.addFlashAttribute("success", "Đã hủy đơn hàng " + id);
            } else if (status == com.example.skysport1.enums.OrderStatus.CONFIRMED.getValue()) {
                // CONFIRMED(2) -> customer request hủy
                billService.requestCancel(id, note != null ? note : "Khách hàng hủy đơn");
                ra.addFlashAttribute("success", "Yêu cầu hủy đơn hàng đã được gửi: " + id + ". Vui lòng chờ shop xác nhận");
            } else {
                ra.addFlashAttribute("error", "Đơn hàng không thể hủy ở trạng thái hiện tại");
            }

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/guest/order/" + id + "?email=" + email;
    }

    /**
     * Hủy từng sản phẩm trong đơn (guest)
     * POST: /guest/order/{billId}/details/{billDetailId}/cancel?email=...
     */
    @PostMapping("/order/{billId}/details/{billDetailId}/cancel")
    public String cancelBillDetail(@PathVariable String billId,
                                   @PathVariable Integer billDetailId,
                                   @RequestParam String email,
                                   @RequestParam(required = false) String note,
                                   RedirectAttributes ra,
                                   HttpServletRequest httpRequest) {
        if (!rateLimiter.allow("cancel-detail:" + clientIp(httpRequest), CANCEL_MAX_ATTEMPTS, WINDOW_SECONDS)) {
            ra.addFlashAttribute("error", "Bạn đã thử quá nhiều lần, vui lòng thử lại sau ít phút");
            return "redirect:/guest/track";
        }
        try {
            Bill bill = billService.findById(billId);

            // Kiểm tra email hoặc số điện thoại quyền hủy
            boolean emailMatches = false;

            if (bill.getGuestEmail() != null && bill.getGuestEmail().equalsIgnoreCase(email)) {
                emailMatches = true;
            }

            if (bill.getCustomer() != null && bill.getCustomer().getEmail() != null
                    && bill.getCustomer().getEmail().equalsIgnoreCase(email)) {
                emailMatches = true;
            }

            if (bill.getReceiverPhone() != null && bill.getReceiverPhone().equals(email)) {
                emailMatches = true;
            }

            if (!emailMatches) {
                ra.addFlashAttribute("error", "Không có quyền hủy sản phẩm trong đơn này");
                return "redirect:/guest/order/" + billId + "?email=" + email;
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

        return "redirect:/guest/order/" + billId + "?email=" + email;
    }
}