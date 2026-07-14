package com.example.skysport1.controller.customer;

import com.example.skysport1.entity.Cart;
import com.example.skysport1.entity.CartDetail;
import com.example.skysport1.entity.ProductDetail;
import com.example.skysport1.repository.CartDetailRepository;
import com.example.skysport1.repository.ProductDetailRepository;
import com.example.skysport1.service.AccountService;
import com.example.skysport1.service.CartService;
import com.example.skysport1.service.CustomerService;
import com.example.skysport1.service.ProductDetailService;
import com.example.skysport1.util.mapper.CustomerMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer/cart")
public class CustomerCartController {

    private static final String SESSION_GUEST_CART = "GUEST_CART";
    // Giỏ hàng tạm cho luồng "Mua ngay" — tách biệt hoàn toàn khỏi giỏ hàng
    // thật (persistent cart / GUEST_CART) để không làm lệch giỏ hàng của khách.
    private static final String SESSION_BUY_NOW_ITEM = "BUY_NOW_ITEM";

    private final CartService cartService;
    private final AccountService accountService;
    private final CustomerService customerService;
    private final ProductDetailService productDetailService;
    private final ProductDetailRepository productDetailRepository;
    private final CartDetailRepository cartDetailRepository;  // ✅ THÊM

    @SuppressWarnings("unused")
    private final CustomerMapper customerMapper;

    public CustomerCartController(
            CartService cartService,
            AccountService accountService,
            CustomerService customerService,
            ProductDetailService productDetailService,
            ProductDetailRepository productDetailRepository,
            CartDetailRepository cartDetailRepository,  // ✅ THÊM
            CustomerMapper customerMapper
    ) {
        this.cartService = cartService;
        this.accountService = accountService;
        this.customerService = customerService;
        this.productDetailService = productDetailService;
        this.productDetailRepository = productDetailRepository;
        this.cartDetailRepository = cartDetailRepository;  // ✅ THÊM
        this.customerMapper = customerMapper;
    }

    @GetMapping
    public String viewCart(Model model, HttpSession session) {
        String customerId = getCurrentCustomerId().orElse(null);

        // Guest mode: convert session map -> CartDetail list for template
        if (customerId == null) {
            Map<Integer, Integer> guestCart = getGuestCart(session);
            List<CartDetail> guestItems = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : guestCart.entrySet()) {
                try {
                    ProductDetail pd = productDetailRepository.findByIdWithProductSizeColor(e.getKey()).orElseThrow();
                    CartDetail cd = CartDetail.builder()
                            .productDetail(pd)
                            .quantity(e.getValue())
                            .build();
                    guestItems.add(cd);
                } catch (Exception ex) {
                    // sản phẩm không còn tồn tại -> bỏ qua
                }
            }
            model.addAttribute("cart", null);
            model.addAttribute("items", guestItems);
            model.addAttribute("guestCart", guestCart);
            model.addAttribute("totalItems",
                    guestCart.values().stream().mapToInt(Integer::intValue).sum());
            addCartSummary(model, guestItems);
            return "customer/cart/cart";
        }

        // ✅ Logged-in user: lấy cart + details có fetch join Product
        Cart cart = cartService.getOrCreateCart(customerId);
        List<CartDetail> details = cartDetailRepository.findByCartIdWithProduct(cart.getId());

        model.addAttribute("cart", cart);
        model.addAttribute("items", details);
        model.addAttribute("totalItems", details.stream().mapToInt(CartDetail::getQuantity).sum());
        addCartSummary(model, details);
        return "customer/cart/cart";
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestParam("productDetailId") Integer productDetailId,
            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
            HttpSession session
    ) {
        String customerId = getCurrentCustomerId().orElse(null);

        if (productDetailId == null || productDetailId <= 0 || quantity <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Vui lòng chọn biến thể và số lượng hợp lệ"));
        }

        if (customerId == null) {
            Map<Integer, Integer> guestCart = getGuestCart(session);
            guestCart.merge(productDetailId, quantity, Integer::sum);
            session.setAttribute(SESSION_GUEST_CART, guestCart);
            int count = guestCart.values().stream().mapToInt(Integer::intValue).sum();
            return ResponseEntity.ok(Map.of("success", true, "cartCount", count));
        }

        cartService.addToCart(customerId, productDetailId, quantity);
        int count = cartService.getCartDetails(customerId).stream()
                .mapToInt(CartDetail::getQuantity).sum();
        return ResponseEntity.ok(Map.of("success", true, "cartCount", count));
    }

    /**
     * "Mua ngay" — không đụng tới giỏ hàng thật (persistent cart / GUEST_CART).
     * Lưu đúng 1 sản phẩm/biến thể vào 1 session key riêng, rồi chuyển thẳng
     * tới trang checkout ở chế độ buyNow=true, nơi chỉ đọc đúng sản phẩm này.
     */
    @PostMapping("/buy-now")
    public String buyNow(
            @RequestParam("productDetailId") Integer productDetailId,
            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
            HttpSession session
    ) {
        if (productDetailId == null || productDetailId <= 0 || quantity <= 0) {
            return "redirect:/customer/cart";
        }
        session.setAttribute(SESSION_BUY_NOW_ITEM,
                Collections.singletonMap(productDetailId, quantity));
        return "redirect:/customer/checkout?buyNow=true";
    }

    /**
     * Trả số lượng sản phẩm trong giỏ hàng thật (không tính buy-now) — dùng
     * để hiển thị badge trên icon giỏ hàng khi trang vừa load.
     */
    @GetMapping("/count")
    @ResponseBody
    public Map<String, Object> cartCount(HttpSession session) {
        String customerId = getCurrentCustomerId().orElse(null);
        int count;
        if (customerId == null) {
            count = getGuestCart(session).values().stream().mapToInt(Integer::intValue).sum();
        } else {
            count = cartService.getCartDetails(customerId).stream()
                    .mapToInt(CartDetail::getQuantity).sum();
        }
        return Map.of("cartCount", count);
    }

    @PostMapping("/update")
    public String updateQuantity(
            @RequestParam(value = "cartDetailId", required = false) Integer cartDetailId,
            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
            HttpSession session
    ) {
        String customerId = getCurrentCustomerId().orElse(null);

        if (cartDetailId == null || cartDetailId <= 0) {
            return "redirect:/customer/cart";
        }

        if (customerId == null) {
            // guest: cartDetailId treated as productDetailId
            Map<Integer, Integer> guestCart = getGuestCart(session);
            if (quantity <= 0) {
                guestCart.remove(cartDetailId);
            } else {
                // Giới hạn số lượng theo tồn kho thực tế, tránh guest tự set
                // số lượng vượt kho (trước đây chỉ chặn ở bước checkout).
                int allowedQuantity = quantity;
                ProductDetail pd = productDetailRepository.findById(cartDetailId).orElse(null);
                if (pd == null || pd.getStatus() != 1 || Boolean.TRUE.equals(pd.getDeleteFlag())) {
                    guestCart.remove(cartDetailId);
                    session.setAttribute(SESSION_GUEST_CART, guestCart);
                    return "redirect:/customer/cart";
                }
                if (allowedQuantity > pd.getQuantity()) {
                    allowedQuantity = pd.getQuantity();
                }
                if (allowedQuantity <= 0) {
                    guestCart.remove(cartDetailId);
                } else {
                    guestCart.put(cartDetailId, allowedQuantity);
                }
            }
            session.setAttribute(SESSION_GUEST_CART, guestCart);
            return "redirect:/customer/cart";
        }

        cartService.updateQuantity(customerId, cartDetailId, quantity);
        return "redirect:/customer/cart";
    }

    @PostMapping("/remove")
    public String removeItem(
            @RequestParam(value = "cartDetailId", required = false) Integer cartDetailId,
            HttpSession session
    ) {
        String customerId = getCurrentCustomerId().orElse(null);

        if (cartDetailId == null || cartDetailId <= 0) {
            return "redirect:/customer/cart";
        }

        if (customerId == null) {
            // guest: cartDetailId treated as productDetailId
            Map<Integer, Integer> guestCart = getGuestCart(session);
            guestCart.remove(cartDetailId);
            session.setAttribute(SESSION_GUEST_CART, guestCart);
            return "redirect:/customer/cart";
        }

        cartService.removeItem(customerId, cartDetailId);
        return "redirect:/customer/cart";
    }

    private Map<Integer, Integer> getGuestCart(HttpSession session) {
        Object raw = session.getAttribute(SESSION_GUEST_CART);
        if (raw instanceof Map<?, ?> map) {
            Map<Integer, Integer> result = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() instanceof Integer k && e.getValue() instanceof Integer v) {
                    result.put(k, v);
                }
            }
            return result;
        }
        return new HashMap<>();
    }

    /**
     * Map logged-in user -> customerId.
     */
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

    private void addCartSummary(Model model, List<CartDetail> items) {
        BigDecimal subtotal = items.stream()
                .filter(i -> i.getProductDetail() != null && i.getProductDetail().getPrice() != null)
                .map(i -> i.getProductDetail().getPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = subtotal.compareTo(BigDecimal.valueOf(500_000)) < 0
                ? BigDecimal.valueOf(30_000) : BigDecimal.ZERO;

        BigDecimal totalAmount = subtotal.add(shippingFee);

        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountAmount", BigDecimal.ZERO);
        model.addAttribute("totalAmount", totalAmount);
    }
}