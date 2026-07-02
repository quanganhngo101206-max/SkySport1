package com.example.skysport1.controller.staff;

import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.Staff;
import com.example.skysport1.repository.BillRepository;
import com.example.skysport1.repository.CustomerRepository;
import com.example.skysport1.repository.ProductDetailRepository;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
@Slf4j
public class StaffDashboardController {

    private final BillService billService;
    private final StaffService staffService;
    private final BillRepository billRepository;

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        try {
            Staff staff = staffService.findByAccountUsername(auth.getName());
            model.addAttribute("staff", staff);
        } catch (Exception e) {
            log.warn("Could not load staff info: {}", e.getMessage());
        }

        // ===== STATS =====
        List<Bill> allBills = billService.findAll();

        long totalOrders = allBills.size();
        long pendingOrders = billRepository.countByStatus(1);
        long deliveringOrders = billRepository.countByStatus(3);
        long completedOrders = billRepository.countByStatus(7);

        BigDecimal totalRevenue = allBills.stream()
                .filter(b -> b.getStatus() != null && b.getStatus() == 7)
                .map(Bill::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ===== 10 đơn gần nhất =====
        List<Bill> recentBills = billRepository.findTop5WithCustomer(
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("deliveringOrders", deliveringOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentBills", recentBills);

        model.addAttribute("title", "Staff Dashboard");
        model.addAttribute("pageContent", "staff/dashboard");
        return "layouts/staff/layout";
    }
}