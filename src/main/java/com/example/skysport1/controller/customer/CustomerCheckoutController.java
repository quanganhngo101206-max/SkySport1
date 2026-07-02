package com.example.skysport1.controller.customer;

import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.BillDetail;
import com.example.skysport1.entity.CartDetail;
import com.example.skysport1.entity.ProductDetail;
import com.example.skysport1.repository.ProductDetailRepository;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.CartService;
import com.example.skysport1.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer/checkout")
@RequiredArgsConstructor
@Slf4j
public class CustomerCheckoutController {

    private static final String SESSION_GUEST_CART = "GUEST_CART";

    private final ProductDetailRepository productDetailRepository;
    private final BillService billService;
    private final CartService cartService;
    private final AccountService accountService;
    private final CustomerService customerService;

    // ===== HIỂN THỊ FORM CHECKOUT =====

    @GetMapping
    public String checkoutPage(Model model, HttpSession session) {
        String customerId = getCurrentCustomerId().orElse(null);

        // Lấy giỏ hàng
        List<CartDetail> cartItems = new ArrayList<>();
        if (customerId != null) {
            cartItems = cartService.getCartDetails(customerId);
        } else {
            Map<Integer, Integer> guestCart = getGuestCart(session);
            for (Map.Entry<Integer, Integer> e : guestCart.entrySet()) {
                try {
                    ProductDetail pd = productDetailRepository
                            .findByIdWithProductSizeColor(e.getKey()).orElseThrow();
                    CartDetail cd = CartDetail.builder()
                            .productDetail(pd)
                            .quantity(e.getValue())
                            .build();
                    cartItems.add(cd);
                } catch (Exception ex) {
                    // sản phẩm không còn tồn tại -> bỏ qua
                }
            }
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("title", "Thanh toán");
        // Tính subtotal
        BigDecimal subtotal = cartItems.stream()
                .filter(i -> i.getProductDetail() != null && i.getProductDetail().getPrice() != null)
                .map(i -> i.getProductDetail().getPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = subtotal.compareTo(BigDecimal.valueOf(500_000)) < 0
                ? BigDecimal.valueOf(30_000) : BigDecimal.ZERO;

        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountAmount", BigDecimal.ZERO);
        model.addAttribute("totalAmount", subtotal.add(shippingFee));
        return "customer/checkout/checkout";
    }

    @GetMapping("/guest")
    public String guestCheckoutPage(Model model) {
        model.addAttribute("title", "Thanh toán - Khách");
        return "customer/checkout/checkout";
    }

    // ===== GUEST CHECKOUT - KHÔNG CẦN ĐĂNG NHẬP =====
    @PostMapping("/guest")
    public String createGuestBill(
            @RequestParam("guestEmail") String guestEmail,
            @RequestParam("shippingAddress") String shippingAddress,
            @RequestParam("receiverName") String receiverName,
            @RequestParam("receiverPhone") String receiverPhone,
            @RequestParam("paymentId") String paymentId,
            @RequestParam(value = "discountCode", required = false) String discountCode,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            Map<Integer, Integer> guestCart = getGuestCart(session);

            List<BillDetail> items = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : guestCart.entrySet()) {
                Integer productDetailId = e.getKey();
                Integer qty = e.getValue();

                if (productDetailId == null || productDetailId <= 0) continue;
                if (qty == null || qty <= 0) continue;

                items.add(BillDetail.builder()
                        .productDetail(com.example.skysport1.entity.ProductDetail.builder().id(productDetailId).build())
                        .quantity(qty)
                        .build());
            }

            if (items.isEmpty()) {
                ra.addFlashAttribute("error", "Giỏ hàng trống!");
                return "redirect:/customer/cart";
            }

            Bill created = billService.createGuestBill(
                    guestEmail,
                    shippingAddress,
                    receiverName,
                    receiverPhone,
                    paymentId,
                    discountCode,
                    items
            );

            session.removeAttribute(SESSION_GUEST_CART);
            ra.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn: " + created.getId());
            return "redirect:/customer/bill/detail/" + created.getId();

        } catch (Exception e) {
            log.error("Guest checkout error: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/checkout/guest";
        }
    }

    // ===== LOGGED-IN CHECKOUT =====
    @PostMapping
    public String createOnlineBill(
            @RequestParam("shippingAddress") String shippingAddress,
            @RequestParam("receiverName") String receiverName,
            @RequestParam("receiverPhone") String receiverPhone,
            @RequestParam("paymentId") String paymentId,
            @RequestParam(value = "discountCode", required = false) String discountCode,
            RedirectAttributes ra
    ) {
        try {
            String customerId = getCurrentCustomerId()
                    .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để checkout"));

            List<CartDetail> cartDetails = cartService.getCartDetails(customerId);
            if (cartDetails == null || cartDetails.isEmpty()) {
                ra.addFlashAttribute("error", "Giỏ hàng trống!");
                return "redirect:/customer/cart";
            }

            List<BillDetail> items = new ArrayList<>();
            for (CartDetail cd : cartDetails) {
                items.add(BillDetail.builder()
                        .productDetail(cd.getProductDetail())
                        .quantity(cd.getQuantity())
                        .build());
            }

            Bill created = billService.createOnlineBill(
                    customerId,
                    shippingAddress,
                    receiverName,
                    receiverPhone,
                    paymentId,
                    discountCode,
                    items
            );

            cartService.clearCart(customerId);
            ra.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn: " + created.getId());
            return "redirect:/customer/bill/detail/" + created.getId();

        } catch (Exception e) {
            log.error("Checkout error: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/checkout";
        }
    }

    // ===== HELPER =====
    private Map<Integer, Integer> getGuestCart(HttpSession session) {
        Object raw = session.getAttribute(SESSION_GUEST_CART);
        if (raw instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> result = (Map<Integer, Integer>) map;
            return result;
        }
        return java.util.Collections.emptyMap();
    }

    private java.util.Optional<String> getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return java.util.Optional.empty();
        }

        String username = auth.getName();
        if (username == null || username.isBlank()) {
            return java.util.Optional.empty();
        }

        try {
            var account = accountService.findByUsername(username);
            var customer = customerService.findByAccountId(account.getId());
            return java.util.Optional.ofNullable(customer.getId());
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }
}