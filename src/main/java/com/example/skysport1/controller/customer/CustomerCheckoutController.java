package com.example.skysport1.controller.customer;

import com.example.skysport1.entity.AddressShipping;
import com.example.skysport1.entity.Bill;
import com.example.skysport1.entity.BillDetail;
import com.example.skysport1.entity.CartDetail;
import com.example.skysport1.entity.DiscountCode;
import com.example.skysport1.entity.ProductDetail;
import com.example.skysport1.repository.AddressShippingRepository;
import com.example.skysport1.repository.CustomerRepository;
import com.example.skysport1.repository.ProductDetailRepository;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.BillService;
import com.example.skysport1.service.CartService;
import com.example.skysport1.service.CustomerService;
import com.example.skysport1.service.DiscountCodeService;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/customer/checkout")
@RequiredArgsConstructor
@Slf4j
public class CustomerCheckoutController {

    private static final String SESSION_GUEST_CART = "GUEST_CART";
    // Phải trùng key với CustomerCartController — nơi "Mua ngay" ghi 1 sản
    // phẩm/biến thể vào session, tách biệt khỏi giỏ hàng thật.
    private static final String SESSION_BUY_NOW_ITEM = "BUY_NOW_ITEM";

    private final ProductDetailRepository productDetailRepository;
    private final BillService billService;
    private final CartService cartService;
    private final AccountService accountService;
    private final CustomerService customerService;
    private final DiscountCodeService discountCodeService;
    private final AddressShippingRepository addressShippingRepository;
    private final CustomerRepository customerRepository;

    // ===== HIỂN THỊ FORM CHECKOUT =====
    @GetMapping
    public String checkoutPage(@RequestParam(required = false) Boolean buyNow,
                               Model model, HttpSession session) {
        String customerId = getCurrentCustomerId().orElse(null);
        boolean buyNowMode = isBuyNowMode(buyNow, session);

        log.info("🔍 checkoutPage - customerId: {}, buyNow: {}", customerId, buyNowMode);

        // Lấy giỏ hàng: ưu tiên item "mua ngay" nếu đang ở chế độ này, ngược
        // lại đọc giỏ hàng thật như bình thường (không đụng vào nhau).
        List<CartDetail> cartItems;
        if (buyNowMode) {
            cartItems = buildCartDetailsFromMap(getBuyNowItem(session));
            log.info("🛒 Buy-now mode: {} sản phẩm", cartItems.size());
        } else if (customerId != null) {
            cartItems = cartService.getCartDetails(customerId);
            log.info("🟢 Customer {} có {} sản phẩm trong giỏ", customerId, cartItems.size());
        } else {
            Map<Integer, Integer> guestCart = getGuestCart(session);
            log.info("🟡 Guest có {} sản phẩm trong session cart", guestCart.size());
            cartItems = buildCartDetailsFromMap(guestCart);
        }

        log.info("📦 Total cart items after processing: {}", cartItems.size());

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartItemsCount", cartItems.size());
        model.addAttribute("buyNow", buyNowMode);
        model.addAttribute("title", "Thanh toán");

        // Tính subtotal
        BigDecimal subtotal = cartItems.stream()
                .filter(i -> i.getProductDetail() != null && i.getProductDetail().getPrice() != null)
                .map(i -> i.getProductDetail().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = subtotal.compareTo(BigDecimal.valueOf(500_000)) < 0
                ? BigDecimal.valueOf(30_000)
                : BigDecimal.ZERO;

        // Luôn lấy danh sách discount codes để hiển thị
        List<DiscountCode> discountCodes = discountCodeService.findAll();
        model.addAttribute("discountCodes", discountCodes);

        // Chỉ tính best discount cho customer đã login
        BigDecimal bestDiscountAmount = BigDecimal.ZERO;
        String bestDiscountCode = null;
        if (customerId != null) {
            List<com.example.skysport1.service.DiscountCodeService.LineItem> lineItems =
                    com.example.skysport1.service.DiscountCodeService.fromCartDetails(cartItems);
            for (DiscountCode dc : discountCodes) {
                try {
                    BigDecimal current = discountCodeService.validate(dc.getCode(), customerId, lineItems);
                    if (current != null && current.compareTo(bestDiscountAmount) > 0) {
                        bestDiscountAmount = current;
                        bestDiscountCode = dc.getCode();
                    }
                } catch (Exception ignored) {
                    // voucher không hợp lệ -> bỏ qua
                }
            }
        }

        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(bestDiscountAmount);

        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountAmount", bestDiscountAmount);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("bestDiscountCode", bestDiscountCode);

        // Auto-fill thông tin nhận hàng cho khách đăng nhập
        if (customerId != null) {
            fillCustomerInfo(model, customerId);
        }

        return "customer/checkout/checkout";
    }

    // ===== CHECK TRÙNG EMAIL/PHONE (GỢI Ý ĐĂNG NHẬP, KHÔNG CHẶN) =====
    /**
     * Gọi bằng AJAX khi guest nhập email/phone ở form checkout khách vãng lai.
     * Chỉ dùng để GỢI Ý "email/SĐT này đã có tài khoản, đăng nhập để nhận ưu
     * đãi" — KHÔNG chặn guest tiếp tục đặt hàng nếu họ không muốn đăng nhập
     * (đúng yêu cầu nghiệp vụ, xem mục 2.7/14 trong note đánh giá cải tiến).
     */
    @GetMapping("/check-guest-contact")
    @ResponseBody
    public Map<String, Object> checkGuestContact(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        boolean existsByEmail = email != null && !email.isBlank()
                && customerRepository.findByEmail(email.trim()).isPresent();
        boolean existsByPhone = phone != null && !phone.isBlank()
                && customerRepository.existsByPhone(phone.trim());

        return Map.of("exists", existsByEmail || existsByPhone);
    }

    @GetMapping("/guest")
    public String guestCheckoutPage(@RequestParam(required = false) Boolean buyNow,
                                    Model model, HttpSession session) {
        boolean buyNowMode = isBuyNowMode(buyNow, session);

        // Load cart items and compute totals for guest, so template can render order summary.
        List<CartDetail> cartItems = buyNowMode
                ? buildCartDetailsFromMap(getBuyNowItem(session))
                : buildCartDetailsFromMap(getGuestCart(session));

        log.info("📦 [GUEST] cartItems.size() after processing: {}, buyNow: {}", cartItems.size(), buyNowMode);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartItemsCount", cartItems.size());
        model.addAttribute("buyNow", buyNowMode);
        model.addAttribute("title", "Thanh toán - Khách");

        BigDecimal subtotal = cartItems.stream()
                .filter(i -> i.getProductDetail() != null && i.getProductDetail().getPrice() != null)
                .map(i -> i.getProductDetail().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("💰 [GUEST] subtotal computed: {}", subtotal);

        BigDecimal shippingFee = subtotal.compareTo(BigDecimal.valueOf(500_000)) < 0
                ? BigDecimal.valueOf(30_000)
                : BigDecimal.ZERO;

        // Guest không tính best discount theo customerId, chỉ hiển thị danh sách.
        List<DiscountCode> discountCodes = discountCodeService.findAll();
        model.addAttribute("discountCodes", discountCodes);

        BigDecimal bestDiscountAmount = BigDecimal.ZERO;
        String bestDiscountCode = null;

        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(bestDiscountAmount);

        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountAmount", bestDiscountAmount);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("bestDiscountCode", bestDiscountCode);

        return "customer/checkout/checkout";
    }

    // ===== ÁP DỤNG MÃ GIẢM GIÁ =====
    @PostMapping("/apply-discount")
    public String applyDiscount(
            @RequestParam(value = "applyDiscountCode", required = false) String discountCode,
            @RequestParam(required = false) Boolean buyNow,
            Model model,
            HttpSession session,
            RedirectAttributes ra
    ) {
        String customerId = getCurrentCustomerId().orElse(null);

        if (customerId == null) {
            return "redirect:/customer/checkout/guest";
        }

        boolean buyNowMode = isBuyNowMode(buyNow, session);

        // Lấy giỏ hàng: ưu tiên item "mua ngay" nếu đang ở chế độ này
        List<CartDetail> cartItems = buyNowMode
                ? buildCartDetailsFromMap(getBuyNowItem(session))
                : cartService.getCartDetails(customerId);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("buyNow", buyNowMode);
        model.addAttribute("title", "Thanh toán");

        BigDecimal subtotal = cartItems.stream()
                .filter(i -> i.getProductDetail() != null && i.getProductDetail().getPrice() != null)
                .map(i -> i.getProductDetail().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = subtotal.compareTo(BigDecimal.valueOf(500_000)) < 0
                ? BigDecimal.valueOf(30_000)
                : BigDecimal.ZERO;

        BigDecimal discountAmount = BigDecimal.ZERO;
        String finalCode = null;

        String normalizedCode = (discountCode != null && !discountCode.isBlank())
                ? discountCode.trim()
                : null;

        if (normalizedCode != null) {
            try {
                discountAmount = discountCodeService.validate(normalizedCode, customerId,
                        com.example.skysport1.service.DiscountCodeService.fromCartDetails(cartItems));
                finalCode = normalizedCode;
                ra.addFlashAttribute("discountConfirmCode", normalizedCode);
                log.info("✅ Áp dụng mã thành công: {} - giảm: {}", normalizedCode, discountAmount);
            } catch (Exception e) {
                log.error("❌ Lỗi áp dụng mã {}: {}", normalizedCode, e.getMessage());
                ra.addFlashAttribute("error", e.getMessage());
                discountAmount = BigDecimal.ZERO;
                finalCode = null;
            }
        }

        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount);

        List<DiscountCode> discountCodes = discountCodeService.findAll();

        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("discountCodes", discountCodes);
        model.addAttribute("bestDiscountCode", finalCode);

        // Auto-fill thông tin nhận hàng
        if (customerId != null) {
            fillCustomerInfo(model, customerId);
        }

        return "customer/checkout/checkout";
    }

    // ===== GUEST CHECKOUT =====
    @PostMapping("/guest")
    public String createGuestBill(
            @RequestParam("guestEmail") String guestEmail,
            @RequestParam("shippingAddress") String shippingAddress,
            @RequestParam("receiverName") String receiverName,
            @RequestParam("receiverPhone") String receiverPhone,
            @RequestParam("paymentId") String paymentId,
            @RequestParam(value = "discountCode", required = false) String discountCode,
            @RequestParam(required = false) Boolean buyNow,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            boolean buyNowMode = isBuyNowMode(buyNow, session);
            Map<Integer, Integer> sourceCart = buyNowMode ? getBuyNowItem(session) : getGuestCart(session);
            log.info("🟡 Guest checkout - cart content: {}, buyNow: {}", sourceCart, buyNowMode);

            if (sourceCart == null || sourceCart.isEmpty()) {
                ra.addFlashAttribute("error", "Giỏ hàng trống! Vui lòng thêm sản phẩm trước khi thanh toán.");
                return "redirect:/customer/cart";
            }

            List<BillDetail> items = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : sourceCart.entrySet()) {
                Integer productDetailId = e.getKey();
                Integer qty = e.getValue();

                if (productDetailId == null || productDetailId <= 0) continue;
                if (qty == null || qty <= 0) continue;

                items.add(BillDetail.builder()
                        .productDetail(ProductDetail.builder().id(productDetailId).build())
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

            // Chỉ xoá đúng nguồn giỏ hàng đã dùng — "mua ngay" không được đụng
            // tới giỏ hàng thật (guest cart) của khách và ngược lại.
            if (buyNowMode) {
                clearBuyNowItem(session);
            } else {
                session.removeAttribute(SESSION_GUEST_CART);
            }
            ra.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn: " + created.getId());

            // ✅ Sửa redirect để guest (chưa đăng nhập) không bị chặn role CUSTOMER
            return "redirect:/guest/order/" + created.getId() + "?email=" + guestEmail;

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
            @RequestParam(required = false) Boolean buyNow,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            String customerId = getCurrentCustomerId()
                    .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để checkout"));

            boolean buyNowMode = isBuyNowMode(buyNow, session);
            List<CartDetail> cartDetails = buyNowMode
                    ? buildCartDetailsFromMap(getBuyNowItem(session))
                    : cartService.getCartDetails(customerId);
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

            // "Mua ngay" chỉ xoá item tạm, KHÔNG đụng tới giỏ hàng thật của khách
            if (buyNowMode) {
                clearBuyNowItem(session);
            } else {
                cartService.clearCart(customerId);
            }
            ra.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn: " + created.getId());
            return "redirect:/customer/orders/" + created.getId();

        } catch (Exception e) {
            log.error("Checkout error: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/checkout";
        }
    }

    // ===== HELPER METHODS =====

    /**
     * true nếu đang ở luồng "Mua ngay" VÀ session còn item tương ứng — dùng ở
     * cả GET (hiển thị) lẫn POST (tạo đơn) để đảm bảo tính nhất quán.
     */
    private boolean isBuyNowMode(Boolean buyNow, HttpSession session) {
        return Boolean.TRUE.equals(buyNow) && !getBuyNowItem(session).isEmpty();
    }

    private Map<Integer, Integer> getBuyNowItem(HttpSession session) {
        Object raw = session.getAttribute(SESSION_BUY_NOW_ITEM);
        if (raw instanceof Map<?, ?> map) {
            Map<Integer, Integer> result = new java.util.HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() instanceof Integer k && e.getValue() instanceof Integer v) {
                    result.put(k, v);
                }
            }
            return result;
        }
        return Collections.emptyMap();
    }

    private void clearBuyNowItem(HttpSession session) {
        session.removeAttribute(SESSION_BUY_NOW_ITEM);
    }

    /**
     * Build danh sách CartDetail (để tính tiền/hiển thị) từ 1 map
     * productDetailId -> quantity — dùng chung cho cả giỏ hàng guest thật lẫn
     * item "mua ngay", tránh lặp lại vòng lặp try/catch ở nhiều nơi.
     */
    private List<CartDetail> buildCartDetailsFromMap(Map<Integer, Integer> map) {
        List<CartDetail> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            Integer productDetailId = e.getKey();
            Integer qty = e.getValue();
            if (productDetailId == null || productDetailId <= 0) continue;
            if (qty == null || qty <= 0) continue;
            try {
                ProductDetail pd = productDetailRepository.findByIdWithProductSizeColor(productDetailId)
                        .orElseThrow(() -> new RuntimeException("ProductDetail not found: " + productDetailId));
                items.add(CartDetail.builder().productDetail(pd).quantity(qty).build());
            } catch (Exception ex) {
                log.error("buildCartDetailsFromMap - không tìm thấy productDetailId={}: {}", productDetailId, ex.getMessage());
            }
        }
        return items;
    }

    private Map<Integer, Integer> getGuestCart(HttpSession session) {
        Object raw = session.getAttribute(SESSION_GUEST_CART);
        log.info("🔍 Raw guest cart from session: {}", raw);

        if (raw instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> result = (Map<Integer, Integer>) map;
            log.info("✅ Guest cart has {} items: {}", result.size(), result);
            return result;
        }
        log.warn("⚠️ No guest cart found in session or invalid type");
        return Collections.emptyMap();
    }

    private Optional<String> getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }

        String username = auth.getName();
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        try {
            var account = accountService.findByUsername(username);
            var customer = customerService.findByAccountId(account.getId());
            return Optional.ofNullable(customer.getId());
        } catch (Exception e) {
            log.error("Error getting customer id: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void fillCustomerInfo(Model model, String customerId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;

        if (username != null && !username.isBlank()) {
            var account = accountService.findByUsername(username);
            var customer = customerService.findByAccountId(account.getId());
            if (customer != null) {
                model.addAttribute("receiverName", customer.getFullName());
                model.addAttribute("receiverPhone", customer.getPhone());
            }
        }

        Optional<AddressShipping> defaultAddressOpt =
                addressShippingRepository.findByCustomerIdAndIsDefault(customerId, true);

        AddressShipping addr = defaultAddressOpt.orElseGet(() ->
                addressShippingRepository.findByCustomerId(customerId).stream().findFirst().orElse(null)
        );

        if (addr != null) {
            model.addAttribute("shippingAddress", addr.getAddress());
        }
    }
}