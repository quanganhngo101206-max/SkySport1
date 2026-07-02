package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.Staff;
import com.example.skysport1.service.CustomerService;
import com.example.skysport1.service.NotificationService;
import com.example.skysport1.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final CustomerService customerService;
    private final StaffService staffService;

    // ── Trang gửi thông báo ──────────────────────────────────────────────

    @GetMapping
    public String index(Model model, Authentication auth) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            model.addAttribute("staffNotifications",
                    notificationService.findUnreadByStaff(staff.getId()));
        } catch (Exception e) {
            log.warn("Could not load staff notifications: {}", e.getMessage());
        }
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("title", "Thông báo");
        model.addAttribute("pageContent", "admin/notification/index");
        return "layouts/adminlte/layout";
    }

    // ── Gửi thông báo đến 1 khách hàng ──────────────────────────────────

    @PostMapping("/send-customer")
    public String sendToCustomer(@RequestParam String customerId,
                                 @RequestParam String title,
                                 @RequestParam String content,
                                 @RequestParam(defaultValue = "GENERAL") String type,
                                 RedirectAttributes ra) {
        try {
            notificationService.sendToCustomer(customerId, title, content, type, null);
            ra.addFlashAttribute("success", "Đã gửi thông báo đến khách hàng");
            log.info("Notification sent to customer {}", customerId);
        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    // ── Gửi thông báo đến tất cả khách hàng ─────────────────────────────

    @PostMapping("/send-all")
    public String sendToAll(@RequestParam String title,
                            @RequestParam String content,
                            @RequestParam(defaultValue = "GENERAL") String type,
                            RedirectAttributes ra) {
        try {
            int count = 0;
            for (var customer : customerService.findAllActive()) {
                try {
                    notificationService.sendToCustomer(customer.getId(), title, content, type, null);
                    count++;
                } catch (Exception ex) {
                    log.warn("Không thể gửi cho customer {}: {}", customer.getId(), ex.getMessage());
                }
            }
            ra.addFlashAttribute("success", "Đã gửi thông báo đến " + count + " khách hàng");
            log.info("Broadcast notification sent to {} customers", count);
        } catch (Exception e) {
            log.error("Error broadcasting notification: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    // ── Đánh dấu đã đọc ─────────────────────────────────────────────────

    @PostMapping("/{id}/read")
    @ResponseBody
    public String markRead(@PathVariable Integer id) {
        try {
            notificationService.markAsRead(id);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }
}