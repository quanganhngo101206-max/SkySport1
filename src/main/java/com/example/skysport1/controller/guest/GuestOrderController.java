package com.example.skysport1.controller.guest;

import com.example.skysport1.dto.request.TrackOrderRequest;
import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.OrderStatusHistory;
import com.example.skysport1.repository.OrderStatusHistoryRepository;
import com.example.skysport1.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/guest")
@RequiredArgsConstructor
@Slf4j
public class GuestOrderController {

    private final BillService billService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    /**
     * Trang tra cứu đơn hàng
     */
    @GetMapping("/track")
    public String trackPage(Model model) {
        model.addAttribute("trackRequest", new TrackOrderRequest());
        model.addAttribute("title", "Tra cứu đơn hàng");
        return "guest/track";
    }

    /**
     * Xử lý tra cứu đơn hàng
     */
    @PostMapping("/track")
    public String trackOrder(@Valid @ModelAttribute ("trackRequest") TrackOrderRequest request,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes ra
                             ) {
        if (bindingResult.hasErrors()) {
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
                model.addAttribute("title", "Tra cứu đơn hàng");
                return "guest/track";
            }

            // Lấy timeline
            List<OrderStatusHistory> timeline =
                    orderStatusHistoryRepository.findByBillIdOrderByCreateDateAsc(request.getOrderId());

            model.addAttribute("bill", bill);
            model.addAttribute("timeline", timeline);
            model.addAttribute("title", "Chi tiết đơn hàng #" + request.getOrderId());
            return "guest/order-detail";

        } catch (Exception e) {
            log.error("Error tracking order: {}", e.getMessage());
            model.addAttribute("error", "Không tìm thấy đơn hàng với mã và email này");
            model.addAttribute("title", "Tra cứu đơn hàng");
            return "guest/track";
        }
    }

    /**
     * Xem chi tiết đơn hàng (từ link trong email)
     */
    @GetMapping("/order/{id}")
    public String orderDetail(@PathVariable String id,
                              @RequestParam String email,
                              Model model,
                              RedirectAttributes ra) {
        try {
            Bill bill = billService.findById(id);

            // Kiểm tra email
            boolean emailMatches = false;

            if (bill.getGuestEmail() != null && bill.getGuestEmail().equalsIgnoreCase(email)) {
                emailMatches = true;
            }

            if (bill.getCustomer() != null && bill.getCustomer().getEmail() != null
                    && bill.getCustomer().getEmail().equalsIgnoreCase(email)) {
                emailMatches = true;
            }

            if (!emailMatches) {
                ra.addFlashAttribute("error", "Không có quyền xem đơn hàng này");
                return "redirect:/guest/track";
            }

            List<OrderStatusHistory> timeline =
                    orderStatusHistoryRepository.findByBillIdOrderByCreateDateAsc(id);

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
}