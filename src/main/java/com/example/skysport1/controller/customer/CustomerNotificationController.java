package com.example.skysport1.controller.customer;

import com.example.skysport1.entity.Account;
import com.example.skysport1.entity.Notification;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.CustomerService;
import com.example.skysport1.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer/notifications")
@RequiredArgsConstructor
@Slf4j
public class CustomerNotificationController {

    private final NotificationService notificationService;
    private final CustomerService customerService;
    private final AccountService accountService;

    /**
     * Danh sách thông báo
     */
    @GetMapping
    public String list(Authentication auth, Model model) {
        String customerId = getCurrentCustomerId(auth);
        List<Notification> notifications = notificationService.findByCustomer(customerId);
        long unreadCount = notificationService.countUnread(customerId);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("title", "Thông báo");
        model.addAttribute("pageContent", "customer/notification/index");
        return "layouts/customer/layout";
    }

    /**
     * Đánh dấu đã đọc
     */
    @PostMapping("/read/{id}")
    @ResponseBody
    public String markRead(@PathVariable Integer id) {
        try {
            notificationService.markAsRead(id);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * Đánh dấu tất cả đã đọc
     */
    @PostMapping("/read-all")
    public String markAllRead(Authentication auth, RedirectAttributes ra) {
        try {
            String customerId = getCurrentCustomerId(auth);
            notificationService.markAllAsRead(customerId);
            ra.addFlashAttribute("success", "Đã đánh dấu tất cả thông báo là đã đọc");
        } catch (Exception e) {
            log.error("Error marking all as read: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/notifications";
    }

    private String getCurrentCustomerId(Authentication auth) {
        String username = auth.getName();
        Account account = accountService.findByUsername(username);
        return customerService.findByAccountId(account.getId()).getId();
    }
}