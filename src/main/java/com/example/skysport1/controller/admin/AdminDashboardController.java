package com.example.skysport1.controller.admin;

import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.BillDetail;
import com.example.skysport1.entity.Customer;
import com.example.skysport1.entity.DiscountCode;
import com.example.skysport1.entity.ProductDetail;
import com.example.skysport1.repository.BillRepository;
import com.example.skysport1.repository.CustomerRepository;
import com.example.skysport1.repository.DiscountCodeRepository;
import com.example.skysport1.repository.ProductDetailRepository;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.CustomerService;
import com.example.skysport1.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ProductService productService;
    private final BillService billService;
    private final CustomerService customerService;

    private final BillRepository billRepository;

    private final DiscountCodeRepository discountCodeRepository;
    private final CustomerRepository customerRepository;
    private final ProductDetailRepository productDetailRepository;

    @GetMapping
    public String dashboard(Model model) {
        // ===== STATS ROW =====
        long totalProducts = productService.findAllActive().size();
        long totalCustomers = customerService.findAll().size();

        List<Bill> allBills = billService.findAll();

        long totalOrders = allBills.size();
        BigDecimal totalRevenue = allBills.stream()
                .filter(b -> b.getStatus() != null && b.getStatus() == 7)
                .map(Bill::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5 recent bills
        List<Bill> recentBills = billRepository.findTop5WithCustomer(PageRequest.of(0, 5));

        // ===== Order status counts (theo template) =====
        long pendingOrders = billRepository.countByStatus(1);      // Chờ xác nhận
        long deliveringOrders = billRepository.countByStatus(2);   // Đang giao
        long completedOrders = billRepository.countByStatus(7);     // Hoàn thành
        long cancelledOrders = billRepository.countByStatus(5);     // Đã hủy

        // ===== Recent customers + new customers this month =====
        List<Customer> recentCustomers = customerRepository.findTopRecent(10);
        long newCustomersThisMonth = customerRepository.countByCreateDateBetween(
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusMonths(1).atStartOfDay()
        );

        // ===== Active vouchers =====
        LocalDateTime now = LocalDateTime.now();
        List<DiscountCode> activeVouchers = discountCodeRepository.findAll().stream()
                .filter(dc -> dc.getStatus() != null && dc.getStatus() == 1)
                .filter(dc -> dc.getDeleteFlag() != null && !dc.getDeleteFlag())
                .filter(dc -> dc.getStartDate() == null || !now.isBefore(dc.getStartDate()))
                .filter(dc -> dc.getEndDate() == null || !now.isAfter(dc.getEndDate()))
                .toList();

        // ===== Stock based on ProductDetail.quantity =====
        List<ProductDetail> allDetails = productDetailRepository.findAll();
        long inStockProducts = allDetails.stream()
                .filter(pd -> pd.getDeleteFlag() == null || !pd.getDeleteFlag())
                .filter(pd -> pd.getStatus() == null || pd.getStatus() == 1)
                .filter(pd -> pd.getQuantity() != null && pd.getQuantity() > 0)
                .count();

        long lowStockProducts = allDetails.stream()
                .filter(pd -> pd.getDeleteFlag() == null || !pd.getDeleteFlag())
                .filter(pd -> pd.getStatus() == null || pd.getStatus() == 1)
                .filter(pd -> pd.getQuantity() != null && pd.getQuantity() > 0 && pd.getQuantity() < 5)
                .count();

        // ===== Top products (soldQuantity + revenue) =====
        // Template expects: product.productName, product.soldQuantity, product.revenue
        // Ta tính từ BillDetail snapshot:
        // FIX: billDetails là lazy -> dùng query fetch join từ repo
        List<Bill> completedBills = billRepository.findByStatusWithDetails(7);

        Map<String, Long> soldQtyByName = new HashMap<>();
        Map<String, BigDecimal> revenueByName = new HashMap<>();

        for (Bill b : completedBills) {
            if (b.getBillDetails() == null) continue;
            for (BillDetail bd : b.getBillDetails()) {
                String name = bd.getProductNameSnapshot();
                if (name == null) continue;

                long qty = bd.getQuantity() == null ? 0 : bd.getQuantity().longValue();
                BigDecimal amount = bd.getTotalAmount() == null ? BigDecimal.ZERO : bd.getTotalAmount();

                soldQtyByName.merge(name, qty, Long::sum);
                revenueByName.merge(name, amount, BigDecimal::add);
            }
        }

        List<Map<String, Object>> topProducts = soldQtyByName.entrySet().stream()
                .map(e -> {
                    String productName = e.getKey();
                    Long soldQuantity = e.getValue();
                    BigDecimal revenue = revenueByName.getOrDefault(productName, BigDecimal.ZERO);

                    Map<String, Object> row = new HashMap<>();
                    row.put("productName", productName);
                    row.put("soldQuantity", soldQuantity);
                    row.put("revenue", revenue);
                    return row;
                })
                .sorted((a, b) -> ((Long) b.get("soldQuantity")).compareTo((Long) a.get("soldQuantity")))
                .limit(5)
                .toList();

        // ===== Doanh thu thực thu (trừ giá vốn) =====
        // Lưu ý: BillDetail không snapshot costPrice lúc bán, nên lấy costPrice
        // hiện tại của ProductDetail — chấp nhận được cho mục đích thống kê
        // tổng quan (không dùng để đối soát kế toán chính xác tuyệt đối).
        BigDecimal totalCost = completedBills.stream()
                .filter(b -> b.getBillDetails() != null)
                .flatMap(b -> b.getBillDetails().stream())
                .filter(bd -> bd.getProductDetail() != null && bd.getProductDetail().getCostPrice() != null
                        && bd.getQuantity() != null)
                .map(bd -> bd.getProductDetail().getCostPrice().multiply(BigDecimal.valueOf(bd.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netRevenue = totalRevenue.subtract(totalCost);

        // ===== Top khách hàng theo tổng chi tiêu (dựa trên đơn Hoàn thành) =====
        Map<Customer, BigDecimal> spendByCustomer = completedBills.stream()
                .filter(b -> b.getCustomer() != null && b.getTotalAmount() != null)
                .collect(Collectors.groupingBy(
                        Bill::getCustomer,
                        Collectors.reducing(BigDecimal.ZERO, Bill::getTotalAmount, BigDecimal::add)
                ));

        List<Map<String, Object>> topCustomers = spendByCustomer.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("fullName", e.getKey().getFullName());
                    row.put("email", e.getKey().getEmail());
                    row.put("totalSpent", e.getValue());
                    return row;
                })
                .toList();

        // ===== Revenue chart 6 months =====
        YearMonth current = YearMonth.from(LocalDate.now());
        List<String> revenueLabels = new ArrayList<>();
        List<BigDecimal> revenueData = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);

            revenueLabels.add(ym.toString().replace("-", "/")); // format yyyy/MM

            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

            BigDecimal sum = completedBills.stream()
                    .filter(b -> b.getCreateDate() != null)
                    .filter(b -> !b.getCreateDate().isBefore(start) && b.getCreateDate().isBefore(end))
                    .map(Bill::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenueData.add(sum);
        }

        // ===== set model for Thymeleaf template =====
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentBills", recentBills);

        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("deliveringOrders", deliveringOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);

        model.addAttribute("recentCustomers", recentCustomers);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("netRevenue", netRevenue);
        model.addAttribute("topCustomers", topCustomers);

        model.addAttribute("activeVouchers", activeVouchers.size());
        model.addAttribute("inStockProducts", inStockProducts);
        model.addAttribute("lowStockProducts", lowStockProducts);

        model.addAttribute("newCustomersThisMonth", newCustomersThisMonth);

        model.addAttribute("revenueLabels", revenueLabels);
        model.addAttribute("revenueData", revenueData);

        model.addAttribute("title", "Dashboard");
        model.addAttribute("pageContent", "admin/dashboard");

        return "layouts/adminlte/layout";
    }
}